# Payout Structure

This document defines how SequoRider handles courier earnings, payout eligibility, provider rules, subsidies, holds, and auditability.

## Core Rule

Internal salaried drivers and independent freelancers are financially separate.

- Internal drivers receive fixed salaries through Sequo payroll.
- Internal drivers are excluded from per-order delivery fee splits.
- Freelancers are paid per completed eligible delivery.
- If the customer delivery fee is lower than the required freelancer courier rate, Sequo covers the difference.
- Payouts are digital only through Yas Togo and Moov Africa.
- Cash withdrawals are not supported.

## Rider Financial Classification

| Rider class | Earnings source | Per-order delivery payout | Can request payout in SequoRider | Notes |
| --- | --- | --- | --- | --- |
| Internal driver | Fixed salary | No | No | App may show mission history and performance, but not withdrawable delivery earnings |
| Moto freelancer | Per completed delivery | Yes | Yes | Required for express food deliveries |
| Bicycle freelancer | Per completed delivery | Yes | Yes | Eligible for short-distance, low-weight, non-urgent deliveries |

## Delivery Fee Terms

| Term | Definition |
| --- | --- |
| Customer delivery fee | Amount charged to the customer for delivery |
| Courier rate | Required amount owed to a freelancer for completing the delivery |
| Sequo subsidy | Difference paid by Sequo when customer delivery fee is lower than courier rate |
| Freelancer gross earning | Courier rate plus approved bonus, before holds or corrections |
| Freelancer net payable | Gross earning minus incident holds, corrections, or approved deductions |
| Internal driver allocation | Operational cost attribution only; never a rider payout split |

Formula for freelancer missions:

```text
freelancerGrossEarning = courierRate + approvedBonus
sequoSubsidy = max(0, courierRate - customerDeliveryFee)
customerDeliveryFeeContribution = min(customerDeliveryFee, courierRate)
freelancerNetPayable = freelancerGrossEarning - holds - corrections
```

Internal driver missions:

```text
freelancerGrossEarning = 0
sequoSubsidy = 0 for courier payout purposes
deliveryFeeSplit = not applicable
```

The finance system may still track internal delivery cost for unit economics, but SequoRider must not represent it as rider earnings.

## Earnings Lifecycle

| Status | Meaning |
| --- | --- |
| `NOT_APPLICABLE` | Internal driver mission or non-payable operational task |
| `ESTIMATED` | Freelancer sees expected payout before accepting |
| `PENDING_COMPLETION` | Mission accepted but not delivered |
| `PENDING_REVIEW` | Delivery completed but proof, incident, or adjustment is under review |
| `AVAILABLE` | Amount is eligible for payout request |
| `ON_HOLD` | Amount is blocked by dispute, missing proof, damaged package, or admin review |
| `REQUESTED` | Freelancer requested payout |
| `PROCESSING` | Finance team or provider transfer is in progress |
| `PAID` | Provider confirms successful transfer |
| `FAILED` | Provider transfer failed |
| `CORRECTED` | Amount changed by audited finance correction |

Earnings become `AVAILABLE` only after:

- The mission is completed.
- Required proof is validated.
- Package reception is recorded for the mission type.
- No blocking incident is active.
- The rider is a freelancer with approved KYC.
- The rider has an active Yas Togo or Moov Africa payout account.

## Package Reception SLA

Payout processing requests must be handled within one week of package reception.

Use this definition by mission type:

| Mission type | Package reception event that starts SLA |
| --- | --- |
| Direct customer delivery | Recipient handoff is confirmed by PIN, signature, photo, or admin-approved proof |
| Relay delivery | Relay partner confirms package reception |
| Batch pickup to Sequo hub | Sequo hub confirms package reception |
| Cooperative market consolidation | Consolidated package is received by the recipient, relay, or Sequo hub depending on route design |
| Return mission | Authorized receiver confirms return package reception |

If a freelancer submits a payout request after amounts are available, finance must process that request no later than seven calendar days after the relevant package reception timestamp. If an incident blocks payout, the hold reason and expected review path must be visible.

## Payout Providers

Supported courier payout providers:

- Yas Togo.
- Moov Africa.

Unsupported payout paths:

- Cash withdrawal.
- Cash handoff by admin.
- Cash handoff by seller.
- Informal mobile-money transfer outside the payout ledger.
- Payout to an unverified phone number.

Provider account requirements:

- Provider name.
- Registered account phone number.
- Account holder name.
- Verification status.
- Last verification timestamp.
- Default payout provider flag.

The backend must validate the provider before creating a payout request. The mobile app should not allow unsupported provider selection.

## Payout Request Workflow

1. Freelancer opens earnings.
2. App loads server-calculated `availableBalance`.
3. Freelancer selects amount up to the available balance.
4. Freelancer selects Yas Togo or Moov Africa.
5. Backend validates KYC, account status, amount, provider, idempotency key, and holds.
6. Backend creates `PayoutRequest`.
7. Status becomes `REQUESTED`.
8. Finance review or automated provider transfer moves status to `PROCESSING`.
9. Provider response marks request `PAID` or `FAILED`.
10. Ledger entries are locked and audit records are retained.

Required payout request fields:

| Field | Purpose |
| --- | --- |
| `payoutRequestId` | Stable payout request identity |
| `riderId` | Freelancer identity |
| `provider` | Yas Togo or Moov Africa |
| `amount` | Requested amount in FCFA |
| `currency` | FCFA |
| `status` | Request status |
| `packageReceptionCutoff` | Latest package reception event included in payout |
| `requestedAt` | Request timestamp |
| `processedBy` | Admin or automated processor |
| `providerTransferId` | Provider-side transfer reference |
| `idempotencyKey` | Duplicate request prevention |

## Ledger Structure

Do not store payout totals as mutable standalone numbers. Use ledger entries.

| Ledger entry type | Sign | Description |
| --- | --- | --- |
| `DELIVERY_EARNING` | Credit | Courier rate for completed freelancer delivery |
| `BONUS` | Credit | Quality, volume, zone, or weather bonus approved by Sequo |
| `SEQUO_SUBSIDY` | Credit metadata | Records Sequo-funded gap when customer fee is below courier rate |
| `INCIDENT_HOLD` | Hold | Temporarily blocks amount pending review |
| `CORRECTION_CREDIT` | Credit | Admin-approved correction increasing payable amount |
| `CORRECTION_DEBIT` | Debit | Admin-approved correction reducing payable amount |
| `PAYOUT_DEBIT` | Debit | Amount moved into payout processing |
| `PAYOUT_FAILURE_REVERSAL` | Credit | Restores balance after provider failure |

`SEQUO_SUBSIDY` should be visible in finance reporting. It does not reduce the freelancer earning.

## Examples

### Customer Fee Covers Courier Rate

```text
customerDeliveryFee = 700 FCFA
courierRate = 600 FCFA
sequoSubsidy = 0 FCFA
freelancerGrossEarning = 600 FCFA
```

The remaining 100 FCFA is platform delivery margin before other costs, depending on finance rules.

### Sequo Covers The Difference

```text
customerDeliveryFee = 400 FCFA
courierRate = 650 FCFA
sequoSubsidy = 250 FCFA
freelancerGrossEarning = 650 FCFA
```

The freelancer receives the full courier rate. Sequo absorbs the shortfall.

### Internal Driver Delivery

```text
customerDeliveryFee = 400 FCFA
courierRate = not applicable
freelancerGrossEarning = 0 FCFA
deliveryFeeSplit = not applicable
```

The driver is paid by salary, not by the order.

## Holds And Disputes

Create a hold when:

- Delivery proof is missing.
- PIN is invalid or disputed.
- Package is reported damaged.
- Package is reported missing.
- Customer or seller opens a high-risk complaint.
- Rider was reassigned after pickup and custody is unclear.
- Admin marks mission for finance review.

Hold rules:

- Holds must block only the affected ledger amount when possible.
- Holds must have a reason code.
- Holds must have an owner or queue.
- Holds must be visible to support/admin.
- Freelancer-facing text should be plain and non-accusatory.
- Release or deduction requires an audit record.

## Bonuses

Bonuses are optional and must be configured by admin.

Possible bonus types:

- High-demand zone bonus.
- Heavy rain or difficult-route bonus.
- Quality bonus.
- Volume bonus.
- Manual incident compensation.

Bonuses apply only to freelancers unless explicitly modeled as internal payroll performance metadata. A bonus displayed in SequoRider must never imply salary adjustment for an internal driver.

## Payout Screen Rules

Freelancer earnings screen should show:

- Available balance.
- Pending completion.
- Pending review.
- On hold.
- Last payout request.
- Payout provider.
- Mission-level earning history.
- Bonus history.
- Failed payout retry action when allowed.

Internal driver screen should show:

- Completed missions.
- Current route or shift summary.
- Performance metrics approved for internal display.
- No withdrawable balance.
- No per-order payout call to action.

## Admin And Finance Controls

Admin/finance must be able to:

- View freelancer balances.
- View internal driver mission volume without payout balances.
- Filter payout requests by provider, status, rider, and date.
- Approve or reject payout requests.
- Retry failed provider transfers.
- Place or release holds.
- Add audited corrections.
- Export payout reports.
- View Sequo subsidy totals by zone, service type, and rider class.

High-risk actions require reason capture:

- Manual payout approval.
- Payout rejection.
- Balance correction.
- Hold release.
- Provider account change.
- KYC override.

## Audit Requirements

Every payout-related action must store:

- Actor.
- Timestamp.
- Source application.
- Rider ID.
- Mission ID or payout request ID.
- Before value.
- After value.
- Reason code.
- Free-text note when required.
- Idempotency key for write actions.

Provider transfer records must store:

- Provider.
- Request payload hash.
- Provider response code.
- Provider transfer ID.
- Attempt count.
- Final state.

## Validation Rules

Reject payout request when:

- Rider is internal.
- Rider KYC is not approved.
- Provider is not Yas Togo or Moov Africa.
- Provider account is not verified.
- Requested amount is zero or negative.
- Requested amount exceeds available balance.
- Included ledger entries are on hold.
- Currency is not FCFA.
- Duplicate idempotency key already succeeded.

## Test Cases

Minimum coverage:

- Internal driver completes delivery and no earning ledger is created.
- Moto freelancer completes express food delivery and earns courier rate.
- Bicycle freelancer is blocked from express food by default.
- Customer fee below courier rate creates Sequo subsidy metadata.
- Payout provider validation rejects unsupported providers.
- Payout request can be made only against available balance.
- Payout request SLA is calculated from package reception timestamp.
- Failed provider transfer restores payable balance.
- Incident hold prevents payout until released.

