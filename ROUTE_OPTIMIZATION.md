# Route Optimization

This document defines how SequoRider should plan pickup sequences, delivery routes, cooperative market consolidation, and map-provider integration.

## Goals

- Reduce delivery time without breaking service-type rules.
- Support batch pickup from several nearby sellers.
- Support single-package consolidation for cooperative markets.
- Preserve express food speed by avoiding inappropriate batching.
- Use internal drivers efficiently for subscriber and scheduled routes.
- Use freelancers efficiently for express and overflow missions.
- Keep map behavior resilient when addresses are landmark-based or GPS is unavailable.

## Routing Principles

Route optimization must respect dispatch constraints before minimizing distance.

Priority order:

1. Safety.
2. Service-type eligibility.
3. Subscriber/internal fleet rules.
4. Express food moto-freelancer rule.
5. Package custody and proof requirements.
6. SLA deadlines.
7. Route efficiency.
8. Cost efficiency.

The shortest route is not valid if it violates fleet eligibility, package temperature, custody, proof, or customer promise.

## Supported Route Patterns

| Pattern | Description | Typical fleet |
| --- | --- | --- |
| Point-to-point | One pickup, one dropoff | Moto freelancer, bicycle freelancer, internal driver |
| Multi-pickup single-drop | Several pickups combined into one delivery | Internal driver, moto freelancer when urgent |
| Single-pickup multi-drop | One pickup location serving several nearby customers | Internal driver, moto freelancer |
| Cooperative consolidation | Several cooperative sellers grouped into one customer-facing package | Internal driver or eligible freelancer |
| Scheduled tour | Admin-planned route across pickups, hub, relays, and dropoffs | Internal driver |
| Overflow route | Route moved from internal fleet to freelancer due to capacity or SLA risk | Moto or bicycle freelancer depending on constraints |

## Cooperative Market Consolidation

A cooperative market groups nearby merchants into a single customer-facing store. Legacy Sequo docs described cooperative sellers as nearby shops that share exposure but keep revenues separate. SequoRider extends that idea into field routing.

Example: items from Marche de Mulhouse are ordered from several cooperative sellers. The customer should receive one consolidated package when possible, while each merchant's sale remains traceable.

Required behavior:

- Preserve seller-level item ownership.
- Preserve sub-order or merchant lot IDs.
- Allow batch pickup from multiple stalls or shops.
- Consolidate into one package when packaging and food-safety rules allow it.
- Deliver as a single package when customer SLA allows.
- Pay each merchant separately outside SequoRider.
- Pay the assigned freelancer per delivery when a freelancer performs the route.
- Exclude internal drivers from per-order payout when an internal driver performs the route.

## Consolidation Rules

Items can be consolidated when:

- Pickup points are inside the cooperative market boundary.
- Merchant locations are close enough for walking or short riding sequence.
- Items are compatible in the same package.
- Package weight and dimensions fit the assigned vehicle.
- Customer SLA allows the extra pickup time.
- All required merchant lots are ready or can be ready within the pickup window.
- Proof and custody can identify each merchant lot before final handoff.

Do not consolidate when:

- Hot food would be delayed beyond acceptable time.
- Fragile or incompatible items would be damaged.
- A seller has not confirmed item readiness.
- The customer paid for immediate express handling that excludes batching.
- The resulting package exceeds vehicle capacity.
- The route would cause a subscriber SLA breach.

## Route Data Model

```text
RoutePlan {
  routeId
  missionId
  riderId
  routeType
  fleetClass
  stops: List<RouteStop>
  packageLots: List<PackageLot>
  estimatedDistanceKm
  estimatedDurationMin
  slaDeadline
  mapProvider
  routePolyline
  optimizationVersion
  createdAt
}
```

```text
RouteStop {
  stopId
  type: PICKUP | CONSOLIDATION | HUB | RELAY | DROPOFF | RETURN
  sequence
  location
  neighborhood
  landmark
  timeWindowStart
  timeWindowEnd
  serviceTimeMin
  requiredProof
  relatedMerchantId
  relatedPackageLotIds
}
```

```text
PackageLot {
  lotId
  orderId
  merchantId
  cooperativeId
  weightKg
  dimensions
  handlingTags
  custodyStatus
}
```

## Map Provider Responsibilities

The map layer should provide:

- Forward geocoding for merchant, relay, hub, and customer addresses.
- Reverse geocoding for rider pins and manually placed customer pins.
- Road distance.
- Road ETA.
- Route polyline.
- Matrix estimates for multiple pickup and dropoff candidates.
- Zone boundary checks.
- Traffic-aware ETA when available.

Google Maps was named in the legacy technical docs. The implementation can use any approved provider later, but the domain should depend on interfaces, not direct SDK calls.

Suggested interfaces:

```text
Geocoder
DistanceMatrixProvider
DirectionsProvider
ZoneResolver
EtaProvider
MapSnapshotProvider
```

## Address Strategy

Sequo must support non-standard addresses.

Required address fields:

- Neighborhood.
- Landmark.
- Contact method or masked contact token.
- Delivery note.

Optional but recommended:

- Map pin.
- Shared live location window.
- Saved address label.
- Entrance or building details.

Routing rules:

- If map pin exists, use it for routing but still show landmark to rider.
- If map pin is missing, geocode using neighborhood and landmark.
- If geocoding confidence is low, require support review before automated dispatch.
- If customer shares live location, use it as temporary routing assistance, not as the permanent saved address unless customer confirms.

## Batch Pickup Algorithm

Use batch pickup when multiple pickup stops can be served by one route without violating SLA or handling constraints.

Pseudocode:

```text
function buildPickupBatch(orderGroup):
  lots = collectReadyLots(orderGroup)
  clusters = clusterByMarketOrDistance(lots)
  for cluster in clusters:
    if allLotsCompatible(cluster) and withinPickupWindow(cluster):
      route = sequenceStops(cluster.pickups, cluster.dropoffs)
      if route.meetsSla and route.fitsVehicle:
        return route
  return splitIntoSmallerRoutes(orderGroup)
```

Clustering inputs:

- Cooperative ID.
- Market boundary.
- Walking distance between merchants.
- Road distance between merchants.
- Item readiness time.
- Required vehicle.
- Customer destination.
- SLA deadline.

## Stop Sequencing

For small batches, use a deterministic heuristic:

1. Start from rider current location or assigned start hub.
2. Visit nearest ready pickup first.
3. Prefer pickups with earliest closing or readiness deadline.
4. Consolidate at market, hub, or final pickup when packaging requires it.
5. Deliver to customer or relay.
6. Recompute ETA after every status update.

For larger scheduled tours, use a vehicle-routing solver on the backend. SequoRider should display the route and collect field updates, not solve large tours locally.

## Route Scoring

Lower score is better:

```text
score =
  totalDurationMin * 2.0
  + totalDistanceKm * 1.0
  + slaRiskPenalty
  + pickupWaitPenalty
  + packageCompatibilityPenalty
  + vehicleCapacityPenalty
  + locationConfidencePenalty
  - consolidationBonus
  - subscriberInternalRouteBonus
```

Penalty examples:

- Add high penalty when ETA exceeds SLA.
- Add penalty when any geocode is low confidence.
- Add penalty when pickup wait time is likely.
- Add penalty when route requires a vehicle class with low availability.
- Add penalty when package count approaches rider capacity.

Bonus examples:

- Add consolidation bonus when several cooperative lots can become one package.
- Add route-fit bonus when a new pickup is near an active internal route.
- Add subscriber-internal route bonus only when the mission is internal-eligible.

## Vehicle Constraints

| Vehicle | Best for | Avoid |
| --- | --- | --- |
| Moto freelancer | Express food, urgent routes, moderate distance, overflow | Oversized loads, excessive multi-drop tours unless configured |
| Bicycle freelancer | Short distance, low weight, non-urgent routes | Express food, long distance, heavy loads, rain-heavy zones |
| Internal driver | Subscriber routes, scheduled routes, cooperative consolidation, high-trust logistics | Express food when business rule requires moto freelancer |

Suggested default thresholds:

| Constraint | Bicycle freelancer | Moto freelancer | Internal driver |
| --- | --- | --- | --- |
| Distance | Short local routes | Short to medium routes | Configured by shift and vehicle |
| Weight | Low weight only | Moderate weight | Configured by assigned vehicle |
| Package count | Low | Low to moderate | Best for batches |
| Urgency | Low to medium | High | Medium to scheduled |

Exact values should be backend-configurable by city, weather, and operations policy.

## Express Food Routing

Express food should generally be point-to-point:

1. Seller marks food ready or near-ready.
2. Dispatch selects eligible moto freelancer.
3. Rider goes to pickup.
4. Rider delivers directly to customer.
5. Proof is captured.

Do not batch express food unless all conditions are true:

- Same seller or same pickup point.
- Dropoffs are very close.
- SLA remains safe.
- Food quality is not degraded.
- Operations has enabled express batching.

## Subscriber Route Handling

Subscriber standard orders should be inserted into internal routes first when eligible.

Insertion check:

```text
function canInsertSubscriberOrder(route, order):
  candidateRoute = insertPickupAndDropoff(route, order)
  return candidateRoute.fitsCapacity
    and candidateRoute.meetsAllExistingSlas
    and candidateRoute.meetsSubscriberSla
    and candidateRoute.shiftSafe
```

If no internal route can absorb the order, create a freelancer overflow dispatch record with the reason:

```text
INTERNAL_CAPACITY_EXHAUSTED
INTERNAL_SLA_RISK
INTERNAL_ZONE_UNAVAILABLE
INTERNAL_SHIFT_END_RISK
```

## Cooperative Market Example

Scenario:

- Customer orders tomatoes, spices, and packaged goods from Marche de Mulhouse.
- Three cooperative sellers confirm readiness.
- The customer selected a consolidated delivery window.

Route:

1. Rider arrives at market entry point.
2. App shows pickup sequence by stall or shop.
3. Rider scans or confirms each merchant lot.
4. App marks all lots in custody.
5. Rider consolidates lots into one package if packaging rules allow.
6. Rider delivers the consolidated package.
7. Customer proof confirms the full route.
8. Backend releases freelancer earning or records internal completion.

If one merchant is not ready:

- If SLA allows, wait up to configured threshold.
- If SLA does not allow, split the missing lot into a later route.
- Customer receives partial or delayed delivery messaging according to order policy.
- Merchant-level custody remains traceable.

## Reoptimization Triggers

Recompute route when:

- Rider rejects mission.
- Rider accepts mission.
- Rider location changes significantly before pickup.
- Seller readiness changes.
- Customer changes delivery window before pickup.
- A pickup fails.
- Package weight or package count changes.
- Traffic or rain causes SLA risk.
- Internal fleet capacity changes.
- Admin inserts a priority subscriber order.

Reoptimization must never silently remove a required stop. It must create a new route version and preserve history.

## Route Versioning

Every route plan should be immutable after assignment. Changes create a new version.

Required fields:

- `routePlanId`
- `version`
- `previousVersionId`
- `changeReason`
- `changedBy`
- `changedAt`
- `oldStopSequence`
- `newStopSequence`
- `etaDelta`
- `distanceDelta`

The rider app should always display the latest active route and gracefully handle route update notifications.

## Offline And Low-Connectivity Behavior

SequoRider should cache:

- Active mission details.
- Current route stops.
- Address text.
- Contact action tokens when valid.
- Proof requirements.
- Last known map polyline if available.

When offline:

- Rider can view assigned route.
- Rider can mark local progress as pending sync only when policy allows.
- Delivery completion requiring server PIN validation should wait for connectivity unless admin fallback is configured.
- The app should prevent duplicate proof submissions using local idempotency keys.

## Privacy And Safety

- Show only the location data needed to execute the mission.
- Avoid exposing exact customer location before mission assignment.
- Use masked contact actions.
- Do not store live customer location longer than required for the delivery.
- Keep rider live tracking frequency configurable to protect battery.
- Do not show precise rider earnings to internal drivers because their pay is salary-based.

## Backend Responsibilities

Route optimization belongs primarily on the backend.

Backend should:

- Own route scoring.
- Own map-provider API keys.
- Own distance matrix calls.
- Own route versioning.
- Validate capacity and SLA.
- Emit route updates to the rider app.
- Store audit logs.
- Calculate payout implications.

SequoRider should:

- Display the active route.
- Collect rider status updates.
- Capture proof and incidents.
- Request navigation handoff.
- Cache mission state for low connectivity.
- Sync field events with idempotency keys.

## Test Cases

Minimum coverage:

- Cooperative market lots can be grouped into one consolidated route.
- Incompatible lots are split into separate routes.
- Express food is not batched by default.
- Subscriber standard order is inserted into internal route when capacity allows.
- Subscriber order overflows when internal route would breach SLA.
- Bicycle freelancer is excluded when route exceeds distance or weight threshold.
- Route update creates a new immutable version.
- Missing map pin with valid landmark can still route through support-approved geocoding.
- Low-confidence geocode blocks automated dispatch.
- Offline proof submission uses idempotency keys and syncs once online.

