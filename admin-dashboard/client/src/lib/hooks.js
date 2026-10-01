import { useCallback, useEffect, useRef, useState } from 'react';
import { api, qs } from './api.js';

/** Fetches a resource and exposes { data, loading, error, reload }. */
export function useApi(path, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const load = useCallback(async () => {
    if (!path) return;
    setState((s) => ({ ...s, loading: true }));
    try {
      setState({ data: await api(path), loading: false, error: null });
    } catch (e) {
      setState({ data: null, loading: false, error: e.message });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [path, ...deps]);
  useEffect(() => { load(); }, [load]);
  return { ...state, reload: load };
}

/** Server-paginated list with search & filters (talks to crudRouter endpoints). */
export function useList(resource, initialFilters = {}, { limit = 15, sort } = {}) {
  const [filters, setFilters] = useState(initialFilters);
  const [page, setPage] = useState(1);
  const [q, setQ] = useState('');
  const [debouncedQ, setDebouncedQ] = useState('');
  const timer = useRef();

  useEffect(() => {
    clearTimeout(timer.current);
    timer.current = setTimeout(() => { setDebouncedQ(q); setPage(1); }, 300);
    return () => clearTimeout(timer.current);
  }, [q]);

  const path = `/${resource}${qs({ ...filters, q: debouncedQ, page, limit, sort })}`;
  const res = useApi(path);
  const setFilter = (k, v) => { setFilters((f) => ({ ...f, [k]: v })); setPage(1); };
  return { ...res, filters, setFilter, page, setPage, q, setQ };
}
