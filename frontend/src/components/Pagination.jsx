/**
 * Compact page navigator with Previous/Next and a windowed set of page
 * buttons, so long result sets stay usable.
 */
export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;

  const current = page; // 1-based
  const windowSize = 5;

  let start = Math.max(1, current - Math.floor(windowSize / 2));
  const end = Math.min(totalPages, start + windowSize - 1);
  start = Math.max(1, end - windowSize + 1);

  const pages = [];
  for (let p = start; p <= end; p += 1) {
    pages.push(p);
  }

  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" onClick={() => onChange(current - 1)} disabled={current <= 1}>
        Prev
      </button>

      {start > 1 && (
        <>
          <button type="button" onClick={() => onChange(1)}>
            1
          </button>
          {start > 2 && <span className="muted">…</span>}
        </>
      )}

      {pages.map((p) => (
        <button
          key={p}
          type="button"
          className={p === current ? 'active' : ''}
          aria-current={p === current ? 'page' : undefined}
          onClick={() => onChange(p)}
        >
          {p}
        </button>
      ))}

      {end < totalPages && (
        <>
          {end < totalPages - 1 && <span className="muted">…</span>}
          <button type="button" onClick={() => onChange(totalPages)}>
            {totalPages}
          </button>
        </>
      )}

      <button type="button" onClick={() => onChange(current + 1)} disabled={current >= totalPages}>
        Next
      </button>
    </nav>
  );
}
