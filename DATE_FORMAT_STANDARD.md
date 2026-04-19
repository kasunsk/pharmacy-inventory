# Global Date Format Standard: dd-mm-yyyy

This document outlines the standardized date format used throughout the Pharmacy Inventory Management System to ensure consistency across all UI components, APIs, and data handling.

## Standard Format

**All dates must be displayed and processed in: `dd-mm-yyyy` format**

Examples:
- January 15, 2026 → `15-01-2026`
- December 31, 2025 → `31-12-2025`
- March 1, 2024 → `01-03-2024`

## Frontend Implementation

### Date Utility Functions

Located in: `frontend/src/utils/dateUtils.js`

#### Available Functions

```javascript
import {
  formatDate,           // Format Date/ISO string to dd-mm-yyyy
  parseToISOFormat,     // Parse dd-mm-yyyy to YYYY-MM-DD for HTML inputs
  parseToDate,          // Parse dd-mm-yyyy string to Date object
  isValidDateFormat,    // Validate dd-mm-yyyy format
  formatDateTime,       // Format datetime to dd-mm-yyyy HH:mm
  getDatePlaceholder,   // Get placeholder text (dd-mm-yyyy)
  getDateTimePlaceholder // Get placeholder text (dd-mm-yyyy HH:mm)
} from '../utils/dateUtils';
```

### Usage Examples

```javascript
// Display a date from API response
import { formatDate } from '../utils/dateUtils';

const medicine = { expiryDate: '2026-03-15' };
<td>{formatDate(medicine.expiryDate)}</td>  // Displays: 15-03-2026

// Format datetime for display
import { formatDateTime } from '../utils/dateUtils';

const bill = { dateTime: '2026-01-15T14:30:00Z' };
<p>Date: {formatDateTime(bill.dateTime)}</p>  // Displays: 15-01-2026 14:30

// Add placeholder to date inputs
import { getDatePlaceholder } from '../utils/dateUtils';

<input type="date" placeholder={getDatePlaceholder()} />  // Shows: dd-mm-yyyy
```

### Components Using Date Format

| Component | Date Fields | Implementation |
|-----------|------------|-----------------|
| InventoryListPage | Expiry date (display & input) | formatDate() for display, placeholder for input |
| ProfilePage | Birthdate | getDatePlaceholder() for hints |
| BillingPageV2 | Transaction datetime | formatDateTime() for display |
| TransactionHistoryPage | Date filters, transaction dates | formatDateTime() for display, getDatePlaceholder() for inputs |
| PrescriptionSalesPage | Bill datetime, history dates | formatDateTime() for display |

## Backend Implementation

### Date Format Configuration

Located in: `backend/src/main/java/lk/pharmacy/inventory/config/DateFormatConfiguration.java`

The backend automatically serializes/deserializes `LocalDate` instances to/from `dd-mm-yyyy` format:

- **JSON Serialization**: `LocalDate` → `"15-01-2026"` (dd-mm-yyyy string)
- **JSON Deserialization**: `"15-01-2026"` → `LocalDate` (supports both dd-mm-yyyy and YYYY-MM-DD)

### Custom Validation

Located in: `backend/src/main/java/lk/pharmacy/inventory/util/ValidDateFormat.java`

Use the `@ValidDateFormat` annotation to validate date format strings:

```java
public record UserProfileRequest(
    String firstName,
    @ValidDateFormat
    String birthdate,  // Must be in dd-mm-yyyy format
    String email
) {
}
```

### API Date Handling

All API endpoints automatically handle date conversion:

```java
// Controllers receive LocalDate parameters
@GetMapping
public Page<SaleTransactionSummaryResponse> list(
    @RequestParam(required = false) LocalDate fromDate,  // Parsed from dd-mm-yyyy
    @RequestParam(required = false) LocalDate toDate     // Parsed from dd-mm-yyyy
) {
    // fromDate and toDate are automatically parsed
    return salesService.findTransactions(fromDate, toDate, page, size);
}

// Responses include LocalDate fields
public record SaleBillResponse(
    Instant dateTime,                    // Server timestamp (Instant)
    LocalDate transactionDate           // User-facing date (dd-mm-yyyy)
) {
}
```

## Data Storage

### Database

Dates are stored in the database using their native formats:
- `LocalDate` → SQL `DATE` type (YYYY-MM-DD internally)
- `Instant` → SQL `TIMESTAMP` type (with timezone)

**Format conversion only occurs during serialization/deserialization, not in storage.**

### Birthdate and User Profile Dates

User birthdates are stored as `VARCHAR` in the database and formatted on display/input:

```sql
-- User.birthdate stored as VARCHAR in YYYY-MM-DD format
-- Displayed in UI as dd-mm-yyyy
-- Accepted from UI in both dd-mm-yyyy and YYYY-MM-DD formats
```

## Migration Guide (For Existing Data)

If existing data uses a different date format, apply these conversions:

### Database Migration

```sql
-- Update Expiry Dates (if currently stored in different format)
-- Example: If stored as 'yyyy-MM-dd', re-insert as 'yyyy-MM-dd'
-- The UI automatically converts for display
```

### Frontend

Existing components automatically use the new format after deployment:
- No manual data conversion required
- Backend handles bidirectional conversion
- Edge cases like null values are handled gracefully

## Error Handling

### Invalid Date Formats

**Frontend Validation:**
```javascript
import { isValidDateFormat } from '../utils/dateUtils';

if (!isValidDateFormat(userInput)) {
  showError('Invalid date format. Use dd-mm-yyyy');
}
```

**Backend Validation:**
```
POST /users with birthdate: "15/01/2026"  → Error: Invalid date format
POST /users with birthdate: "15-01-2026"  → Success
POST /users with birthdate: "2026-01-15"  → Success (fallback parsing)
```

## Timezone Handling

- **Local Dates (`LocalDate`)**: No timezone, represents date only
- **Timestamps (`Instant`)**: UTC timezone, precise moment in time
- **Display**: All dates displayed in user's system timezone via browser's `toLocaleString()`

## Exports and Reports

### PDF/CSV Exports

All exports use the standardized `dd-mm-yyyy` format:

```javascript
// When exporting bills to PDF
const dateText = formatDate(bill.expiryDate);  // 15-01-2026
// PDF includes: Expiry: 15-01-2026
```

### Query Parameters

API query parameters accept both formats:
```
GET /sales?fromDate=15-01-2026&toDate=31-12-2026  ✓ Works
GET /sales?fromDate=2026-01-15&toDate=2026-12-31  ✓ Works (fallback)
```

## Best Practices

### For Frontend Developers

1. **Always use utility functions** for date display:
   ```javascript
   // ✓ Good
   <td>{formatDate(medicine.expiryDate)}</td>
   
   // ✗ Avoid
   <td>{medicine.expiryDate}</td>
   ```

2. **Provide format hints** in date inputs:
   ```javascript
   <input 
     type="date" 
     placeholder={getDatePlaceholder()}
     title="Format: dd-mm-yyyy"
   />
   ```

3. **Use native HTML date inputs** (`type="date"`):
   - Browser handles conversion to ISO format internally
   - Utility functions handle the conversion

### For Backend Developers

1. **Always use `LocalDate` for dates** (not `String` or `Date`):
   ```java
   // ✓ Good
   private LocalDate expiryDate;
   
   // ✗ Avoid
   private String expiryDate;  // Already deprecated in User entity
   ```

2. **Register `DateFormatConfiguration` bean**:
   - Automatically applied by Spring
   - Handles all LocalDate serialization/deserialization

3. **Validate date input**:
   ```java
   public record CreateUserRequest(
       @NotNull
       @ValidDateFormat
       String birthdate
   ) {}
   ```

## Testing

### Frontend Tests

```javascript
import { formatDate, isValidDateFormat, parseToDate } from '../utils/dateUtils';

describe('dateUtils', () => {
  test('formatDate converts ISO to dd-mm-yyyy', () => {
    expect(formatDate('2026-01-15')).toBe('15-01-2026');
  });

  test('isValidDateFormat validates dd-mm-yyyy', () => {
    expect(isValidDateFormat('15-01-2026')).toBe(true);
    expect(isValidDateFormat('2026-01-15')).toBe(false);
  });

  test('handles null values gracefully', () => {
    expect(formatDate(null)).toBe('');
    expect(formatDate('')).toBe('');
  });
});
```

### Backend Tests

```java
@Test
public void testLocalDateSerialization() {
    LocalDate date = LocalDate.of(2026, 1, 15);
    String json = objectMapper.writeValueAsString(date);
    assertThat(json).contains("15-01-2026");
}

@Test
public void testLocalDateDeserialization() {
    String json = "\"15-01-2026\"";
    LocalDate date = objectMapper.readValue(json, LocalDate.class);
    assertThat(date).isEqualTo(LocalDate.of(2026, 1, 15));
}
```

## Troubleshooting

### Issue: Date displays incorrectly in UI

**Solution:**
```javascript
// Check if using formatDate() utility
import { formatDate } from '../utils/dateUtils';
<td>{formatDate(item.dateField)}</td>  // Correct

// Verify API response includes date field
console.log(item);  // Check date field exists
```

### Issue: Backend API rejects date input

**Solution:**
```java
// Check date format in error message
// POST /users with: {"birthdate": "15/01/2026"}
// Error: "Invalid date format. Expected: dd-mm-yyyy or YYYY-MM-DD"

// Correct format:
// {"birthdate": "15-01-2026"}  // or "2026-01-15"
```

### Issue: Exported dates show as timestamp

**Solution:**
```javascript
// Check export function includes formatDate()
const exportRow = {
  ...item,
  expiryDate: formatDate(item.expiryDate)  // Add formatting
};
```

## References

- [Frontend Date Utils](../frontend/src/utils/dateUtils.js)
- [Backend Date Configuration](../backend/src/main/java/lk/pharmacy/inventory/config/DateFormatConfiguration.java)
- [Date Validation](../backend/src/main/java/lk/pharmacy/inventory/util/ValidDateFormat.java)
- Java `java.time` Documentation
- JavaScript `Intl` API Documentation

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-15 | Initial implementation of dd-mm-yyyy format standard |

