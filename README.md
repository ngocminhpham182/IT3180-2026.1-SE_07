# BlueMoon — Frontend Sprint 1

Frontend tiếng Việt cho **US-01, US-02, US-08, US-09, US-10**. React 18, Vite 6, React Router 7, JavaScript/JSX và Fetch. Source nằm ngay tại root ứng dụng trên nhánh frontend, được tích hợp từ ZIP BlueMoon; không có frontend lồng nhau hoặc API mock trong sản phẩm.

## Chạy ứng dụng

Cần Node.js **22.12+** (đã kiểm chứng Node 24.22.1) và npm.

```powershell
npm ci
Copy-Item .env.example .env
npm run dev
```

Mở `http://localhost:5173`. Cấu hình `.env`:

```dotenv
VITE_API_BASE_URL=/api/v1
API_PROXY_TARGET=http://localhost:8080
```

Vite chuyển tiếp `/api` đến Spring Boot khi chạy dev. `API_PROXY_TARGET` chỉ là cấu hình dev server; thay đổi cần khởi động lại Vite. Nếu API ở domain khác, đặt `VITE_API_BASE_URL` thành URL đầy đủ và cấu hình CORS ở backend. Không đặt JWT, mật khẩu hay secret trong biến `VITE_*`.

**Backend trong repository tại thời điểm audit mới là khung Spring Boot, chưa có controller nghiệp vụ.** Khi backend chưa chạy, đăng nhập báo lỗi kết nối; ứng dụng không tự tạo dữ liệu/tài khoản demo. Cần tài khoản do nhóm backend cấp.

## Kiểm tra và build

```powershell
npm run check          # Unit + component/integration; HTTP mock chỉ trong tests
npm run format:check
npm run build          # dist/; không commit thư mục này
npx playwright install chromium
npm run test:browser   # Bản production, desktop/mobile; API mock trong test
npm run preview        # Xem build cục bộ
```

`npm run test` mở Vitest watch. `npm run format` định dạng source/test/tài liệu.

Production cần reverse proxy `/api/v1` đến Spring Boot hoặc URL API đầy đủ khi build; **Vite preview không dùng dev proxy**. Hosting phải rewrite các đường dẫn frontend về `index.html`, giữ `/api` phục vụ bởi backend. File `.env` không được commit.

## Route và quyền

| Route                              | Quyền           | Chức năng                                       |
| ---------------------------------- | --------------- | ----------------------------------------------- |
| `/dang-nhap`                       | Công khai       | Đăng nhập; đếm ngược khóa theo tài khoản        |
| `/`                                | Đã đăng nhập    | Truy cập nhanh theo vai trò                     |
| `/ho-gia-dinh`, `/ho-gia-dinh/:id` | BQT, ACCOUNTANT | Danh sách, chi tiết, thành viên; chỉ BQT sửa    |
| `/nhan-khau`, `/nhan-khau/:id`     | BQT, ACCOUNTANT | Tra cứu, chi tiết; chỉ BQT sửa/xóa nhập nhầm    |
| `/tai-khoan`                       | BQT             | Cấp tài khoản, nhiều vai trò, khóa/mở, chi tiết |
| `/ho-so`                           | Mọi vai trò     | Hồ sơ của mình và đổi mật khẩu                  |

RESIDENT/AUTHORITY vào hồ sơ sau đăng nhập; BQT/ACCOUNTANT vào danh sách hộ. JWT lưu trong `sessionStorage`, xác minh bằng `/me` trước khi mở nghiệp vụ. Đổi mật khẩu thành công hoặc phiên hết hạn/thu hồi sẽ xóa phiên. Không có refresh token. Backend phải kiểm tra quyền trên từng request.

## Tích hợp nhóm

Base đã được người dùng xác nhận: **frontend**. Nhánh làm việc: `feature/fe-us01-us02-us08-us09-us10`; PR dự kiến vào `frontend`. Không push hoặc merge tự động; chỉ push khi người dùng xác nhận `PUSH`.

- [Báo cáo tích hợp, audit và blocker](docs/INTEGRATION.md)
- [22 Acceptance Criteria và bằng chứng](docs/AC_COVERAGE.md)
- [Kết quả kiểm chứng](docs/VERIFICATION.md)
- [Nội dung PR đã chuẩn bị](docs/PR_DESCRIPTION.md)
