import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatusBadge from './StatusBadge';
import PriorityBadge from './PriorityBadge';

describe('StatusBadge', () => {
  it('renders a friendly label for the status', () => {
    render(<StatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText('In progress')).toBeInTheDocument();
  });

  it('applies a status-specific class', () => {
    render(<StatusBadge status="COMPLETED" />);
    expect(screen.getByText('Completed')).toHaveClass('badge-completed');
  });
});

describe('PriorityBadge', () => {
  it('renders a friendly priority label', () => {
    render(<PriorityBadge priority="HIGH" />);
    expect(screen.getByText('High priority')).toBeInTheDocument();
  });
});
