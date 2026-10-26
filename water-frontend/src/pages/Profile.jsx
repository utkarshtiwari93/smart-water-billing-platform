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
      setNameMsg('Name updated');
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
      setPwMsg('Password changed');
    } catch (err) {
      setPwErr(err);
    }
  };

  return (
    <>
      <PageHeader title="My profile" subtitle="Your account details" />
      <div className="grid-2">
        <Card title="Account">
          <dl className="kv">
            <dt>Email</dt><dd>{user.email}</dd>
            <dt>Role</dt><dd>{user.role}</dd>
            <dt>Apartment ID</dt><dd>{user.apartmentId}</dd>
            <dt>Household ID</dt><dd>{user.householdId ?? 'Not assigned yet'}</dd>
          </dl>
          <form className="form" onSubmit={saveName}>
            <label className="field"><span className="field-label">Display name</span>
              <input required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} /></label>
            <Alert type="success">{nameMsg}</Alert>
            <ErrorBox error={nameErr} />
            <div><button className="btn btn-primary">Save name</button></div>
          </form>
        </Card>

        <Card title="Change password">
          <form className="form" onSubmit={savePassword}>
            <label className="field"><span className="field-label">Current password</span>
              <input type="password" required value={pw.currentPassword}
                onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} /></label>
            <label className="field"><span className="field-label">New password</span>
              <input type="password" required minLength={8} maxLength={72} value={pw.newPassword}
                onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} />
              <span className="field-hint">At least 8 characters. Accounts that only use Google sign-in have no password.</span></label>
            <Alert type="success">{pwMsg}</Alert>
            <ErrorBox error={pwErr} />
            <div><button className="btn btn-primary">Change password</button></div>
          </form>
        </Card>
      </div>
    </>
  );
}
