# Audit và tích hợp Sprint 1 — 02/11/2026

## Trạng thái nguồn đã xác minh

Đã đọc 5 US và đủ 22 AC từ Excel, API Contract v3, SQL tham chiếu, source/tests/config trong ZIP và quy tắc Git của PDF (trang 7, 9, 14, 28–35, 42). `.codex-input/` chưa tồn tại ở workspace ban đầu; dùng các đường dẫn tài liệu người dùng cung cấp. ZIP được giải nén vào thư mục tạm ngoài Git, so sánh từng file cấu hình trước khi tích hợp.

Sau `git fetch --prune origin`:

| Nhánh    | Commit    | Nội dung                                                                                               |
| -------- | --------- | ------------------------------------------------------------------------------------------------------ |
| master   | `9a852ec` | Merge PR #1 backend; workspace ban đầu sạch                                                            |
| backend  | `21edfce` | Khung Spring Boot: application, properties, context test; chưa có controller/entity/security nghiệp vụ |
| frontend | `11340ec` | package/Vite/index/env/README, chưa có src/scripts/tests                                               |

Cấu hình frontend và README khớp ZIP theo nội dung. Không có lockfile hoặc CI frontend trước đó. ZIP từng được thêm tại `7e6d4d0` và xóa tại `11340ec`; hiện không còn track ZIP, không cần commit xóa hay viết lại lịch sử.

Người dùng xác nhận feature branch tạo từ `origin/frontend`, PR vào `frontend`. Không merge frontend vào master, không sửa backend/SQL. Khi nhóm tích hợp frontend vào master sau này, cần review riêng: master hiện đã xóa các file cấu hình frontend cũ; có thể gặp conflict modify/delete. Các thư mục Java thuộc nhánh backend, không phải source bị xóa bởi PR này khi so với base frontend.

## Kiến trúc và thay đổi

- `api/http.js`: base URL, envelope data, 204, Bearer, timeout 15 giây, hủy request, lỗi field lồng nhau, các mã lỗi nghiệp vụ; 401 login không làm hết phiên. Không tự retry mutation sau timeout vì kết quả ghi phía server có thể chưa rõ.
- `api/services.js`: tập trung endpoint hợp đồng. Payload cập nhật chỉ gồm field được phép; không query PostgreSQL.
- `auth/AuthContext.jsx`, `permissions.js`, `App.jsx`: xác minh `/me`, hết hạn theo thời điểm token/login, phản hồi token bị thu hồi, route/menu theo vai trò, phiên lưu trong sessionStorage. Không tin thông tin người dùng lưu cũ để cấp quyền.
- Form và trang: hộ + chủ hộ trong một request, đổi chủ hộ riêng, thêm nhân khẩu theo entryType, giữ lỗi nghiệp vụ; tài khoản nhiều vai trò và liên kết cư dân; hồ sơ/đổi mật khẩu của chính mình. BQT không tự khóa/bỏ quyền BQT.
- Modal: chặn Escape/backdrop/nút đóng khi đang ghi; giữ vòng Tab trong dialog, khôi phục focus; tránh double submit. Thành công tải lại dữ liệu và số lượng, lỗi tải lại không bị giấu. Request chi tiết cũ không ghi đè dữ liệu của route mới.
- Giữ phong cách BlueMoon, bổ sung focus/reduced motion; bỏ câu chữ kỹ thuật không cần thiết khỏi luồng người dùng.
- React Router từ 6 lên 7.18.5 để khắc phục advisory của nhánh 6; API khai báo route được giữ và kiểm thử. React 18/Vite 6 giữ nguyên major; Vitest 4.1.12, lockfile, Node 22.12+.
- Bỏ kiểm tra chuỗi source của ZIP; thay bằng unit, HTTP, component/integration và browser test kiểm tra hành vi.

## Endpoint được sử dụng

| Nhóm      | Endpoint / method                                                                                 |
| --------- | ------------------------------------------------------------------------------------------------- |
| Auth      | POST `/auth/login`, GET `/me`                                                                     |
| Hồ sơ     | PUT `/me`, PATCH `/me/password`                                                                   |
| Hộ        | GET/POST `/households`; GET/PUT/DELETE `/households/{id}`; PUT `/{id}/head`; PATCH `/{id}/status` |
| Nhân khẩu | GET/POST `/households/{id}/residents`; GET `/residents`; GET/PUT/DELETE `/residents/{id}`         |
| Tài khoản | GET `/roles`; GET/POST `/users`; GET `/users/{id}`; PUT `/{id}/roles`; PATCH `/{id}/status`       |

Tất cả có prefix `/api/v1`. Logout xóa phiên tại client theo contract (endpoint logout tùy chọn). Danh sách page bắt đầu 1, size 20; thành viên một hộ là mảng không phân trang. Không gọi `/movements`, `/search` hoặc endpoint reset password tự đặt.

## Mismatch và backend blockers

| Vấn đề                             | Bằng chứng / xử lý                                                                                                                                                                                                   |
| ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Chưa có triển khai API nghiệp vụ   | Cây source origin/master/backend chỉ có lớp Application và context test. Không có listener localhost:8080 khi kiểm tra. Cần URL backend, tài khoản và dữ liệu test để chạy tích hợp thật.                            |
| `gender=OTHER`                     | Contract cho phép MALE/FEMALE/OTHER; CHECK SQL chỉ MALE/FEMALE. Frontend theo contract và hiện “Khác”; backend cần đồng bộ schema, không tự loại giá trị contract ở UI.                                              |
| `RETURN`                           | Contract movementType có RETURN, SQL không có. Chỉ báo cáo; US-03 ngoài phạm vi, không thêm màn hình/endpoint.                                                                                                       |
| Cú pháp SQL                        | Có `//` sau CREATE EXTENSION, CREATE TABLE household và entity_type. Không chạy hoặc sửa SQL trong công việc frontend.                                                                                               |
| Hộ có giao dịch                    | SQL tham chiếu chưa có bảng khoản thu/phiếu thu/đóng góp; không thể kiểm chứng HAS_TRANSACTIONS thật. Frontend xử lý đúng 409 và cho người dùng xác nhận ngừng hoạt động.                                            |
| Bảo mật/dữ liệu                    | BCrypt, audit_log, transaction, quyền mỗi request, khóa 15 phút, token cũ mất hiệu lực và không ảnh hưởng hộ khác cần backend/DB xác minh. Mock không chứng minh các bảo đảm này.                                    |
| Thêm RESIDENT cho tài khoản đã tạo | PUT roles không liên kết residentId. UI chặn nếu chưa có residentId; không tự tạo API liên kết.                                                                                                                      |
| entryType BIRTH                    | Contract mô tả entryDate là ngày vào hộ/ngày sinh nhưng mặc định hôm nay. Frontend gửi ngày sinh khi BIRTH; khi tạm trú chỉ gửi startDate/endDate, không gửi ngày ẩn. Nhóm backend cần xác nhận diễn giải trong UAT. |

## Kiểm thử thủ công với backend thật

1. Chạy migration đã được backend xác nhận, seed tài khoản từ cấu hình ngoài Git, khởi động API và đặt proxy target. Dùng dữ liệu thử riêng.
2. BQT tạo hộ + chủ hộ, thử trùng mã/căn hộ/CCCD; thêm người bằng 4 entryType, đổi chủ hộ, sửa hộ; xác minh DB và audit có người/thời điểm/thao tác.
3. Mở hộ có nhiều thành viên, kiểm tra residentCount/presentCount; thêm/xóa hộ A không ảnh hưởng B. Thử hộ inactive, nhân khẩu có lịch sử/tài khoản/chủ hộ, hộ có giao dịch.
4. Thử đúng/sai login, không tồn tại, vô hiệu hóa, sai 5 lần; A bị khóa vẫn nhập B. Kiểm tra khóa 15 phút được server thực thi.
5. Dùng ACCOUNTANT, RESIDENT, AUTHORITY; truy cập trực tiếp route và gọi API trái quyền. UI ẩn nút chỉ là hỗ trợ, API phải trả 403.
6. Cấp tài khoản nhiều vai trò, cư dân chưa liên kết/đã liên kết, trùng username/email. Đổi quyền/khóa tài khoản khác và kiểm tra request kế tiếp bị từ chối đúng.
7. Sửa hồ sơ, nhập sai mật khẩu cũ, thử mật khẩu không hợp lệ; đổi đúng, kiểm tra mật khẩu cũ và JWT cũ bị server từ chối. Thử sửa người khác qua API.
8. Cập nhật kết quả và bằng chứng từng AC trong AC_COVERAGE.md; chỉ đánh dấu tích hợp PASS sau khi thực sự chạy.

Nguồn công cụ kiểm thử: [Vitest](https://vitest.dev/guide/), [React Testing Library](https://testing-library.com/docs/react-testing-library/setup/). Các tài liệu đầu vào không được commit.
