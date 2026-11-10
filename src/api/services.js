import { api } from "./http";
// Các API cũ và bổ sung US-03/04/12/13/14 theo API Contract Sprint 1 v5.
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
export const movementApi = {
  list: (id, opts) => api.get(`/residents/${id}/movements`, undefined, opts),
  create: (id, data) => api.post(`/residents/${id}/movements`, data),
};
export const searchApi = {
  search: (params, opts) => api.get("/search", params, opts),
};
export const feeApi = {
  list: (params, opts) => api.get("/fee-categories", params, opts),
  get: (id) => api.get(`/fee-categories/${id}`),
  create: (data) => api.post("/fee-categories", data),
  update: (id, data) => api.put(`/fee-categories/${id}`, data),
  updateStatus: (id, active) =>
    api.patch(`/fee-categories/${id}/status`, { active }),
  remove: (id) => api.delete(`/fee-categories/${id}`),
  preview: (data) => api.post("/fee-calculation/preview", data),
};
export const periodApi = {
  list: (params, opts) => api.get("/billing-periods", params, opts),
  get: (id) => api.get(`/billing-periods/${id}`),
  create: (data) => api.post("/billing-periods", data),
  update: (id, data) => api.put(`/billing-periods/${id}`, data),
  remove: (id) => api.delete(`/billing-periods/${id}`),
  generate: (id, dryRun = false) =>
    api.post(`/billing-periods/${id}/generate${dryRun ? "?dryRun=true" : ""}`),
  receivables: (id, params, opts) =>
    api.get(`/billing-periods/${id}/receivables`, params, opts),
  updateStatus: (id, status) =>
    api.patch(`/billing-periods/${id}/status`, { status }),
};
export const userApi = {
  roles: () => api.get("/roles"),
  list: (params, opts) => api.get("/users", params, opts),
  get: (id) => api.get(`/users/${id}`),
  create: (data) => api.post("/users", data),
  updateRoles: (id, roles) => api.put(`/users/${id}/roles`, { roles }),
  updateStatus: (id, active) => api.patch(`/users/${id}/status`, { active }),
};
