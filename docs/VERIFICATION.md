# Kết quả kiểm chứng — 02/11/2026

Môi trường: Windows, Node 24.22.1, npm 11.19.1, React 18.3.1, Vite 6.4.5, React Router 7.18.5, Vitest 4.1.12, Chromium headless qua Playwright 1.64.1.

| Lệnh / kiểm tra                                     | Kết quả thực tế                                                                    |
| --------------------------------------------------- | ---------------------------------------------------------------------------------- |
| `npm ci`                                            | PASS; cài tái lập từ package-lock, 166 packages                                    |
| `npm run check`                                     | PASS: 17 Node unit tests + 60 Vitest HTTP/component/integration tests; 0 fail/skip |
| `npm run build`                                     | PASS: 1608 modules; JS 267.64 kB (gzip 82.46 kB), CSS 18.39 kB (gzip 5.17 kB)      |
| `npm run test:browser`                              | PASS: 4/4 Chromium tests, desktop 1440×1000 và mobile 390×844                      |
| `npm audit` khi cài sạch                            | 0 vulnerabilities; kiểm tra theo dữ liệu registry tại thời điểm chạy               |
| `npm run format:check`                              | PASS sau khi định dạng source/test/tài liệu                                        |
| `git diff --check`                                  | PASS; không có lỗi whitespace hoặc conflict marker                                 |
| Backend thật / DB / audit / BCrypt / JWT revocation | BLOCKED: repository mới có khung Spring Boot; chưa có API/tài khoản/dữ liệu test   |
| GitHub Actions                                      | NOT TESTED trên GitHub: workflow đã chuẩn bị, chưa push                            |

**81 test PASS không phải 81 E2E với backend.** Node tests kiểm tra validation và quyền; Vitest dùng fetch mock ở `tests/helpers.jsx` và `tests/http.test.js`. Browser tests gọi bản production qua Vite preview nhưng chặn HTTP `/api/v1` bằng Playwright. Không có mock trong `src/`.

Bằng chứng chạy mới nhất:

```text
Node:    tests 17, pass 17, fail 0
Vitest:  Test Files 3 passed (3), Tests 60 passed (60)
Vite:    1608 modules transformed; built in 5.12s
Browser: 4 passed (8.0s)
```

Các nhóm test:

- `validation.test.mjs`, `permissions.test.mjs`: giới hạn diện tích/tầng, ngày không tồn tại/tương lai, field bắt buộc, vai trò, liên kết cư dân, mật khẩu và tự khóa BQT.
- `http.test.js`: envelope, Authorization, page 1/active false, 204, các HTTP status, lỗi lồng nhau, 401 login khác phiên hết hạn, HTML 401, network, timeout và hủy request.
- `forms.test.jsx`: tạo/sửa hộ và nhân khẩu, whitelist payload, 4 entryType, duplicate field, inactive race, tạo tài khoản trùng/thiếu cư dân, double submit, modal khóa khi lưu và focus bàn phím.
- `flows.test.jsx`: login `/me`, A khóa/B đăng nhập, vai trò, logout/expiry/revocation UI, giao dịch/lịch sử/tài khoản chặn xóa, count sau thêm/xóa, tạo tài khoản nhiều vai trò, phân quyền/khóa, tìm/lọc/phân trang, hồ sơ/đổi mật khẩu.
- `browser/smoke.spec.js`: bản production không có lỗi JavaScript, không tràn ngang ở mobile, modal giữ focus và khôi phục màn hình, logout và bảo vệ deep link.

Ảnh kiểm tra desktop/mobile được sinh tại `test-results/*/household-form.png` (không commit). Đã xem trực tiếp: form một cột trên điện thoại, body cuộn độc lập và footer lưu/hủy luôn hiện; desktop dùng form hai cột.

Lượt browser đầu thất bại do selector exact không tính dấu `*` của nhãn bắt buộc; đã sửa selector và chạy lại cả 4 test đạt. Lượt build sandbox đầu bị Windows từ chối đọc đường dẫn cha của esbuild; build bằng quyền cần thiết đã thành công, không thay logic ứng dụng để né lỗi. npm có cảnh báo deprecated `whatwg-encoding` và thông tin chính sách install-scripts của esbuild; cài/build/test đã hoàn tất.

Xem [AC_COVERAGE.md](AC_COVERAGE.md) để phân biệt phần frontend PASS với các điều kiện nghiệm thu cần backend.
