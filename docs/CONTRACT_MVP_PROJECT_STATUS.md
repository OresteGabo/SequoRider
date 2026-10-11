# Sequo Rider MVP Status

Source contract: `/Users/muhirwagabooreste/AndroidStudioProjects/SequoService/sequo-api/docs/contracts/contrat_mvp_sequo.tex`

This file tracks only contract-listed MVP scope for the rider app.

| Contract item | Status | Progress | Evidence | Remaining MVP work |
| --- | --- | ---: | --- | --- |
| Rider login/account context | In progress | 55% | App shell and network/core scaffolding | Complete role guard and `/api/auth/me` display. |
| Rider receives or accepts missions | In progress | 65% | Mission docs, recent API client/repository wiring | Verify against rider-scoped mission endpoints and real tokens. |
| Rider picks up from merchant/SEQUO/relay | In progress | 50% | Delivery and route docs plus partial UI | Complete pickup action flow and endpoint calls. |
| Rider delivers to customer or deposits at relay | In progress | 45% | Delivery/relay domains and app docs | Complete delivery/deposit validation UI. |
| Rider validates delivery steps in app | In progress | 45% | Backend delivery/PIN services | Wire proof/check steps in UI without raw errors. |
| Rider sees independent courier pay calculation | In progress | 30% | `PAYOUT_STRUCTURE.md`, settlement domain | Add rider payout visibility API and app screen wiring. |
| Salaried courier exclusion from freelancer pay | In progress | 60% | API settlement/delivery workforce models | Ensure rider UI distinguishes non-payable salaried records. |
| Notifications for rider missions | In progress | 55% | Notification domain exists | Confirm app-family registration and UI states. |

Recommended verification:

```bash
./gradlew :androidApp:compileDebugKotlin
```
