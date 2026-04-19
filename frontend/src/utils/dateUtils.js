/**
 * Global date format standard: dd-mm-yyyy
 * This module provides utility functions for formatting and parsing dates consistently across the application.
 */

const DATE_FORMAT = 'dd-mm-yyyy';
const DATE_FORMAT_PATTERN = /^\d{2}-\d{2}-\d{4}$/;

/**
 * Format a Date object or ISO string to dd-mm-yyyy format
 * @param {Date|string|null} dateInput - Date object, ISO string (YYYY-MM-DD), or null
 * @returns {string} Formatted date as dd-mm-yyyy or empty string if null/invalid
 */
export function formatDate(dateInput) {
  if (!dateInput) {
    return '';
  }

  let date;
  if (typeof dateInput === 'string') {
    // Handle ISO format (YYYY-MM-DD) from HTML date inputs
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateInput)) {
      date = new Date(dateInput + 'T00:00:00Z');
    } else {
      date = new Date(dateInput);
    }
  } else if (dateInput instanceof Date) {
    date = dateInput;
  } else {
    return '';
  }

  if (isNaN(date.getTime())) {
    return '';
  }

  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();

  return `${day}-${month}-${year}`;
}

/**
 * Parse a dd-mm-yyyy format string to ISO format (YYYY-MM-DD)
 * Useful for converting user input to HTML date input value
 * @param {string} dateString - Date string in dd-mm-yyyy format
 * @returns {string} ISO format string (YYYY-MM-DD) or empty string if invalid
 */
export function parseToISOFormat(dateString) {
  if (!dateString) {
    return '';
  }

  if (!DATE_FORMAT_PATTERN.test(dateString)) {
    return '';
  }

  const parts = dateString.split('-');
  const day = parts[0];
  const month = parts[1];
  const year = parts[2];

  // Validate day and month ranges
  const dayNum = Number(day);
  const monthNum = Number(month);

  if (dayNum < 1 || dayNum > 31 || monthNum < 1 || monthNum > 12) {
    return '';
  }

  return `${year}-${month}-${day}`;
}

/**
 * Parse a dd-mm-yyyy format string to Date object
 * @param {string} dateString - Date string in dd-mm-yyyy format
 * @returns {Date|null} Date object or null if invalid
 */
export function parseToDate(dateString) {
  const isoFormat = parseToISOFormat(dateString);
  if (!isoFormat) {
    return null;
  }

  const date = new Date(isoFormat + 'T00:00:00Z');
  return isNaN(date.getTime()) ? null : date;
}

/**
 * Validate if a string is in dd-mm-yyyy format
 * @param {string} dateString - String to validate
 * @returns {boolean} True if valid dd-mm-yyyy format
 */
export function isValidDateFormat(dateString) {
  if (!dateString || typeof dateString !== 'string') {
    return false;
  }

  if (!DATE_FORMAT_PATTERN.test(dateString)) {
    return false;
  }

  const parts = dateString.split('-');
  const day = Number(parts[0]);
  const month = Number(parts[1]);
  const year = Number(parts[2]);

  if (month < 1 || month > 12 || day < 1 || day > 31) {
    return false;
  }

  // Check for valid date (handles leap years, etc.)
  const date = new Date(year, month - 1, day);
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
}

/**
 * Format a datetime (ISO string or Date) to dd-mm-yyyy HH:mm
 * @param {string|Date|null} dateTimeInput - DateTime to format
 * @returns {string} Formatted datetime as dd-mm-yyyy HH:mm or empty string
 */
export function formatDateTime(dateTimeInput) {
  if (!dateTimeInput) {
    return '';
  }

  let date;
  if (typeof dateTimeInput === 'string') {
    date = new Date(dateTimeInput);
  } else if (dateTimeInput instanceof Date) {
    date = dateTimeInput;
  } else {
    return '';
  }

  if (isNaN(date.getTime())) {
    return '';
  }

  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${day}-${month}-${year} ${hours}:${minutes}`;
}

/**
 * Get placeholder text for date inputs
 * @returns {string} Placeholder text (dd-mm-yyyy)
 */
export function getDatePlaceholder() {
  return DATE_FORMAT;
}

/**
 * Get placeholder text for datetime inputs
 * @returns {string} Placeholder text (dd-mm-yyyy HH:mm)
 */
export function getDateTimePlaceholder() {
  return 'dd-mm-yyyy HH:mm';
}

export default {
  formatDate,
  parseToISOFormat,
  parseToDate,
  isValidDateFormat,
  formatDateTime,
  getDatePlaceholder,
  getDateTimePlaceholder
};

