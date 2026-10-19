import { describe, expect, it } from 'vitest';
import { describeDueDate, formatDate, initials, statusLabel, priorityLabel } from './format';

describe('formatDate', () => {
  it('formats an ISO date', () => {
    expect(formatDate('2026-10-19')).toMatch(/19 Oct 2026/);
  });

  it('returns an em dash for empty values', () => {
    expect(formatDate(null)).toBe('—');
    expect(formatDate('')).toBe('—');
  });
});

describe('describeDueDate', () => {
  it('labels a past date as overdue', () => {
    const past = new Date();
    past.setDate(past.getDate() - 3);
    const result = describeDueDate(past.toISOString().slice(0, 10));
    expect(result.overdue).toBe(true);
    expect(result.text).toMatch(/Overdue by 3 days/);
  });

  it('labels today correctly', () => {
    const result = describeDueDate(new Date().toISOString().slice(0, 10));
    expect(result.text).toBe('Due today');
    expect(result.overdue).toBe(false);
  });

  it('labels a future date', () => {
    const future = new Date();
    future.setDate(future.getDate() + 5);
    const result = describeDueDate(future.toISOString().slice(0, 10));
    expect(result.text).toBe('Due in 5 days');
    expect(result.overdue).toBe(false);
  });
});

describe('initials', () => {
  it('builds initials from a full name', () => {
    expect(initials('Jane Doe')).toBe('JD');
  });

  it('uses a single letter for one-word names', () => {
    expect(initials('Madonna')).toBe('M');
  });

  it('falls back to a question mark', () => {
    expect(initials('')).toBe('?');
  });
});

describe('enum labels', () => {
  it('maps status values to friendly labels', () => {
    expect(statusLabel('IN_PROGRESS')).toBe('In progress');
  });

  it('maps priority values to friendly labels', () => {
    expect(priorityLabel('HIGH')).toBe('High');
  });
});
