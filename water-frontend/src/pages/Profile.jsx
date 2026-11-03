import { useState } from 'react';
import { useAuth } from '../auth.jsx';
import { put } from '../api.js';
import { Alert, Card, ErrorBox, PageHeader } from '../ui.jsx';

export default function Profile() {
  const { user, refresh } = useAuth();
  const [name, setName] = useState(user.name);
  const [nameMsg, setNameMsg] = useState('');
  const [nameErr, setNameErr] = useState(null);

  const [pw, setPw] = useState({ currentPassword: '', newPassword: '' });
  const [pwMsg, setPwMsg] = useState('');
  const [pwErr, setPwErr] = useState(null);

  const saveName = async (e) => {
    e.preventDefault();
    setNameMsg('');
    setNameErr(null);
    try {
      await put('/api/v1/users/me', { name: name.trim() });
      await refresh();
      setNameMsg('Display name updated successfully');
    } catch (err) {
      setNameErr(err);
    }
  };

  const savePassword = async (e) => {
    e.preventDefault();
    setPwMsg('');
    setPwErr(null);
    try {
      await put('/api/v1/users/me/password', pw);
      setPw({ currentPassword: '', newPassword: '' });
      setPwMsg('Password changed successfully');
    } catch (err) {
      setPwErr(err);
    }
  };

  return (
    <div className="profile-page">
      <PageHeader title="My profile" subtitle="Manage your personal information, role details, and security credentials" />
      <div className="grid-2">
        <Card title="Account details">
          <div className="profile-identity">
            <span className="profile-avatar">{(user.name || '?').charAt(0).toUpperCase()}</span>
            <span>
              <strong>{user.name}</strong>
              <small>{user.email}</small>
              <span className="profile-role" style={{ marginTop: 4, display: 'inline-block' }}>
                {user.role === 'ADMIN' ? 'Society Administrator' : 'Resident Account'}
              </span>
            </span>
          </div>
          <dl className="kv" style={{ padding: '10px 0', borderTop: '1px solid var(--line)', borderBottom: '1px solid var(--line)', marginBottom: 20 }}>
            <dt>Email address</dt><dd>{user.email}</dd>
            <dt>Role type</dt><dd>{user.role}</dd>
            <dt>Apartment ID</dt><dd>#{user.apartmentId}</dd>
            <dt>Household / Flat</dt><dd>{user.householdId ? `#${user.householdId}` : 'Not assigned yet'}</dd>
          </dl>
          <form className="form" onSubmit={saveName}>
            <label className="field">
              <span className="field-label">Display name</span>
              <input required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} />
            </label>
            {nameMsg && <Alert type="success">{nameMsg}</Alert>}
            <ErrorBox error={nameErr} />
            <div><button className="btn btn-primary">Update name</button></div>
          </form>
        </Card>

        <Card title="Security & password">
          <form className="form" onSubmit={savePassword}>
            <label className="field">
              <span className="field-label">Current password</span>
              <input
                type="password"
                required
                value={pw.currentPassword}
                onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })}
                placeholder="Enter current password"
              />
            </label>
            <label className="field">
              <span className="field-label">New password</span>
              <input
                type="password"
                required
                minLength={8}
                maxLength={72}
                value={pw.newPassword}
                onChange={(e) => setPw({ ...pw, newPassword: e.target.value })}
                placeholder="Minimum 8 characters"
              />
              <span className="field-hint">Accounts created via Google Sign-In do not require a separate local password.</span>
            </label>
            {pwMsg && <Alert type="success">{pwMsg}</Alert>}
            <ErrorBox error={pwErr} />
            <div><button className="btn btn-primary">Update password</button></div>
          </form>
        </Card>
      </div>
    </div>
  );
}
