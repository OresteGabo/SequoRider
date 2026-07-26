# SequoRider

SequoRider is the courier and field-operations mobile application for the Sequo logistics ecosystem. It is a Kotlin Multiplatform project targeting Android and iOS, with shared business logic and Compose Multiplatform UI in `shared`.

This repository documents and will implement rider classification, dispatch, route execution, proof of delivery, and payout visibility for a dual-fleet delivery model.

## Product Scope

SequoRider supports two active courier populations:

- Internal salaried Sequo drivers.
- Independent freelance couriers using motorcycles or bicycles.

The application must keep those populations financially and operationally distinct. Internal drivers can receive assigned missions as part of their salaried work, but they are never paid through per-order delivery fee splits. Freelancers are paid per completed delivery according to the courier rate configured by Sequo.

## Current Business Rules

These rules override older Sequo documentation whenever there is a conflict:

- Subscriber orders are assigned to internal salaried drivers first when the order type is eligible for internal fleet handling.
- When internal drivers reach capacity, eligible subscriber orders overflow to freelancers.
- Express food deliveries use moto freelancers.
- Freelancers may operate motos or bicycles, but moto freelancers are preferred or required for urgent, hot-food, long-distance, or weight-sensitive deliveries.
- Internal drivers receive fixed salaries and are excluded from per-order delivery fee splits.
- Freelancers are paid per delivery.
- If the customer delivery fee is lower than the required courier rate, Sequo covers the difference.
- Courier payouts are digital only through Yas Togo and Moov Africa.
- Cash withdrawal flows are not supported.
- Payout processing requests must be handled within one week of the relevant package reception event.
- Route planning must support batch pickup and single-package consolidation routes for cooperative markets, including examples such as grouping items from Marche de Mulhouse.

## Documentation Map

Use these files as the operational source of truth for the rider application:

- [FLEET_DISPATCH.md](FLEET_DISPATCH.md): rider classification rules, dispatch queues, assignment algorithm, capacity rules, map integration, and incident handling.
- [PAYOUT_STRUCTURE.md](PAYOUT_STRUCTURE.md): freelancer earnings, internal-driver exclusion from delivery splits, Sequo subsidy handling, digital payout workflow, audit requirements, and SLA rules.
- [ROUTE_OPTIMIZATION.md](ROUTE_OPTIMIZATION.md): pickup grouping, cooperative-market consolidation, route scoring, map provider responsibilities, ETA handling, and optimization data models.

## Legacy Sequo Inputs Reviewed

The new docs were produced after scanning the legacy project at `/Users/muhirwagabooreste/AndroidStudioProjects/Sequo`.

Markdown inputs found there:

- `README.md`
- `docs/OPEN_QUESTIONS.md`
- `docs/ORDER_LIFECYCLE.md`
- `docs/REALISTIC_SCENARIOS.md`
- `docs/MARKET_LOME_NOTES.md`
- `docs/PRODUCT_VALIDATION_BRIEF.md`
- `docs/TECHNICAL_README.md`
- `docs/ROADMAP.md`
- `docs/TASKS.md`
- `docs/FEATURES_AND_REQUIREMENTS.md`
- `docs/USER_ROLES_AND_PERMISSIONS.md`
- `docs/original/SEQUO_SERVICE_CAHIER_DE_CHARGE.md`
- `docs/original/README.md`
- `docs/original/CDC_SEQUO_v6.md`
- `docs/diagrams/plantuml/README.md`
- `docs/diagrams/plantuml/auth/auth_rbac_matrix.md`
- `docs/diagrams/plantuml/auth/README.md`

Important legacy concepts preserved:

- Order lifecycle from customer confirmation through seller preparation, rider assignment, pickup, delivery confirmation, and settlement.
- Manual rider verification and KYC.
- Landmark-based addressing with optional map pin support.
- Masked contact between customer, seller, and rider.
- Delivery PIN or equivalent proof before final handoff.
- Admin visibility over active orders, delivery incidents, rider status, and payments.
- Auditable financial records for payouts, refunds, corrections, and disputes.
- Cooperative merchant grouping where several nearby sellers appear as one customer-facing market.
- Grouped logistics, pickup batches, delivery tours, and point-relais style handoff concepts.

Legacy concepts intentionally revised:

- Older docs often treated rider payouts, payment providers, and dispatch automation as open questions. This repository defines them directly.
- Older docs mentioned multiple payout providers such as Wave, Flooz, and TMoney. SequoRider courier payouts are restricted to Yas Togo and Moov Africa unless a later signed business rule changes this.
- Older docs separated freelance immediate delivery from internal scheduled logistics. This repository adds subscriber-first internal dispatch for eligible orders, with overflow to freelancers.
- Older docs allowed broad cash and manual-money assumptions for early pilots. SequoRider payout processing avoids cash withdrawals entirely.

## Rider Types

| Rider type | Employment model | Transport | Receives app missions | Per-order payout | Main use |
| --- | --- | --- | --- | --- | --- |
| Internal driver | Salaried Sequo staff | Sequo-managed vehicle, moto, or other approved transport | Yes | No | Subscriber priority, scheduled routes, controlled logistics, high-trust deliveries |
| Moto freelancer | Independent courier | Motorcycle | Yes | Yes | Express food, urgent parcels, medium/long distance, overflow capacity |
| Bicycle freelancer | Independent courier | Bicycle | Yes | Yes | Short-distance, low-weight, lower-urgency deliveries |

## Order Types

| Order type | Primary fleet | Overflow fleet | Notes |
| --- | --- | --- | --- |
| Subscriber standard order | Internal drivers | Freelancers after capacity threshold | Internal priority applies before public SLA optimization |
| Express food order | Moto freelancers | Moto freelancers only unless operations explicitly overrides | Hot-food SLA and speed outrank internal priority |
| Cooperative market order | Internal drivers for scheduled or consolidated routes | Freelancers when urgent or capacity constrained | Supports batch pickup and single-package consolidation |
| Non-subscriber standard order | Best eligible fleet by SLA, distance, cost, and availability | Any eligible fleet | Dispatch engine optimizes for service quality and cost |
| Bicycle-eligible local order | Bicycle freelancers or internal driver | Moto freelancer | Requires low weight, short distance, and non-urgent SLA |

## System Responsibilities

SequoRider should provide:

- Rider availability and capacity state.
- Mission proposal, acceptance, rejection, timeout, and reassignment flows.
- Pickup and dropoff instructions using seller/customer names, masked contact actions, neighborhood, landmark, and map pin when available.
- In-app navigation handoff to the selected map provider.
- Status updates for accepted, en route to pickup, arrived at pickup, picked up, en route to dropoff, arrived at dropoff, delivered, failed, and returned.
- Proof-of-delivery capture with PIN, photo, signature, recipient confirmation, or admin override depending on order policy.
- Incident reporting for missing package, customer absent, address unclear, damaged package, payment anomaly, or safety issue.
- Earnings visibility only for freelancers.
- Payout request initiation only for eligible freelancer balances.

## Recommended Shared Domain Modules

The current project is still a starter Kotlin Multiplatform app. As implementation grows, keep courier logic in shared, testable modules:

```text
shared/src/commonMain/kotlin/dev/orestegabo/sequorider/
  core/
    money/
    time/
    location/
    result/
  domain/
    dispatch/
    fleet/
    mission/
    payout/
    route/
  data/
    api/
    local/
    mapper/
  presentation/
    availability/
    mission/
    earnings/
    route/
```

## Data Objects To Model First

- `RiderProfile`
- `RiderClassification`
- `VehicleProfile`
- `FleetEmploymentType`
- `AvailabilityState`
- `CapacitySnapshot`
- `OrderDispatchProfile`
- `MissionOffer`
- `MissionAssignment`
- `MissionLifecycle`
- `CourierRate`
- `DeliveryFeeLedgerEntry`
- `PayoutAccount`
- `PayoutRequest`
- `RouteStop`
- `RoutePlan`
- `ConsolidationBatch`

## Map And Location Requirements

The app must support both map pins and local addressing realities:

- Neighborhood and landmark are required for delivery addresses.
- Map pin is recommended but not sufficient by itself.
- Routes should use road distance and ETA when a map provider is available.
- Straight-line distance is allowed only as a fallback estimate and must be marked as approximate in operational logic.
- Rider live location should be sampled conservatively to protect battery life.
- Customers should see simple status updates; precise rider location exposure must follow privacy policy and safety rules.

See [ROUTE_OPTIMIZATION.md](ROUTE_OPTIMIZATION.md) for route scoring and batching rules.

## Financial Requirements

SequoRider must not calculate final platform finance locally. The mobile app can display server-provided balances and payout states, but the backend remains the source of truth for:

- Courier rates.
- Customer delivery fee.
- Sequo subsidy amount when delivery fee is below courier rate.
- Freelancer earning ledger.
- Internal driver salary exclusion.
- Payout request status.
- Payout provider transfer IDs.
- Corrections, holds, disputes, and audit logs.

See [PAYOUT_STRUCTURE.md](PAYOUT_STRUCTURE.md) for the payout workflow.

## Repository Structure

```text
androidApp/   Android application entry point
iosApp/       iOS application entry point
shared/       Shared Kotlin, Compose UI, domain logic, resources, and tests
```

Current package namespace:

```text
dev.orestegabo.sequorider
```

## Build Commands

Android debug build:

```bash
./gradlew :androidApp:assembleDebug
```

Android shared tests:

```bash
./gradlew :shared:testAndroidHostTest
```

iOS simulator tests:

```bash
./gradlew :shared:iosSimulatorArm64Test
```

## Testing Expectations

The first production tests should cover:

- Subscriber-first internal dispatch with freelancer overflow.
- Express food assignment to moto freelancers.
- Internal driver exclusion from per-order delivery payout.
- Freelancer payout ledger creation after completed delivery.
- Sequo subsidy when customer delivery fee is below courier rate.
- Digital-only payout provider validation for Yas Togo and Moov Africa.
- Route batching for cooperative market pickups.
- Reassignment after rider rejection, timeout, or capacity change.
- PIN or proof-of-delivery validation before mission completion.

## Security And Audit Requirements

- Do not store secrets in the mobile application.
- Never expose customer or rider phone numbers directly when masked contact is available.
- Every mission assignment, reassignment, delivery confirmation, payout request, payout approval, and payout correction must be auditable.
- Payout actions require idempotency keys.
- Rider location must be used only for dispatch, safety, ETA, and delivery verification.
- Admin overrides must capture actor, reason, timestamp, affected IDs, and before/after values.
