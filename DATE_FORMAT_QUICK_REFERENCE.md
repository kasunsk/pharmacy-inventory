# Date Format Quick Reference - dd-mm-yyyy

## Frontend - Display Dates
```javascript
import { formatDate, formatDateTime } from '../utils/dateUtils';
<td>{formatDate(item.expiryDate)}</td>        // 2026-01-15 → 15-01-2026
<p>{formatDateTime(item.dateTime)}</p>       // Returns: 15-01-2026 14:30
```

## Frontend - Input Hints
```javascript
import { getDatePlaceholder } from '../utils/dateUtils';
<input type="date" placeholder={getDatePlaceholder()} />  // Shows: dd-mm-yyyy
```

## Backend - Automatic Conversion
All LocalDate fields serialize to "dd-mm-yyyy" format automatically via DateFormatConfiguration bean.

## Components Updated
- InventoryListPage: Expiry date display + input
- ProfilePage: Birthdate hints
- BillingPageV2: Bill datetime formatted
- TransactionHistoryPage: Date filters + display
- PrescriptionSalesPage: Bill datetime formatted

## Files Created
- frontend/src/utils/dateUtils.js (180 lines)
- backend/src/main/java/.../config/DateFormatConfiguration.java (70 lines)
- backend/src/main/java/.../util/ValidDateFormat.java (60 lines)
- DATE_FORMAT_STANDARD.md (comprehensive guide)
- DATE_FORMAT_IMPLEMENTATION_SUMMARY.md (implementation details)

