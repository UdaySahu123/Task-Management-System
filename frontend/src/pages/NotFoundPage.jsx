import { Link } from 'react-router-dom';
import { Compass } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function NotFoundPage() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="auth-page">
      <div className="auth-card text-center">
        <div className="empty-icon" style={{ margin: '0 auto 16px' }}>
          <Compass size={26} />
        </div>
        <h1 className="auth-title">404</h1>
        <p className="auth-subtitle">We couldn't find that page.</p>
        <Link className="btn btn-primary" to={isAuthenticated ? '/dashboard' : '/login'}>
          {isAuthenticated ? 'Back to dashboard' : 'Go to sign in'}
        </Link>
      </div>
    </div>
  );
}
