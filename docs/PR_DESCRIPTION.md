# feat: complete BlueMoon Sprint 1 frontend

Base: `frontend`

Head: `feature/fe-us01-us02-us08-us09-us10`

## Mục đích

Nhánh frontend đang thiếu source React, scripts và tests dù đã có cấu hình Vite. PR tích hợp baseline ZIP và hoàn thiện frontend cho US-01/02/08/09/10, sử dụng API Contract v3.

## Thay đổi

- Tạo source hộ/nhân khẩu, đăng nhập, tài khoản/phân quyền và hồ sơ cá nhân cùng layout, form, router, API client.
- Chờ `/me` xác minh trước khi vào nghiệp vụ; khóa A không chặn B; xử lý JWT hết hạn/thu hồi, timeout, 204, lỗi field lồng nhau và lỗi nghiệp vụ.
- Modal không đóng khi lưu; chống double submit, hỗ trợ bàn phím. Payload chỉ gồm field được phép, ngày theo entryType, ngăn BQT tự khóa/bỏ quyền.
- Cập nhật dependency đã vá, lockfile, CI và hướng dẫn nối Spring Boot; giữ React 18/Vite 6, cập nhật Router 7 có kiểm thử.
- Không thay backend, SQL hoặc triển khai US-03/04. ZIP đã bị xóa khỏi nhánh từ trước, không sửa lịch sử.

Danh sách endpoint và blocker: [INTEGRATION.md](INTEGRATION.md). Danh sách file cuối cùng: `git diff --name-only origin/frontend...HEAD`.

## Kiểm thử

- `npm ci`: PASS.
- `npm run check`: 77/77 PASS (17 unit + 60 HTTP/component/integration).
- `npm run build`: PASS.
- `npm run test:browser`: 4/4 PASS desktop/mobile, **API mock**.
- `npm run format:check`, `git diff --check`: PASS.
- npm audit: 0 vulnerabilities khi cài sạch.
- 22 AC có báo cáo: 18 phần frontend PASS bằng mock, 4 phần chủ yếu thuộc backend BLOCKED; toàn bộ kiểm chứng tích hợp backend thật còn BLOCKED. Chưa kiểm chứng BCrypt, DB, audit log và thu hồi JWT thật.

## Checklist

- [x] Đúng 5 US được phân công.
- [x] Build và tests local thành công.
- [x] Không commit secret, ZIP, tài liệu đầu vào, node_modules hoặc dist.
- [x] Không ghi đè code thành viên khác; base đã xác nhận.
- [x] Phân quyền UI và API contract được kiểm thử.
- [x] Bằng chứng và blocker ghi rõ; sẵn sàng review frontend.
- [ ] Backend integration/UAT với tài khoản thật.
- [ ] CI trên GitHub (chưa push).
- [ ] Nhóm chỉ định reviewer và reviewer duyệt.

Chỉ push khi người dùng xác nhận `PUSH`. Không tự merge PR hoặc tạo release/tag.
