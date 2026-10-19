import { useState } from 'react';
import { KeyRound, Save, UserCog } from 'lucide-react';
import { changePassword, updateProfile } from '../api/users';
import { getErrorMessage, getFieldErrors } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatDate, initials } from '../utils/format';

const PASSWORD_RULE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;

export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const { toast } = useToast();

  const [profile, setProfile] = useState({ name: user?.name || '', email: user?.email || '' });
  const [profileErrors, setProfileErrors] = useState({});
  const [savingProfile, setSavingProfile] = useState(false);

  const [passwords, setPasswords] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [passwordErrors, setPasswordErrors] = useState({});
  const [savingPassword, setSavingPassword] = useState(false);

  const onProfileChange = (field) => (event) => {
    setProfile((current) => ({ ...current, [field]: event.target.value }));
    setProfileErrors((current) => ({ ...current, [field]: undefined }));
  };

  const onPasswordChange = (field) => (event) => {
    setPasswords((current) => ({ ...current, [field]: event.target.value }));
    setPasswordErrors((current) => ({ ...current, [field]: undefined }));
  };

  const handleProfileSubmit = async (event) => {
    event.preventDefault();
    const errors = {};
    const name = profile.name.trim();

    if (!name) {
      errors.name = 'Name is required';
    } else if (name.length < 2 || name.length > 100) {
      errors.name = 'Name must be between 2 and 100 characters';
    }

    if (!profile.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(profile.email.trim())) {
      errors.email = 'Please provide a valid email address';
    }

    setProfileErrors(errors);
    if (Object.keys(errors).length) return;

    setSavingProfile(true);
    try {
      const updated = await updateProfile({ name, email: profile.email.trim() });
      setUser(updated);
      toast.success('Profile updated');
    } catch (err) {
      setProfileErrors(getFieldErrors(err));
      toast.error(getErrorMessage(err));
    } finally {
      setSavingProfile(false);
    }
  };

  const handlePasswordSubmit = async (event) => {
    event.preventDefault();
    const errors = {};

    if (!passwords.currentPassword) {
      errors.currentPassword = 'Current password is required';
    }

    if (!passwords.newPassword) {
      errors.newPassword = 'New password is required';
    } else if (passwords.newPassword.length < 8 || passwords.newPassword.length > 72) {
      errors.newPassword = 'Password must be between 8 and 72 characters';
    } else if (!PASSWORD_RULE.test(passwords.newPassword)) {
      errors.newPassword = 'Must contain an uppercase letter, a lowercase letter and a digit';
    }

    if (!passwords.confirmPassword) {
      errors.confirmPassword = 'Please confirm your new password';
    } else if (passwords.newPassword !== passwords.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    setPasswordErrors(errors);
    if (Object.keys(errors).length) return;

    setSavingPassword(true);
    try {
      await changePassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
      });
      toast.success('Password updated');
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setPasswordErrors(getFieldErrors(err));
      toast.error(getErrorMessage(err));
    } finally {
      setSavingPassword(false);
    }
  };

  return (
    <div>
      <div className="mt-sm" style={{ marginBottom: 20 }}>
        <h2 className="page-title">Profile</h2>
        <p className="page-subtitle">Manage your account details and password.</p>
      </div>

      <section className="card" style={{ marginBottom: 18 }}>
        <div className="modal-body row gap">
          <span className="avatar" style={{ width: 52, height: 52, fontSize: 18 }}>
            {initials(user?.name)}
          </span>
          <div className="grow">
            <div style={{ fontWeight: 700 }}>{user?.name}</div>
            <div className="muted" style={{ fontSize: 13 }}>
              {user?.email}
            </div>
            <div className="muted" style={{ fontSize: 13 }}>
              Member since {formatDate(user?.createdAt)}
            </div>
          </div>
        </div>
      </section>

      <div className="detail-grid">
        <section className="card">
          <div className="modal-header">
            <h3 className="modal-title row gap-sm">
              <UserCog size={17} />
              Account details
            </h3>
          </div>
          <form className="modal-body" onSubmit={handleProfileSubmit} noValidate>
            <div className="form-group">
              <label className="form-label" htmlFor="profile-name">
                Full name
              </label>
              <input
                id="profile-name"
                className={`form-input${profileErrors.name ? ' is-invalid' : ''}`}
                value={profile.name}
                onChange={onProfileChange('name')}
              />
              {profileErrors.name && <p className="form-error">{profileErrors.name}</p>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="profile-email">
                Email
              </label>
              <input
                id="profile-email"
                type="email"
                className={`form-input${profileErrors.email ? ' is-invalid' : ''}`}
                value={profile.email}
                onChange={onProfileChange('email')}
              />
              {profileErrors.email && <p className="form-error">{profileErrors.email}</p>}
            </div>

            <button type="submit" className="btn btn-primary" disabled={savingProfile}>
              {savingProfile && <span className="spinner" />}
              <Save size={15} />
              Save changes
            </button>
          </form>
        </section>

        <section className="card">
          <div className="modal-header">
            <h3 className="modal-title row gap-sm">
              <KeyRound size={17} />
              Change password
            </h3>
          </div>
          <form className="modal-body" onSubmit={handlePasswordSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="currentPassword">
                Current password
              </label>
              <input
                id="currentPassword"
                type="password"
                autoComplete="current-password"
                className={`form-input${passwordErrors.currentPassword ? ' is-invalid' : ''}`}
                value={passwords.currentPassword}
                onChange={onPasswordChange('currentPassword')}
              />
              {passwordErrors.currentPassword && (
                <p className="form-error">{passwordErrors.currentPassword}</p>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="newPassword">
                New password
              </label>
              <input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                className={`form-input${passwordErrors.newPassword ? ' is-invalid' : ''}`}
                value={passwords.newPassword}
                onChange={onPasswordChange('newPassword')}
              />
              <p className="form-hint">
                At least 8 characters with an uppercase letter, a lowercase letter and a digit.
              </p>
              {passwordErrors.newPassword && (
                <p className="form-error">{passwordErrors.newPassword}</p>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="confirmNewPassword">
                Confirm new password
              </label>
              <input
                id="confirmNewPassword"
                type="password"
                autoComplete="new-password"
                className={`form-input${passwordErrors.confirmPassword ? ' is-invalid' : ''}`}
                value={passwords.confirmPassword}
                onChange={onPasswordChange('confirmPassword')}
              />
              {passwordErrors.confirmPassword && (
                <p className="form-error">{passwordErrors.confirmPassword}</p>
              )}
            </div>

            <button type="submit" className="btn btn-primary" disabled={savingPassword}>
              {savingPassword && <span className="spinner" />}
              <Save size={15} />
              Update password
            </button>
          </form>
        </section>
      </div>
    </div>
  );
}
