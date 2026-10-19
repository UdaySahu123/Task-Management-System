import { Menu } from 'lucide-react';

/** Sticky page header with a title and (on small screens) a menu button. */
export default function Topbar({ title, onMenuClick, children }) {
  return (
    <header className="topbar">
      <button
        type="button"
        className="btn-icon"
        aria-label="Toggle navigation"
        onClick={onMenuClick}
      >
        <Menu size={20} />
      </button>
      <h1 className="grow truncate">{title}</h1>
      {children}
    </header>
  );
}
