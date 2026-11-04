import React from "react";
import { render, fireEvent, screen } from "@testing-library/react";
import { vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../src/auth/AuthContext";
import App from "../src/App";
export const account = {
  id: 1,
  username: "manager01",
  fullName: "Nguyễn Quản Trị",
  roles: ["BQT"],
  isActive: true,
  email: null,
  phone: null,
  residentId: null,
};
export const house = {
  id: 1,
  householdCode: "H001",
  apartmentNo: "A101",
  area: 75.5,
  floor: 1,
  status: "ACTIVE",
  head: { id: 1, fullName: "Nguyễn An" },
  residentCount: 1,
  presentCount: 1,
};
export const resident = {
  id: 2,
  householdId: 1,
  householdCode: "H001",
  apartmentNo: "A101",
  fullName: "Nguyễn Bình",
  dateOfBirth: "2000-01-01",
  relationshipToHead: "Con",
  residencyStatus: "RESIDING",
  isHead: false,
};
export const roles = ["BQT", "ACCOUNTANT", "RESIDENT", "AUTHORITY"].map(
  (code) => ({
    code,
    name: {
      BQT: "Ban quản trị",
      ACCOUNTANT: "Kế toán",
      RESIDENT: "Cư dân",
      AUTHORITY: "Cơ quan chức năng",
    }[code],
  }),
);
export const ok = (data, status = 200) => ({
  status,
  ok: true,
  json: async () => ({ success: true, data }),
});
export const fail = (code, status = 422, errors = {}, details = {}) => ({
  status,
  ok: false,
  json: async () => ({ success: false, code, errors, details }),
});
export const page = (content) => ({
  content,
  page: 1,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
});
export function server(handler = () => undefined, user = account) {
  const fetcher = vi.fn(async (url, options) => {
    const path = new URL(url).pathname.replace("/api/v1", "");
    const body = options.body ? JSON.parse(options.body) : undefined;
    const custom = await handler(path, options.method, body, new URL(url));
    if (custom) return custom;
    if (path === "/me") return ok(user);
    if (path === "/roles") return ok(roles);
    if (path === "/households") return ok(page([house]));
    if (path === "/households/1") return ok(house);
    if (path === "/households/1/residents") return ok([resident]);
    if (path === "/residents") return ok(page([resident]));
    if (path === "/residents/2") return ok(resident);
    if (path === "/users") return ok(page([user]));
    if (path === "/users/1") return ok(user);
    throw Error("Unhandled test request: " + options.method + " " + path);
  });
  vi.stubGlobal("fetch", fetcher);
  return fetcher;
}
export function app(path = "/", authenticated = true) {
  if (authenticated)
    sessionStorage.setItem("bluemoon_access_token", "test-session");
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}
export function fill(label, value) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}
export function fillHouse() {
  fill(/Mã hộ khẩu/, "H002");
  fill(/Số căn hộ/, "A102");
  fill(/Diện tích/, "75.5");
  fill(/Họ và tên chủ hộ/, "Nguyễn An");
  fill(/Ngày sinh/, "1980-01-01");
}
export function fillResident() {
  fill(/Họ và tên/, "Nguyễn Bình");
  fill(/Ngày sinh/, "2000-01-01");
  fill(/Quan hệ với chủ hộ/, "Con");
}
export const writes = (fetcher) =>
  fetcher.mock.calls.filter(([, o]) => o.method !== "GET");
