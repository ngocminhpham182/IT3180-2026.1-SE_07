const BASE_URL = (import.meta.env?.VITE_API_BASE_URL || "/api/v1").replace(
  /\/$/,
  "",
);
const messages = {
  INVALID_CREDENTIALS: "Sai tài khoản hoặc mật khẩu",
  ACCOUNT_LOCKED: "Tài khoản đang bị khóa tạm thời. Vui lòng thử lại sau.",
  TOKEN_EXPIRED: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
  TOKEN_INVALID: "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại.",
  UNAUTHORIZED: "Vui lòng đăng nhập để tiếp tục.",
  FORBIDDEN: "Bạn không có quyền thực hiện thao tác này.",
  NOT_FOUND: "Không tìm thấy dữ liệu được yêu cầu.",
  DUPLICATE: "Dữ liệu bị trùng. Vui lòng kiểm tra các trường được đánh dấu.",
  HAS_TRANSACTIONS: "Hộ đã có giao dịch. Hãy chuyển sang ngừng hoạt động.",
  HAS_HISTORY: "Nhân khẩu đã có lịch sử biến động nên không thể xóa.",
  HAS_ACCOUNT: "Nhân khẩu đang liên kết với tài khoản nên không thể xóa.",
  INVALID_NEW_HEAD: "Chủ hộ được chọn không hợp lệ.",
  HOUSEHOLD_INACTIVE: "Hộ không còn hoạt động.",
  SELF_LOCKOUT: "Bạn không thể tự khóa hoặc tự gỡ quyền Ban quản trị.",
  RESIDENT_LINK_REQUIRED:
    "Tài khoản cư dân phải liên kết với nhân khẩu đang cư trú.",
  VALIDATION_ERROR: "Vui lòng kiểm tra thông tin đã nhập.",
  NETWORK_ERROR:
    "Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối và thử lại.",
  TIMEOUT:
    "Máy chủ phản hồi quá lâu. Hãy kiểm tra dữ liệu trước khi gửi lại thao tác.",
  INVALID_RESPONSE: "Phản hồi máy chủ không đúng định dạng. Vui lòng thử lại.",
};
const codes = {
  400: "VALIDATION_ERROR",
  401: "UNAUTHORIZED",
  403: "FORBIDDEN",
  404: "NOT_FOUND",
  409: "DUPLICATE",
  422: "VALIDATION_ERROR",
  423: "ACCOUNT_LOCKED",
};
export class ApiError extends Error {
  constructor(payload = {}, status = 0) {
    const code =
      payload?.code ||
      codes[status] ||
      (status === 0 ? "NETWORK_ERROR" : "SERVER_ERROR");
    super(
      code === "INVALID_CREDENTIALS"
        ? messages[code]
        : typeof payload?.message === "string" && payload.message.trim()
          ? payload.message
          : messages[code] || "Đã xảy ra lỗi máy chủ. Vui lòng thử lại.",
    );
    this.name = "ApiError";
    this.code = code;
    this.status = status;
    this.fields = Object.fromEntries(
      Object.entries(payload?.errors || {}).map(([key, value]) => [
        key,
        typeof value === "string"
          ? value
          : Array.isArray(value)
            ? value.filter((v) => typeof v === "string").join(" ")
            : "Thông tin không hợp lệ.",
      ]),
    );
    this.details = payload?.details || {};
  }
}
export async function request(
  method,
  path,
  { body, params, signal, skipAuth = false, timeoutMs = 15000 } = {},
) {
  const url = new URL(BASE_URL + path, window.location.origin);
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value) !== "")
      url.searchParams.set(key, String(value));
  });
  const token = sessionStorage.getItem("bluemoon_access_token");
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token && !skipAuth) headers.Authorization = `Bearer ${token}`;
  const controller = new AbortController();
  let timedOut = false;
  const cancel = () => controller.abort();
  signal?.addEventListener("abort", cancel, { once: true });
  if (signal?.aborted) cancel();
  const timer = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, timeoutMs);
  try {
    const response = await fetch(url.toString(), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    });
    let payload = null;
    if (response.status !== 204) {
      try {
        payload = await response.json();
      } catch (error) {
        if (controller.signal.aborted) throw error;
        if (response.ok)
          throw new ApiError({ code: "INVALID_RESPONSE" }, response.status);
      }
    }
    if (!response.ok || payload?.success === false) {
      const error = new ApiError(payload || {}, response.status);
      // A late response from an older session must not invalidate a new login.
      if (
        response.status === 401 &&
        !skipAuth &&
        token === sessionStorage.getItem("bluemoon_access_token")
      ) {
        window.dispatchEvent(
          new CustomEvent("bluemoon:session-expired", {
            detail: messages[error.code] || messages.UNAUTHORIZED,
          }),
        );
      }
      throw error;
    }
    if (response.status === 204) return null;
    if (
      payload?.success !== true ||
      !Object.prototype.hasOwnProperty.call(payload || {}, "data")
    )
      throw new ApiError({ code: "INVALID_RESPONSE" }, response.status);
    return payload.data;
  } catch (error) {
    if (timedOut) throw new ApiError({ code: "TIMEOUT" });
    if (signal?.aborted || error?.name === "AbortError") throw error;
    if (error instanceof ApiError) throw error;
    throw new ApiError();
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener("abort", cancel);
  }
}
export const api = {
  get: (path, params, opts) => request("GET", path, { params, ...opts }),
  post: (path, body, opts) => request("POST", path, { body, ...opts }),
  put: (path, body, opts) => request("PUT", path, { body, ...opts }),
  patch: (path, body, opts) => request("PATCH", path, { body, ...opts }),
  delete: (path, opts) => request("DELETE", path, opts),
};
