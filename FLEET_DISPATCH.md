# Fleet Dispatch

This document defines how SequoRider classifies couriers, assigns missions, manages capacity, and escalates delivery incidents.

## Goals

- Protect subscriber service quality by using internal salaried drivers first where eligible.
- Keep express food deliveries on moto freelancers.
- Use freelancers as overflow capacity and for delivery types that require their transport mode.
- Keep internal-driver payroll separate from freelancer per-delivery earnings.
- Assign missions using transparent, auditable rules that operations can tune without app releases.

## Source Of Truth

Current SequoRider business rules override older Sequo documentation. The legacy docs are still useful for order lifecycle, PIN validation, manual verification, masked contact, admin monitoring, and grouped logistics, but the dispatch model here is authoritative for the rider app.

## Fleet Classes

| Class | Employment | Vehicle | Paid per delivery | Eligible examples |
| --- | --- | --- | --- | --- |
| `INTERNAL_DRIVER` | Salaried Sequo staff | Approved internal vehicle, moto, or other assigned transport | No | Subscriber standard orders, scheduled routes, cooperative market consolidation, high-trust logistics |
| `FREELANCE_MOTO` | Independent courier | Motorcycle | Yes | Express food, urgent parcels, medium/long distance, overflow subscriber orders |
| `FREELANCE_BICYCLE` | Independent courier | Bicycle | Yes | Short distance, low weight, non-urgent local deliveries |

Internal drivers may still see missions in SequoRider, but they must not see freelancer-style earnings, withdrawal actions, or per-delivery split details.

## Rider Verification

Every rider must be manually verified before receiving production missions.

Required profile fields:

- Legal name.
- Phone number.
- Emergency contact.
- Fleet class.
- Transport type.
- Declared service zones.
- Identity document status.
- Vehicle document status when applicable.
- Active payout account for freelancers.
- Employment status for internal drivers.

Freelancer-specific requirements:

- Payout account must be Yas Togo or Moov Africa.
- KYC must be approved before payout eligibility.
- Vehicle type must be declared as moto or bicycle.
- Current availability must be `AVAILABLE` before mission offers are sent.

Internal-specific requirements:

- Employment status must be active.
- Driver must have a capacity schedule or shift assignment.
- Payroll identity must be linked outside the freelancer payout ledger.

## Order Dispatch Profile

Before dispatch, every order should be normalized into an `OrderDispatchProfile`.

| Field | Purpose |
| --- | --- |
| `orderId` | Stable backend order identity |
| `subscriberPriority` | Whether the customer/order has subscriber priority |
| `serviceType` | Standard, express food, cooperative market, scheduled, external parcel, return, or support mission |
| `temperatureSensitivity` | Hot food or otherwise time-sensitive |
| `pickupStops` | One or more pickup points |
| `dropoffStops` | One or more dropoff points |
| `estimatedDistanceKm` | Road distance where available |
| `estimatedDurationMin` | Map-provider ETA where available |
| `packageWeightKg` | Total expected carrying weight |
| `packageCount` | Number of packages or merchant lots |
| `requiredVehicle` | Moto, bicycle, internal vehicle, or any eligible |
| `slaDeadline` | Latest acceptable delivery or pickup time |
| `deliveryFeePaid` | Customer-facing delivery fee |
| `courierRate` | Minimum amount owed to freelancer if assigned |
| `proofPolicy` | PIN, photo, signature, recipient confirmation, or admin override |

## Eligibility Rules

Dispatch begins by removing riders who cannot legally or operationally handle the mission.

A rider is eligible only when:

- Rider status is active.
- KYC or internal employment verification is approved.
- Rider is available or preassigned by operations.
- Rider has capacity for the package count and weight.
- Rider is inside the allowed service zone or can reach pickup within SLA.
- Vehicle type satisfies the order requirement.
- Rider is not blocked by an active incident or payout hold that prevents assignments.
- Rider has not exceeded concurrent mission limits.

Hard exclusions:

- Internal drivers are excluded from per-delivery payout calculations.
- Bicycle freelancers are excluded from express food unless operations explicitly configures a local exception.
- Bicycle freelancers are excluded from heavy, long-distance, or high-urgency missions.
- Express food deliveries are assigned to moto freelancers.
- A freelancer without Yas Togo or Moov Africa payout setup can receive missions only if operations explicitly allows pre-payout onboarding missions; payout remains blocked until account verification.

## Dispatch Priority Ladder

The dispatch engine applies rules in this order:

1. Safety and legal eligibility.
2. Service-type hard constraints.
3. Subscriber priority.
4. Capacity and SLA feasibility.
5. Route efficiency.
6. Cost and Sequo subsidy exposure.
7. Rider quality score.
8. Fairness and workload balancing.

### Service-Type Hard Constraints

| Service type | Rule |
| --- | --- |
| Express food | Assign to eligible moto freelancers. Subscriber status can raise urgency but does not move the order to internal drivers. |
| Subscriber standard | Try internal salaried drivers first. Overflow to freelancers when internal capacity is unavailable or SLA would be missed. |
| Cooperative market scheduled route | Prefer internal drivers when batching or consolidation is planned. Overflow to freelancers for urgent, short, or capacity-constrained routes. |
| Non-subscriber standard | Choose the best eligible rider by SLA, distance, cost, and quality. |
| Bicycle-eligible local delivery | Allow bicycle freelancers when package is light, distance is short, and SLA is non-urgent. |

## Capacity Model

Each rider has a capacity snapshot:

```text
capacity = {
  maxActiveMissions,
  activeMissions,
  maxPackageCount,
  assignedPackageCount,
  maxWeightKg,
  assignedWeightKg,
  shiftEndTime,
  currentLocation,
  lastLocationAt,
  availabilityState
}
```

Internal capacity is considered exhausted when any of these are true:

- No eligible internal driver is available.
- All eligible internal drivers are at mission, package, or weight capacity.
- Internal driver ETA to pickup would violate the SLA.
- Internal shift end would occur before mission completion.
- Operations marks the internal fleet as paused for the zone.

When capacity is exhausted, subscriber standard orders overflow to eligible freelancers.

## Assignment Algorithm

Pseudocode:

```text
function dispatch(order):
  profile = buildDispatchProfile(order)
  candidates = loadEligibleRiders(profile)

  if profile.serviceType == EXPRESS_FOOD:
    candidates = candidates.only(FREELANCE_MOTO)
    return offerBestCandidate(profile, candidates)

  if profile.subscriberPriority and profile.internalEligible:
    internalCandidates = candidates.only(INTERNAL_DRIVER)
    feasibleInternal = scoreAndFilterFeasible(profile, internalCandidates)
    if feasibleInternal.hasCandidateWithinSla:
      return assignOrOfferInternal(profile, feasibleInternal.best)

  freelancerCandidates = candidates.only(FREELANCE_MOTO, FREELANCE_BICYCLE)
  feasibleFreelancers = scoreAndFilterFeasible(profile, freelancerCandidates)
  return offerBestCandidate(profile, feasibleFreelancers.best)
```

For internal drivers, assignment may be direct when their shift policy allows it. For freelancers, assignment should usually be an offer with an acceptance timeout.

## Candidate Scoring

A candidate score is a weighted sum. Lower score is better.

| Factor | Meaning |
| --- | --- |
| Pickup ETA | Time from rider location to pickup |
| Delivery ETA | Projected time from pickup to dropoff |
| SLA risk | Penalty when projected completion nears deadline |
| Vehicle fit | Penalty for non-ideal but allowed vehicle |
| Route fit | Bonus when mission can join an existing batch |
| Capacity fit | Penalty as rider approaches package or weight limit |
| Quality score | Acceptance rate, completion rate, incident rate |
| Fairness | Avoid starving lower-volume qualified freelancers |
| Subsidy exposure | Operational awareness when Sequo covers delivery-fee gap |

Suggested scoring shape:

```text
score =
  pickupEtaMin * 2.0
  + deliveryEtaMin * 1.0
  + slaRiskPenalty
  + vehiclePenalty
  + capacityPenalty
  + incidentPenalty
  - routeFitBonus
  - subscriberInternalBonus
  - fairnessBonus
```

`subscriberInternalBonus` applies only to eligible internal drivers and must not override express-food moto freelancer rules.

## Mission Offer Flow

Freelancer offer lifecycle:

1. Backend creates `MissionOffer`.
2. Push notification is sent to the selected freelancer.
3. Rider sees pickup area, dropoff area, package type, expected distance, expected payout, and acceptance timer.
4. Rider accepts or rejects.
5. Timeout or rejection records a reason and triggers reassignment.
6. Acceptance creates `MissionAssignment`.
7. Earnings become pending only after successful delivery proof.

Internal driver lifecycle:

1. Backend creates `MissionAssignment` directly or sends an internal acknowledgement request based on shift policy.
2. Driver sees mission priority, pickup, dropoff, package notes, and route order.
3. No per-delivery payout is displayed.
4. Completion updates operational metrics and salary-performance reporting, not freelancer earnings.

## Mission Statuses

Use stable internal statuses and simple rider-facing labels.

| Internal status | Rider-facing label |
| --- | --- |
| `OFFERED` | New mission |
| `ACCEPTED` | Accepted |
| `EN_ROUTE_TO_PICKUP` | Going to pickup |
| `ARRIVED_PICKUP` | At pickup |
| `PICKED_UP` | Picked up |
| `EN_ROUTE_TO_DROPOFF` | On the way |
| `ARRIVED_DROPOFF` | At delivery point |
| `DELIVERED` | Delivered |
| `FAILED` | Problem reported |
| `RETURN_REQUIRED` | Return required |
| `RETURNED` | Returned |
| `CANCELLED` | Cancelled |

## Map Integration

The dispatch engine should use map data for:

- Geocoding seller, market, relay, hub, and customer locations.
- Reverse geocoding rider map pins into human-readable operational areas.
- Road-distance estimates.
- ETA estimates.
- Route polylines.
- Zone containment checks.
- Pickup clustering and dropoff sequencing.

Fallback rules:

- If road ETA is unavailable, use straight-line distance with a conservative multiplier.
- If the customer has no map pin, require neighborhood and landmark.
- If both map pin and landmark are missing, block automated dispatch and send the order to support review.
- If rider location is stale, apply a stale-location penalty or request a fresh ping.

## Subscriber Dispatch Rules

Subscriber priority is a service-level rule, not a payout rule.

For eligible subscriber orders:

1. Search active internal drivers in the order zone.
2. Remove drivers who cannot complete within SLA.
3. Prefer the driver whose current route best absorbs the mission.
4. Assign or request acknowledgement.
5. If no internal driver can meet the SLA, overflow to freelancers.
6. When overflowing, select the lowest-risk eligible freelancer by service type and vehicle.

Subscriber priority must be auditable. The dispatch record should state whether the order used internal capacity or overflowed to freelancers and why.

## Express Food Rules

Express food deliveries use moto freelancers.

Rationale:

- Hot-food delivery is time-sensitive.
- Moto couriers are better suited to immediate point-to-point delivery.
- The customer experience depends on speed and temperature preservation.

Required constraints:

- One active express food mission at a time per freelancer in MVP unless batching is explicitly enabled.
- Pickup begins only after the seller marks the order ready or near-ready.
- Delivery proof is required before completion.
- Customer absence follows the configured food-delivery incident policy.

## Reassignment Rules

Reassignment is triggered by:

- Freelancer rejection.
- Offer timeout.
- Rider cancellation before pickup.
- Rider becomes unreachable.
- Rider reports safety issue.
- Pickup delay exceeds threshold.
- Order is no longer feasible within SLA.
- Admin override.

Each reassignment must log:

- Previous rider.
- New rider.
- Trigger.
- Actor.
- Timestamp.
- Current mission status.
- ETA impact.
- Payout impact, if any.

## Incident Handling

| Incident | First action | Escalation |
| --- | --- | --- |
| Address unclear | Rider uses masked contact and checks landmark | Support reviews address and may request location share |
| Customer absent | Rider waits configured time and contacts customer | Support decides retry, relay drop, return, or closure |
| Seller not ready | Rider records delay | Dispatch may re-sequence route or release rider |
| Package missing | Rider records photo/note | Support contacts seller or relay |
| PIN invalid | Do not hand over package | Support/admin verification required |
| Rider safety issue | Rider pauses mission and reports | Operations reassigns or cancels |
| Package damaged | Capture proof | Support opens dispute and payout hold if needed |

## Metrics

Track these metrics by fleet class and zone:

- Assignment success rate.
- Offer acceptance rate.
- Offer timeout rate.
- Average pickup ETA.
- Average delivery ETA.
- SLA breach rate.
- Internal capacity utilization.
- Subscriber overflow rate.
- Freelancer earnings per hour.
- Sequo delivery-fee subsidy total.
- Incident rate.
- Proof-of-delivery failure rate.

## Admin Controls

Operations should be able to configure:

- Internal fleet capacity by zone and shift.
- Freelancer offer timeout.
- Maximum active missions per rider.
- Vehicle eligibility by service type.
- Weight and distance thresholds for bicycle eligibility.
- Subscriber overflow threshold.
- SLA timers.
- Incident wait times.
- Manual assignment override.
- Temporary zone pause during rain, safety events, or traffic disruption.

Every change must be logged with actor, reason, timestamp, previous value, and new value.

