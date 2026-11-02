import { api } from "./http";
// Đường dẫn chính xác theo API Contract Sprint 1 (v3); KHÔNG có API được suy đoán.
export const authApi = {
  login: (data) => api.post("/auth/login", data, { skipAuth: true }),
  me: () => api.get("/me"),
  updateMe: (data) => api.put("/me", data),
  changePassword: (data) => api.patch("/me/password", data),
};
export const householdApi = {
  list: (params, opts) => api.get("/households", params, opts),
  get: (id) => api.get(`/households/${id}`),
  create: (data) => api.post("/households", data),
  update: (id, data) => api.put(`/households/${id}`, data),
  updateHead: (id, residentId) =>
    api.put(`/households/${id}/head`, { residentId }),
  updateStatus: (id, status) =>
    api.patch(`/households/${id}/status`, { status }),
  remove: (id) => api.delete(`/households/${id}`),
  residents: (id, status) =>
    api.get(`/households/${id}/residents`, status ? { status } : undefined),
  addResident: (id, data) => api.post(`/households/${id}/residents`, data),
};
export const residentApi = {
  list: (params, opts) => api.get("/residents", params, opts),
  get: (id) => api.get(`/residents/${id}`),
  update: (id, data) => api.put(`/residents/${id}`, data),
  remove: (id) => api.delete(`/residents/${id}`),
};
export const userApi = {
  roles: () => api.get("/roles"),
  list: (params, opts) => api.get("/users", params, opts),
  get: (id) => api.get(`/users/${id}`),
  create: (data) => api.post("/users", data),
  updateRoles: (id, roles) => api.put(`/users/${id}/roles`, { roles }),
  updateStatus: (id, active) => api.patch(`/users/${id}/status`, { active }),
};
