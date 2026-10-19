import { NavLink } from 'react-router-dom';
import { LayoutDashboard, ListTodo, User, LogOut, CheckSquare } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { initials } from '../utils/format';

const links = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/tasks', label: 'Tasks', icon: ListTodo },
  { to: '/profile', label: 'Profile', icon: User },
];

export default function Sidebar({ open, onNavigate }) {
  const { user, logout } = useAuth();

  return (
    <aside className={`sidebar${open ? ' open' : ''}`}>
      <div className="sidebar-brand">
        <span className="logo-mark">
          <CheckSquare size={18} />
        </span>
        TaskFlow
      </div>

      <nav className="sidebar-nav">
        {links.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
            onClick={onNavigate}
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-user">
          <span className="avatar">{initials(user?.name)}</span>
          <div className="grow truncate">
            <div className="name truncate">{user?.name}</div>
            <div className="email truncate">{user?.email}</div>
          </div>
        </div>
        <button type="button" className="nav-link" onClick={logout} style={{ width: '100%' }}>
          <LogOut size={18} />
          Sign out
        </button>
      </div>
    </aside>
  );
}
