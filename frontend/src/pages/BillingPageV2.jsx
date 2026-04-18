import { useEffect, useMemo, useRef, useState } from 'react';
import { createPrescriptionSale, fetchBillingMedicines } from '../api';

const USAGE_OPTIONS = [
  { value: 'ORAL_ONCE_DAILY_AFTER_MEALS', label: 'Oral - Take 1 tablet once daily after meals' },
  { value: 'ORAL_TWICE_DAILY_AFTER_FOOD', label: 'Oral - Take 1 tablet twice daily (morning and evening) after food' },
  { value: 'ORAL_THREE_TIMES_BEFORE_MEALS', label: 'Oral - Take 1 tablet three times daily before meals' },
  { value: 'TOPICAL_TWICE_DAILY', label: 'Topical - Apply externally twice daily' },
  { value: 'INJECTION_AS_DIRECTED', label: 'Injection - Take as directed by physician' },
  { value: 'CUSTOM', label: 'Custom instruction' }
];

function money(value) {
  return Number(value || 0).toFixed(2);
}

function round2(value) {
  return Math.round(Number(value || 0) * 100) / 100;
}

function emptyRow() {
  return {
    medicineQuery: '',
    medicineId: '',
    quantity: 1,
    unitType: '',
    sellingPrice: 0,
    usageInstruction: USAGE_OPTIONS[0].value,
    customUsageInstruction: '',
    remark: ''
  };
}

export default function BillingPageV2() {
  const [inventory, setInventory] = useState([]);
  const [rows, setRows] = useState([emptyRow()]);
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [latestBill, setLatestBill] = useState(null);
  const [completionAction, setCompletionAction] = useState('');
  const [deliveryValue, setDeliveryValue] = useState('');
  const [completionMessage, setCompletionMessage] = useState('');
  const [billDiscount, setBillDiscount] = useState('0');
  const [openSuggestionsRowIndex, setOpenSuggestionsRowIndex] = useState(null);
  const [dropdownAnchor, setDropdownAnchor] = useState(null); // {top|bottom, left, minWidth}
  const medicineInputRefs = useRef({});

  const inventoryById = useMemo(() => new Map(inventory.map((item) => [String(item.id), item])), [inventory]);

  const calculatedRows = useMemo(() => {
    return rows.map((row) => {
      const quantity = Number(row.quantity || 0);
      const sellingPrice = Number(row.sellingPrice || 0);
      const subTotal = round2(quantity * sellingPrice);
      return {
        ...row,
        quantity,
        sellingPrice,
        subTotal,
        total: subTotal,
        effectiveUnitPrice: sellingPrice
      };
    });
  }, [rows]);

  const totals = useMemo(() => {
    const subTotal = calculatedRows.reduce((acc, row) => acc + row.subTotal, 0);
    const parsedDiscount = Number(billDiscount || 0);
    const discount = Math.min(Math.max(parsedDiscount, 0), subTotal);
    const payable = round2(subTotal - discount);
    return { subTotal, discount, payable };
  }, [calculatedRows, billDiscount]);

  useEffect(() => {
    loadBillingMedicines();
  }, []);

  async function loadBillingMedicines() {
    setLoading(true);
    setError('');
    try {
      setInventory(await fetchBillingMedicines());
    } catch (e) {
      // Keep billing UX clean for limited users and avoid exposing module-level technical errors.
      if (e.status === 401 || e.status === 403) {
        setInventory([]);
        return;
      }
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  function getAllowedUnits(medicine) {
    if (Array.isArray(medicine?.unitOptions) && medicine.unitOptions.length) {
      return medicine.unitOptions.map((option) => option.unitType);
    }
    const units = Array.isArray(medicine?.allowedUnits) && medicine.allowedUnits.length
      ? medicine.allowedUnits
      : [medicine?.unitType].filter(Boolean);
    return units;
  }

  function getUnitOption(medicine, unitType) {
    if (!medicine || !Array.isArray(medicine.unitOptions)) {
      return null;
    }
    const normalised = (unitType || '').toLowerCase();
    return (
      medicine.unitOptions.find((option) => (option.unitType || '').toLowerCase() === normalised) || null
    );
  }

  function getRequiredBaseUnits(row, medicine) {
    const selectedUnit = getUnitOption(medicine, row.unitType);
    const conversion = Number(selectedUnit?.conversionToBase || 1);
    return Number(row.quantity || 0) * conversion;
  }

  function updateRow(index, patch) {
    const next = [...rows];
    next[index] = { ...next[index], ...patch };

    if (patch.medicineQuery !== undefined && patch.medicineId === undefined) {
      next[index].medicineId = '';
      next[index].unitType = '';
      next[index].sellingPrice = 0;
    }

    if (patch.medicineId !== undefined) {
      const selected = inventoryById.get(String(next[index].medicineId));
      next[index].medicineQuery = selected?.name || next[index].medicineQuery;
      const allowedUnits = getAllowedUnits(selected);
      const defaultUnit = allowedUnits[0] || '';
      const defaultOption = getUnitOption(selected, defaultUnit);
      next[index].unitType = defaultUnit;
      next[index].sellingPrice = Number(defaultOption?.sellingPrice ?? selected?.sellingPrice ?? 0);
    }

    if (patch.unitType !== undefined) {
      const selected = inventoryById.get(String(next[index].medicineId));
      const selectedOption = getUnitOption(selected, patch.unitType);
      if (selectedOption) {
        next[index].sellingPrice = Number(selectedOption.sellingPrice || 0);
      } else if (selected) {
        // fallback: use medicine's base selling price when no per-unit option is configured
        next[index].sellingPrice = Number(selected.sellingPrice || 0);
      }
    }

    if (patch.usageInstruction && patch.usageInstruction !== 'CUSTOM') {
      next[index].customUsageInstruction = '';
    }

    setRows(next);
  }

  function addRow() {
    setRows([...rows, emptyRow()]);
  }

  function removeRow(index) {
    if (rows.length === 1) {
      return;
    }
    setRows(rows.filter((_, idx) => idx !== index));
  }

  function validate() {
    calculatedRows.forEach((row, index) => {
      const selected = inventoryById.get(String(row.medicineId));
      const label = `Row ${index + 1}`;
      if (!row.medicineId) {
        throw new Error(`${label}: medicine is required.`);
      }
      if (selected && Number(selected.quantity) <= 0) {
        throw new Error(`${label}: selected medicine is out of stock.`);
      }
      if (row.quantity <= 0) {
        throw new Error(`${label}: quantity must be greater than zero.`);
      }

      const allowedUnits = getAllowedUnits(selected);
      if (!row.unitType || !allowedUnits.includes(row.unitType)) {
        throw new Error(`${label}: select a valid unit for the medicine.`);
      }

      const requiredBaseUnits = getRequiredBaseUnits(row, selected);
      if (selected && requiredBaseUnits > Number(selected.quantity || 0)) {
        throw new Error(`${label}: quantity exceeds available stock for ${row.unitType}.`);
      }

      if (row.usageInstruction === 'CUSTOM' && !row.customUsageInstruction.trim()) {
        throw new Error(`${label}: custom instruction is required.`);
      }
    });

    const parsedDiscount = Number(billDiscount || 0);
    if (parsedDiscount < 0) {
      throw new Error('Final discount cannot be negative.');
    }
    if (parsedDiscount > totals.subTotal) {
      throw new Error('Final discount cannot exceed subtotal.');
    }
  }

  function mapUsage(row) {
    if (row.usageInstruction === 'CUSTOM') {
      return {
        dosageInstruction: 'CUSTOM',
        customDosageInstruction: row.customUsageInstruction.trim()
      };
    }
    const option = USAGE_OPTIONS.find((item) => item.value === row.usageInstruction);
    return {
      dosageInstruction: option ? option.label : row.usageInstruction,
      customDosageInstruction: null
    };
  }

  function filteredMedicines(query) {
    const text = query.trim().toLowerCase();
    const sorted = [...inventory].sort((a, b) => {
      const aName = a.name.toLowerCase();
      const bName = b.name.toLowerCase();
      const aOut = Number(a.quantity) <= 0;
      const bOut = Number(b.quantity) <= 0;
      if (aOut !== bOut) {
        return aOut ? 1 : -1;
      }
      if (text) {
        const aStarts = aName.startsWith(text);
        const bStarts = bName.startsWith(text);
        if (aStarts !== bStarts) {
          return aStarts ? -1 : 1;
        }
      }
      return aName.localeCompare(bName);
    });

    if (!text) {
      return sorted.slice(0, 5);
    }

    return sorted.filter((item) => item.name.toLowerCase().includes(text));
  }

  function selectMedicineFromSearch(index, medicine) {
    updateRow(index, {
      medicineId: String(medicine.id),
      medicineQuery: medicine.name
    });
    setOpenSuggestionsRowIndex(null);
    setDropdownAnchor(null);
  }

  function openDropdownForRow(index) {
    const inputEl = medicineInputRefs.current[index];
    if (inputEl) {
      const rect = inputEl.getBoundingClientRect();
      const DROPDOWN_MAX_HEIGHT = 240;
      const GAP = 4;
      const spaceBelow = window.innerHeight - rect.bottom;
      const spaceAbove = rect.top;
      const showAbove = spaceBelow < DROPDOWN_MAX_HEIGHT + GAP && spaceAbove > spaceBelow;
      setDropdownAnchor(
        showAbove
          ? { bottom: window.innerHeight - rect.top + GAP, left: rect.left, minWidth: rect.width }
          : { top: rect.bottom + GAP, left: rect.left, minWidth: rect.width }
      );
    }
    setOpenSuggestionsRowIndex(index);
  }

  async function submitSale(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      validate();
      const payload = {
        customerName: customerName || null,
        customerPhone: customerPhone || null,
        discountAmount: Number(billDiscount || 0),
        items: calculatedRows.map((row) => {
          const selected = inventoryById.get(String(row.medicineId));
          const usage = mapUsage(row);
          return {
            medicineId: Number(row.medicineId),
            medicineName: selected?.name || '',
            quantity: row.quantity,
            unitType: row.unitType,
            pricePerUnit: row.effectiveUnitPrice,
            allowPriceOverride: true,
            dosageInstruction: usage.dosageInstruction,
            customDosageInstruction: usage.customDosageInstruction,
            remark: row.remark?.trim() || null
          };
        })
      };
      const bill = await createPrescriptionSale(payload);
      setLatestBill(bill);
      setCompletionAction('');
      setDeliveryValue('');
      setCompletionMessage('');
      setRows([emptyRow()]);
      setBillDiscount('0');
      setCustomerName('');
      setCustomerPhone('');
      await loadBillingMedicines();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  function handleCompletionAction(action) {
    setCompletionAction(action);
    setDeliveryValue('');
    if (action === 'PRINT') {
      setCompletionMessage('Print simulation ready. Open browser print preview to demonstrate.');
      return;
    }
    setCompletionMessage('');
  }

  function submitDeliveryAction() {
    const value = deliveryValue.trim();
    if (!completionAction || completionAction === 'PRINT') {
      return;
    }
    if (!value) {
      setCompletionMessage(completionAction === 'EMAIL'
        ? 'Please enter a customer email address.'
        : 'Please enter a customer phone number.');
      return;
    }
    if (completionAction === 'EMAIL') {
      setCompletionMessage(`Demo only: Bill sent via email to ${value}.`);
      return;
    }
    setCompletionMessage(`Demo only: Bill sent via SMS to ${value}.`);
  }

  return (
    <section>
      <div className="page-title-row">
        <h2>Billing</h2>
        <button type="button" onClick={loadBillingMedicines} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh Inventory'}
        </button>
      </div>

      {!loading && inventory.length === 0 && (
        <p className="muted">No medicines are available for billing right now.</p>
      )}

      {error && <p className="error">{error}</p>}

      <form className="panel" onSubmit={submitSale}>
        <div className="billing-customer-row">
          <label>
            Customer name (optional)
            <input value={customerName} onChange={(event) => setCustomerName(event.target.value)} />
          </label>
          <label>
            Customer phone (optional)
            <input value={customerPhone} onChange={(event) => setCustomerPhone(event.target.value)} />
          </label>
        </div>

        <div className="table-wrap billing-table-wrap">
          <table>
            <thead>
              <tr>
                <th>Medicine</th>
                <th>Unit</th>
                <th>Selling price / unit</th>
                <th>Quantity</th>
                <th>Total price</th>
                <th>Usage instructions</th>
                <th>Remark</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {calculatedRows.map((row, index) => (
                <tr key={index}>
                  <td>
                    <div className="medicine-search-wrap">
                      <input
                        required
                        ref={(el) => { medicineInputRefs.current[index] = el; }}
                        value={row.medicineQuery}
                        onChange={(event) => {
                          updateRow(index, { medicineQuery: event.target.value });
                          openDropdownForRow(index);
                        }}
                        onFocus={() => openDropdownForRow(index)}
                        onBlur={() => {
                          setTimeout(() => {
                            setOpenSuggestionsRowIndex((current) => {
                              if (current === index) {
                                setDropdownAnchor(null);
                                return null;
                              }
                              return current;
                            });
                          }, 150);
                        }}
                        placeholder="Click or type to search medicines"
                      />
                      {openSuggestionsRowIndex === index && dropdownAnchor && (
                        <div
                          className="medicine-suggestions"
                          role="listbox"
                          style={{
                            position: 'fixed',
                            left: dropdownAnchor.left,
                            minWidth: dropdownAnchor.minWidth,
                            ...(dropdownAnchor.top !== undefined
                              ? { top: dropdownAnchor.top }
                              : { bottom: dropdownAnchor.bottom })
                          }}
                        >
                          {filteredMedicines(row.medicineQuery).map((item) => {
                            const out = Number(item.quantity) <= 0;
                            return (
                              <button
                                key={item.id}
                                type="button"
                                className="ghost medicine-suggestion-item"
                                onMouseDown={(event) => event.preventDefault()}
                                onClick={() => !out && selectMedicineFromSearch(index, item)}
                                disabled={out}
                              >
                                {item.name} (stock: {item.quantity}){out ? ' — Out of stock' : ''}
                              </button>
                            );
                          })}
                          {filteredMedicines(row.medicineQuery).length === 0 && (
                            <p className="muted">No matching medicines found.</p>
                          )}
                          {!row.medicineQuery.trim() && (
                            <p className="muted">Showing top 5. Type to search all.</p>
                          )}
                        </div>
                      )}
                    </div>
                  </td>
                  <td>
                    <select
                      value={row.unitType}
                      disabled={!row.medicineId}
                      onChange={(event) => updateRow(index, { unitType: event.target.value })}
                    >
                      {!row.unitType && <option value="">Select unit</option>}
                      {getAllowedUnits(inventoryById.get(String(row.medicineId))).map((unit) => {
                        const selectedMedicine = inventoryById.get(String(row.medicineId));
                        const option = getUnitOption(selectedMedicine, unit);
                        const stockLabel = option ? ` (${option.availableQuantity})` : '';
                        return <option key={unit} value={unit}>{`${unit}${stockLabel}`}</option>;
                      })}
                    </select>
                  </td>
                  <td><input value={money(row.sellingPrice)} readOnly /></td>
                  <td>
                    <input
                      type="number"
                      min="1"
                      value={row.quantity}
                      onChange={(event) => updateRow(index, { quantity: event.target.value })}
                    />
                  </td>
                  <td className="strong amount-cell">{money(row.total)}</td>
                  <td>
                    <select
                      value={row.usageInstruction}
                      onChange={(event) => updateRow(index, { usageInstruction: event.target.value })}
                    >
                      {USAGE_OPTIONS.map((item) => (
                        <option key={item.value} value={item.value}>{item.label}</option>
                      ))}
                    </select>
                    {row.usageInstruction === 'CUSTOM' && (
                      <input
                        required
                        placeholder="Type custom instruction"
                        value={row.customUsageInstruction}
                        onChange={(event) => updateRow(index, { customUsageInstruction: event.target.value })}
                      />
                    )}
                  </td>
                  <td>
                    <input
                      placeholder="Optional"
                      value={row.remark}
                      onChange={(event) => updateRow(index, { remark: event.target.value })}
                    />
                  </td>
                  <td>
                    <button type="button" className="ghost" onClick={() => removeRow(index)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="billing-footer">
          <button type="button" className="plus-btn" onClick={addRow} title="Add medicine row">+</button>
          <label className="discount-input">
            Final bill discount (LKR)
            <input
              type="number"
              min="0"
              step="0.01"
              value={billDiscount}
              onChange={(event) => setBillDiscount(event.target.value)}
            />
          </label>
          <div className="totals-box">
            <span>Subtotal: {money(totals.subTotal)}</span>
            <span>Discount: {money(totals.discount)}</span>
            <span className="strong">Final payable: {money(totals.payable)}</span>
          </div>
          <button type="submit" disabled={saving || loading || inventory.length === 0}>
            {saving ? 'Completing billing...' : 'Complete Billing'}
          </button>
        </div>
      </form>

      {latestBill && (
        <article className="panel">
          <h3>Latest Bill - {latestBill.transactionId}</h3>
          <p>
            <strong>Date:</strong> {new Date(latestBill.dateTime).toLocaleString()} | <strong>Sales person:</strong> {latestBill.salesPerson}
          </p>
          <p>
            <strong>Customer:</strong> {latestBill.customerName || 'Walk-in'} | <strong>Phone:</strong> {latestBill.customerPhone || '-'}
          </p>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Medicine</th>
                  <th>Qty</th>
                  <th>Unit</th>
                  <th>Remark</th>
                  <th>Price/Unit</th>
                  <th>Line Total</th>
                </tr>
              </thead>
              <tbody>
                {latestBill.items.map((item, index) => (
                  <tr key={index}>
                    <td>{item.medicineName}</td>
                    <td>{item.quantity}</td>
                    <td>{item.unitType}</td>
                    <td>{item.remark || '-'}</td>
                    <td>{money(item.pricePerUnit)}</td>
                    <td>{money(item.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="bill-totals">
            <span>Total before discount: {money(latestBill.totalBeforeDiscount)}</span>
            <span>Discount: {money(latestBill.discountAmount)}</span>
            <span className="strong">Total amount: {money(latestBill.totalAmount)}</span>
          </div>

          <div className="bill-completion-actions">
            <h4>Bill completion options (Demo)</h4>
            <div className="bill-action-buttons">
              <button type="button" className="ghost" onClick={() => handleCompletionAction('PRINT')}>Print Bill</button>
              <button type="button" className="ghost" onClick={() => handleCompletionAction('EMAIL')}>Send via Email</button>
              <button type="button" className="ghost" onClick={() => handleCompletionAction('SMS')}>Send via SMS</button>
            </div>
            {completionAction === 'EMAIL' && (
              <div className="bill-delivery-input-row">
                <input
                  type="email"
                  placeholder="customer@example.com"
                  value={deliveryValue}
                  onChange={(event) => setDeliveryValue(event.target.value)}
                />
                <button type="button" onClick={submitDeliveryAction}>Simulate Email</button>
              </div>
            )}
            {completionAction === 'SMS' && (
              <div className="bill-delivery-input-row">
                <input
                  placeholder="0771234567"
                  value={deliveryValue}
                  onChange={(event) => setDeliveryValue(event.target.value)}
                />
                <button type="button" onClick={submitDeliveryAction}>Simulate SMS</button>
              </div>
            )}
            {completionMessage && <p className="success-banner">{completionMessage}</p>}
          </div>
        </article>
      )}
    </section>
  );
}

