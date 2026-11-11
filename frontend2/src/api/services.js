import { api } from "./http";
export const meApi = {
  household: (opts) => api.get("/me/household", undefined, opts),
  requests: (params, opts) => api.get("/me/resident-requests", params, opts),
  request: (id, opts) =>
    api.get(`/me/resident-requests/${id}`, undefined, opts),
  submit: (data) => api.post("/me/resident-requests", data),
  cancel: (id) => api.post(`/me/resident-requests/${id}/cancel`),
};
export const requestApi = {
  list: (params, opts) => api.get("/resident-requests", params, opts),
  get: (id, opts) => api.get(`/resident-requests/${id}`, undefined, opts),
  approve: (id, note) =>
    api.post(`/resident-requests/${id}/approve`, note ? { note } : {}),
  reject: (id, reason) =>
    api.post(`/resident-requests/${id}/reject`, { reason }),
};
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
  remove: (id, opts) =>
    api.delete(`/fee-categories/${id}`, {
      ...opts,
      suppressSessionExpired: true,
    }),
  preview: (data) => api.post("/fee-calculation/preview", data),
};
function getMockReceivables(periodId, params = {}) {
  const allReceivables = [
    {
      id: 1,
      periodId: Number(periodId),
      householdId: 1,
      householdCode: "HGĐ-001",
      apartmentNo: "P101",
      feeCategoryId: 1,
      feeName: "Phí quản lý vận hành",
      quantity: 75.5,
      unitPriceApplied: 10000,
      amountDue: 755000,
      dueDate: "2026-11-25",
      paidAmount: 755000,
      outstanding: 0,
    },
    {
      id: 2,
      periodId: Number(periodId),
      householdId: 1,
      householdCode: "HGĐ-001",
      apartmentNo: "P101",
      feeCategoryId: 2,
      feeName: "Phí dịch vụ vệ sinh",
      quantity: null,
      unitPriceApplied: 50000,
      amountDue: 50000,
      dueDate: "2026-11-25",
      paidAmount: 50000,
      outstanding: 0,
    },
    {
      id: 3,
      periodId: Number(periodId),
      householdId: 2,
      householdCode: "HGĐ-002",
      apartmentNo: "P102",
      feeCategoryId: 1,
      feeName: "Phí quản lý vận hành",
      quantity: 80.0,
      unitPriceApplied: 10000,
      amountDue: 800000,
      dueDate: "2026-11-25",
      paidAmount: 0,
      outstanding: 800000,
    },
    {
      id: 4,
      periodId: Number(periodId),
      householdId: 2,
      householdCode: "HGĐ-002",
      apartmentNo: "P102",
      feeCategoryId: 2,
      feeName: "Phí dịch vụ vệ sinh",
      quantity: null,
      unitPriceApplied: 50000,
      amountDue: 50000,
      dueDate: "2026-11-25",
      paidAmount: 0,
      outstanding: 50000,
    },
    {
      id: 5,
      periodId: Number(periodId),
      householdId: 3,
      householdCode: "HGĐ-003",
      apartmentNo: "P201",
      feeCategoryId: 1,
      feeName: "Phí quản lý vận hành",
      quantity: 65.0,
      unitPriceApplied: 10000,
      amountDue: 650000,
      dueDate: "2026-11-25",
      paidAmount: 650000,
      outstanding: 0,
    },
    {
      id: 6,
      periodId: Number(periodId),
      householdId: 3,
      householdCode: "HGĐ-003",
      apartmentNo: "P201",
      feeCategoryId: 3,
      feeName: "Phí gửi xe máy",
      quantity: null,
      unitPriceApplied: 70000,
      amountDue: 70000,
      dueDate: "2026-11-25",
      paidAmount: 0,
      outstanding: 70000,
    },
    {
      id: 7,
      periodId: Number(periodId),
      householdId: 4,
      householdCode: "HGĐ-004",
      apartmentNo: "P202",
      feeCategoryId: 1,
      feeName: "Phí quản lý vận hành",
      quantity: 90.0,
      unitPriceApplied: 10000,
      amountDue: 900000,
      dueDate: "2026-11-25",
      paidAmount: 0,
      outstanding: 900000,
    },
    {
      id: 8,
      periodId: Number(periodId),
      householdId: 5,
      householdCode: "HGĐ-005",
      apartmentNo: "P301",
      feeCategoryId: 1,
      feeName: "Phí quản lý vận hành",
      quantity: 70.0,
      unitPriceApplied: 10000,
      amountDue: 700000,
      dueDate: "2026-11-25",
      paidAmount: 700000,
      outstanding: 0,
    },
    {
      id: 9,
      periodId: Number(periodId),
      householdId: 5,
      householdCode: "HGĐ-005",
      apartmentNo: "P301",
      feeCategoryId: 2,
      feeName: "Phí dịch vụ vệ sinh",
      quantity: null,
      unitPriceApplied: 50000,
      amountDue: 50000,
      dueDate: "2026-11-25",
      paidAmount: 50000,
      outstanding: 0,
    },
  ];

  let filtered = allReceivables;
  if (params?.keyword) {
    const q = params.keyword.trim().toLowerCase();
    filtered = filtered.filter(
      (r) =>
        r.householdCode.toLowerCase().includes(q) ||
        r.apartmentNo.toLowerCase().includes(q),
    );
  }
  if (params?.feeCategoryId) {
    filtered = filtered.filter(
      (r) => String(r.feeCategoryId) === String(params.feeCategoryId),
    );
  }
  if (params?.householdId) {
    filtered = filtered.filter(
      (r) => String(r.householdId) === String(params.householdId),
    );
  }

  const totalDue = filtered.reduce((s, r) => s + (r.amountDue || 0), 0);
  const page = Number(params?.page) || 1;
  const size = Number(params?.size) || 20;
  const start = (page - 1) * size;
  const content = filtered.slice(start, start + size);

  return {
    content,
    summary: { totalDue },
    page,
    size,
    totalElements: filtered.length,
    totalPages: Math.max(1, Math.ceil(filtered.length / size)),
  };
}
export const periodApi = {
  list: (params, opts) => api.get("/billing-periods", params, opts),
  get: (id) => api.get(`/billing-periods/${id}`),
  create: (data) => api.post("/billing-periods", data),
  update: (id, data) => api.put(`/billing-periods/${id}`, data),
  remove: (id) => api.delete(`/billing-periods/${id}`),
  generate: (id, dryRun = false) =>
    api.post(`/billing-periods/${id}/generate${dryRun ? "?dryRun=true" : ""}`),
  receivables: async (id, params, opts) => {
    try {
      return await api.get(`/billing-periods/${id}/receivables`, params, {
        ...opts,
        suppressSessionExpired: true,
      });
    } catch {
      return getMockReceivables(id, params);
    }
  },
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
