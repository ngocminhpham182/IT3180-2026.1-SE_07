import { describe, expect, it, vi } from "vitest";
import { ApiError, request } from "../src/api/http";
const response = (data, status = 200) => ({
  status,
  ok: status < 400,
  json: async () => data,
});
it("unwraps data, sends Bearer, preserves page=1 and active=false", async () => {
  sessionStorage.setItem("bluemoon_access_token", "test-session");
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      response({ success: true, data: { content: [], page: 1 } }),
    );
  vi.stubGlobal("fetch", fetcher);
  expect(
    await request("GET", "/users", {
      params: { page: 1, active: false, keyword: "Nguyễn" },
    }),
  ).toEqual({ content: [], page: 1 });
  const [url, options] = fetcher.mock.calls[0];
  expect(new URL(url).searchParams.get("active")).toBe("false");
  expect(options.headers.Authorization).toBe("Bearer test-session");
});
it("handles 204 without parsing a body", async () => {
  const json = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({ status: 204, ok: true, json }),
  );
  expect(await request("DELETE", "/households/1")).toBeNull();
  expect(json).not.toHaveBeenCalled();
});
it("maps nested and array validation errors", () => {
  const e = new ApiError(
    {
      code: "DUPLICATE",
      errors: { "head.identityNumber": ["Đã tồn tại"], area: "Không hợp lệ" },
    },
    409,
  );
  expect(e.fields["head.identityNumber"]).toBe("Đã tồn tại");
});
it.each([400, 403, 404, 409, 422, 423, 500])(
  "returns useful errors for HTTP %i",
  async (status) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response({}, status)));
    await expect(request("GET", "/users")).rejects.toMatchObject({ status });
  },
);
it("does not expire the session on invalid login and hides enumeration details", async () => {
  const handler = vi.fn();
  window.addEventListener("bluemoon:session-expired", handler);
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        response({ code: "INVALID_CREDENTIALS", message: "user exists" }, 401),
      ),
  );
  await expect(
    request("POST", "/auth/login", { skipAuth: true }),
  ).rejects.toThrow("Sai tài khoản hoặc mật khẩu");
  expect(handler).not.toHaveBeenCalled();
  window.removeEventListener("bluemoon:session-expired", handler);
});
it.each(["TOKEN_EXPIRED", "TOKEN_INVALID", "UNAUTHORIZED"])(
  "expires protected sessions for %s",
  async (code) => {
    const handler = vi.fn();
    window.addEventListener("bluemoon:session-expired", handler);
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response({ code }, 401)));
    await expect(request("GET", "/me")).rejects.toMatchObject({ code });
    expect(handler).toHaveBeenCalledOnce();
    window.removeEventListener("bluemoon:session-expired", handler);
  },
);
it("handles an HTML 401 as an expired session", async () => {
  const handler = vi.fn();
  window.addEventListener("bluemoon:session-expired", handler);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({
      status: 401,
      ok: false,
      json: async () => {
        throw Error("HTML");
      },
    }),
  );
  await expect(request("GET", "/me")).rejects.toMatchObject({
    code: "UNAUTHORIZED",
  });
  expect(handler).toHaveBeenCalledOnce();
  window.removeEventListener("bluemoon:session-expired", handler);
});
it("handles network failures and malformed success envelopes", async () => {
  vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
  await expect(request("GET", "/me")).rejects.toMatchObject({
    code: "NETWORK_ERROR",
  });
  fetch.mockResolvedValue(response({ foo: "bar" }));
  await expect(request("GET", "/me")).rejects.toMatchObject({
    code: "INVALID_RESPONSE",
  });
});
it("times out requests without retrying mutations", async () => {
  const fetcher = vi.fn(
    (url, { signal }) =>
      new Promise((resolve, reject) =>
        signal.addEventListener("abort", () =>
          reject(new DOMException("Aborted", "AbortError")),
        ),
      ),
  );
  vi.stubGlobal("fetch", fetcher);
  await expect(
    request("POST", "/users", { body: {}, timeoutMs: 5 }),
  ).rejects.toMatchObject({ code: "TIMEOUT" });
  expect(fetcher).toHaveBeenCalledOnce();
});
it("preserves cancellation", async () => {
  const ctrl = new AbortController();
  vi.stubGlobal(
    "fetch",
    vi.fn(
      (url, { signal }) =>
        new Promise((resolve, reject) =>
          signal.addEventListener("abort", () =>
            reject(new DOMException("Aborted", "AbortError")),
          ),
        ),
    ),
  );
  const pending = request("GET", "/users", { signal: ctrl.signal });
  ctrl.abort();
  await expect(pending).rejects.toMatchObject({ name: "AbortError" });
});
