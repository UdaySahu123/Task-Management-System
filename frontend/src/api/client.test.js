import { describe, expect, it } from 'vitest';
import { getErrorMessage, getFieldErrors } from './client';

describe('getErrorMessage', () => {
  it('prefers a backend message', () => {
    const error = { response: { data: { message: 'Invalid credentials' } } };
    expect(getErrorMessage(error)).toBe('Invalid credentials');
  });

  it('joins field errors when present', () => {
    const error = {
      response: {
        data: {
          message: 'Validation failed',
          fieldErrors: [
            { field: 'title', message: 'Title is required' },
            { field: 'dueDate', message: 'Due date is required' },
          ],
        },
      },
    };
    expect(getErrorMessage(error)).toBe('Title is required, Due date is required');
  });

  it('explains network failures', () => {
    expect(getErrorMessage({ message: 'Network Error' })).toMatch(/Cannot reach the server/);
  });

  it('falls back to a generic message', () => {
    expect(getErrorMessage({})).toBe('Something went wrong. Please try again.');
  });
});

describe('getFieldErrors', () => {
  it('maps field errors into an object keyed by field', () => {
    const error = {
      response: { data: { fieldErrors: [{ field: 'email', message: 'Invalid email' }] } },
    };
    expect(getFieldErrors(error)).toEqual({ email: 'Invalid email' });
  });

  it('returns an empty object when there are no field errors', () => {
    expect(getFieldErrors(new Error('nope'))).toEqual({});
  });
});
