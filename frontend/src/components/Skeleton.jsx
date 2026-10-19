/** Grey placeholder blocks shown while data loads. */

export function SkeletonLine({ width = '100%' }) {
  return <div className="skeleton skeleton-line" style={{ width }} />;
}

export function SkeletonCard() {
  return (
    <div className="skeleton skeleton-card" aria-hidden="true" />
  );
}

/** A grid of placeholder cards, sized to match the task grid. */
export function TaskSkeletonGrid({ count = 6 }) {
  return (
    <div className="task-grid" aria-hidden="true">
      {Array.from({ length: count }).map((_, index) => (
        // eslint-disable-next-line react/no-array-index-key
        <SkeletonCard key={index} />
      ))}
    </div>
  );
}

export function StatSkeletonRow({ count = 4 }) {
  return (
    <div className="task-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }} aria-hidden="true">
      {Array.from({ length: count }).map((_, index) => (
        // eslint-disable-next-line react/no-array-index-key
        <div key={index} className="skeleton" style={{ height: 74 }} />
      ))}
    </div>
  );
}
