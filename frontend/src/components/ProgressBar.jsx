/**
 * Horizontal progress bar with an optional caption row.
 */
export default function ProgressBar({ value = 0, label, showPercent = true }) {
  const percent = Math.min(100, Math.max(0, Math.round(value)));

  return (
    <div>
      {(label || showPercent) && (
        <div className="progress-meta">
          <span className="muted">{label}</span>
          {showPercent && <strong>{percent}%</strong>}
        </div>
      )}
      <div
        className="progress-track"
        role="progressbar"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div className="progress-fill" style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}
