import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

const TITLES = {
  '/dashboard': 'Dashboard',
  '/tasks': 'Tasks',
  '/profile': 'Profile',
};

const resolveTitle = (pathname) => {
  if (pathname.startsWith('/tasks/')) return 'Task details';
  return TITLES[pathname] || 'TaskFlow';
};

/** Authenticated application chrome: sidebar + topbar + routed page. */
export default function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { pathname } = useLocation();

  return (
    <div className="app-shell">
      <Sidebar open={sidebarOpen} onNavigate={() => setSidebarOpen(false)} />

      <div
        className={`sidebar-overlay${sidebarOpen ? ' show' : ''}`}
        onClick={() => setSidebarOpen(false)}
        aria-hidden="true"
      />

      <div className="app-main">
        <Topbar title={resolveTitle(pathname)} onMenuClick={() => setSidebarOpen((value) => !value)} />
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
