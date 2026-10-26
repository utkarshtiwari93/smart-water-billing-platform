import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { get } from './api.js';
import { useAuth } from './auth.jsx';
import { Spinner, ErrorBox, useAsync } from './ui.jsx';

const AdminContext = createContext(null);
export const useAdmin = () => useContext(AdminContext);

const KEY = 'wb_apartment';

// Admin works on one apartment at a time; the selector in the top bar changes it.
export function AdminProvider({ children }) {
  const { user } = useAuth();
  const apartments = useAsync(() => get('/api/v1/apartments'), []);
  const [selected, setSelected] = useState(() => Number(localStorage.getItem(KEY)) || user.apartmentId);

  const list = apartments.data || [];

  useEffect(() => {
    if (list.length && !list.some((a) => a.id === selected)) {
      const fallback = list.find((a) => a.id === user.apartmentId) || list[0];
      setSelected(fallback.id);
    }
  }, [list, selected, user.apartmentId]);

  const setApartmentId = (id) => {
    localStorage.setItem(KEY, String(id));
    setSelected(Number(id));
  };

  const value = useMemo(() => ({
    apartmentId: selected,
    apartment: list.find((a) => a.id === selected) || null,
    apartments: list,
    setApartmentId,
    reloadApartments: apartments.reload,
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }), [selected, list]);

  if (apartments.loading && !apartments.data) return <Spinner text="Loading apartments..." />;
  if (apartments.error) return <div className="content"><ErrorBox error={apartments.error} /></div>;
  return <AdminContext.Provider value={value}>{children}</AdminContext.Provider>;
}
