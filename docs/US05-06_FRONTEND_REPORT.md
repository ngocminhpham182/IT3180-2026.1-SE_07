# Báo cáo triển khai Frontend US-05 và US-06

- Ngày: 11/11/2026.
- Branch làm việc: `sprint_1`, nền commit `cb33ede`.
- Mã nguồn frontend: `frontend2/` (thư mục `frontend/` đã bị xóa trên branch).
- Tài liệu đầu vào: `SE_07-W02-UserStory (2).xlsx` — User Story và Acceptance Criteria của US-05, US-06; `API_Contract_US05-06_v7.md`.
- Phạm vi: frontend React, API client, phân quyền điều hướng và kiểm thử. Không sửa backend hoặc cơ sở dữ liệu.

## 1. Chức năng đã triển khai

### US-05 — Xem hộ và đề nghị sửa thông tin

Trang **Hộ của tôi** (`/ho-cua-toi`) lấy thông tin bằng `GET /me/household`, không truyền ID hộ. Hiển thị mã hộ, căn hộ, tầng, diện tích, trạng thái và danh sách thành viên cùng thông tin cá nhân, tình trạng cư trú, ngày hết hạn nếu có.

Mỗi thành viên có nút **Đề nghị sửa**. Form điền sẵn họ tên, ngày sinh, giới tính, CCCD/CMND, điện thoại, quan hệ với chủ hộ. Khi gửi, frontend gửi đủ sáu trường trong `profile`, chuẩn hóa khoảng trắng và giá trị tùy chọn rỗng thành `null`. Không gọi API sửa trực tiếp nhân khẩu.

Frontend chặn trường bắt buộc rỗng, sai độ dài, ngày không hợp lệ/ngày sinh tương lai, sai CCCD hoặc điện thoại và trường hợp không thay đổi gì. Giới tính chỉ gồm `MALE`, `FEMALE` hoặc không cung cấp theo contract v7. Sau khi gửi thành công, chuyển đến chi tiết đề nghị để xem trạng thái chờ duyệt.

### US-06 — Tạm vắng và tạm trú

- **Tạm vắng:** gửi cho thành viên được chọn từ hộ của mình, chỉ cho phép trạng thái `RESIDING`. Người liên quan được xác định bằng nút trên dòng thành viên, không có ô nhập ID tùy ý.
- **Tạm trú:** khai báo một người khách mới với sáu trường trong `person`; không gửi `residentId`.
- Hai loại đều bắt buộc ngày bắt đầu và ngày kết thúc; ngày bắt đầu không ở tương lai hoặc trước ngày sinh; ngày kết thúc không trước ngày bắt đầu. “Hôm nay” được tính theo `Asia/Ho_Chi_Minh`.
- Ghi chú tối đa 500 ký tự. Khóa nút khi đang gửi, kết hợp khóa đồng bộ để chống bấm hai lần.

### Theo dõi, duyệt và hủy đề nghị

| Màn hình             | Quyền    | Chức năng                                                                               |
| -------------------- | -------- | --------------------------------------------------------------------------------------- |
| `/de-nghi`           | RESIDENT | Danh sách đề nghị do chính mình gửi, lọc loại/trạng thái, phân trang, tải lại           |
| `/de-nghi/:id`       | RESIDENT | Chi tiết, so sánh trước/sau, kết quả và lý do từ chối; xác nhận hủy đề nghị đang chờ    |
| `/duyet-de-nghi`     | BQT      | Hàng đợi mặc định `PENDING`, lọc loại/trạng thái/mã hộ/căn hộ/ID hộ, phân trang         |
| `/duyet-de-nghi/:id` | BQT      | So sánh sáu trường và làm nổi bật `changes`; xem khai báo; duyệt hoặc từ chối kèm lý do |

BQT phải nhập lý do có nội dung trước khi từ chối; ghi chú duyệt là tùy chọn. Các thao tác duyệt, từ chối, hủy có bước xác nhận và khóa trong lúc xử lý. Khi đã có quyết định, các nút xử lý được ẩn. Duyệt thành công có liên kết đến hồ sơ/lịch sử biến động của `result.residentId`; cư dân có liên kết quay lại hộ để xem dữ liệu đã cập nhật.

Menu BQT hiển thị số đề nghị chờ từ `GET /resident-requests?status=PENDING&size=1`, cập nhật khi điều hướng và sau thao tác. Lỗi tải huy hiệu không chặn các chức năng khác. Menu, trang chủ và route được giới hạn theo vai trò; tài khoản có cả BQT và RESIDENT dùng được cả hai nhóm. Cư dân đăng nhập được điều hướng đến `/ho-cua-toi`.

## 2. Tích hợp API và lỗi

Đã bổ sung đủ chín endpoint của contract vào `src/api/services.js` thông qua `meApi` và `requestApi`. Tái sử dụng HTTP client hiện có: tiền tố `/api/v1`, Bearer token, đọc `data`, timeout, hủy request khi đổi trang/bộ lọc và xử lý phiên hết hạn.

| Tình huống                                                                                       | Cách xử lý frontend                                                                          |
| ------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------- |
| `VALIDATION_ERROR`, `DUPLICATE`                                                                  | Hiển thị thông báo theo đúng khóa field lồng như `profile.fullName`, `person.identityNumber` |
| `PENDING_REQUEST_EXISTS`                                                                         | Hiện lỗi và liên kết đến đề nghị cũ theo `details.requestId`; tải lại thông tin hộ           |
| `NO_CHANGES`                                                                                     | Chặn ở frontend, đồng thời hỗ trợ thông báo từ backend                                       |
| `RESIDENT_LINK_REQUIRED`                                                                         | Hướng dẫn liên hệ BQT để liên kết tài khoản với nhân khẩu                                    |
| `RESIDENT_NOT_IN_HOUSEHOLD`, `RESIDENT_NOT_RESIDING`, `INVALID_TRANSITION`, `HOUSEHOLD_INACTIVE` | Hiện lỗi và tải lại hộ; khóa gửi nếu dữ liệu mới không còn đủ điều kiện                      |
| `REQUEST_NOT_PENDING`                                                                            | Hiện thông báo và tải lại chi tiết để phản ánh quyết định mới nhất                           |
| `REQUEST_OUTDATED`                                                                               | Hiện cảnh báo và danh sách field đã đổi, chặn duyệt lại, hướng dẫn từ chối để cư dân gửi lại |
| 404 của chi tiết cư dân                                                                          | Giải thích không tìm thấy hoặc không phải đề nghị do mình gửi                                |

`pendingRequests` được tính cho toàn hộ, kể cả đề nghị do thành viên khác gửi. Nút sửa/tạm vắng cùng loại bị khóa khi đã có đề nghị chờ. Theo contract, chỉ người gửi được xem/hủy đề nghị của mình; nếu liên kết trỏ đến đề nghị của thành viên khác, frontend xử lý 404 và không thử gọi API quản trị để vượt quyền.

## 3. Đối chiếu Acceptance Criteria

| Tiêu chí  | Phần frontend đã đáp ứng                                                       | Phần cần xác minh với backend thật                                       |
| --------- | ------------------------------------------------------------------------------ | ------------------------------------------------------------------------ |
| US-05 AC1 | Form điền sẵn, POST đề nghị, hiển thị chờ duyệt, không sửa nhân khẩu trực tiếp | Dữ liệu gốc trong DB giữ nguyên trước khi duyệt                          |
| US-05 AC2 | Kiểm tra họ tên/ngày sinh và hiển thị lỗi field từ API                         | Backend kiểm tra lại dữ liệu                                             |
| US-05 AC3 | Route theo vai trò, đọc hộ qua `/me/household`, không chọn hộ khác             | Backend chặn truy cập trái quyền và kiểm tra quyền sở hữu                |
| US-05 AC4 | So sánh trước/sau, duyệt/từ chối có lý do, cư dân xem kết quả                  | Transaction cập nhật đúng field khi duyệt, không đổi dữ liệu khi từ chối |
| US-06 AC1 | Form tạm vắng/tạm trú, payload đúng loại, hiển thị trạng thái                  | Lưu đề nghị `PENDING` trong DB                                           |
| US-06 AC2 | Bắt buộc đủ hai ngày và kiểm tra khoảng ngày                                   | Kiểm tra lại ngày ở backend                                              |
| US-06 AC3 | Chỉ chọn thành viên hộ của mình, chỉ tạm vắng người `RESIDING`                 | Backend từ chối ID thuộc hộ khác kể cả khi request bị sửa thủ công       |
| US-06 AC4 | Duyệt và liên kết đến hồ sơ/lịch sử biến động                                  | Backend ghi biến động/tạo nhân khẩu đúng quy tắc                         |

## 4. Tệp chính

| Tệp trong `frontend2/`                                                  | Vai trò                                            |
| ----------------------------------------------------------------------- | -------------------------------------------------- |
| `src/pages/MyHouseholdPage.jsx`                                         | Thông tin hộ, thành viên, trạng thái và quyền gửi  |
| `src/components/ResidentRequestForm.jsx`                                | Form cho ba loại đề nghị                           |
| `src/pages/ResidentRequestsPage.jsx`                                    | Danh sách cho cư dân/BQT, bộ lọc và phân trang     |
| `src/pages/ResidentRequestDetailPage.jsx`                               | Chi tiết, so sánh, duyệt/từ chối/hủy, kết quả      |
| `src/utils/residentRequests.js`                                         | Nhãn, chuẩn hóa payload, validation, ngày Việt Nam |
| `src/api/services.js`, `src/api/http.js`                                | Endpoint và thông báo lỗi mới                      |
| `src/App.jsx`, `src/auth/permissions.js`                                | Route được bảo vệ và điều hướng sau đăng nhập      |
| `src/components/Layout.jsx`, `src/pages/HomePage.jsx`, `src/styles.css` | Menu, huy hiệu, truy cập nhanh và giao diện        |
| `tests/resident-requests.test.mjs`                                      | Test validation và múi giờ                         |
| `tests/resident-request-flows.test.jsx`                                 | Test tương tác, API, phân quyền, xung đột          |
| `tests/browser/resident-requests.spec.js`                               | Luồng trên trình duyệt thật ở desktop/mobile       |

Các test đăng nhập/phân quyền và mock hồi quy cũ được cập nhật để phản ánh trang mặc định mới và API huy hiệu chờ duyệt.

## 5. Kiểm thử và giới hạn xác minh

- `npm.cmd run check`: 29 test Node và 94 test Vitest. Bao gồm test cũ và test mới về payload sáu trường, ngày, giới tính, quyền truy cập, gửi lặp, pending toàn hộ, từ chối có lý do, dữ liệu lỗi thời, hủy khi đã được xử lý và bộ lọc.
- `npm.cmd run build`: tạo production bundle thành công.
- `npm.cmd run test:browser`: **10/10 test đạt**, gồm hồi quy chức năng cũ và luồng gửi/hủy US-05, duyệt khách tạm trú US-06 ở desktop 1440×1000 và mobile 390×844; kiểm tra tràn ngang và chụp ảnh giao diện. Đã xem ảnh form mobile và màn hình duyệt desktop.
- Prettier kiểm tra các tệp thay đổi đạt yêu cầu.

**Các test HTTP sử dụng mock theo contract, không phải kiểm thử tích hợp với Spring Boot/PostgreSQL.** Qua kiểm tra mã nguồn backend hiện tại, chưa có endpoint `/me/household` và nhóm `resident-requests`. Frontend thực tế gọi API thật, không tự tạo dữ liệu thành công giả khi API chưa có. Nhóm backend cần triển khai đủ API, phân quyền và migration V2 theo contract để sử dụng chức năng hoàn chỉnh.

Ngày bắt đầu so với biến động gần nhất, uniqueness ở DB, quyền sở hữu và transaction khi duyệt là trách nhiệm backend. FE hiển thị lỗi trả về; `/me/household` không cung cấp lịch sử biến động để kiểm tra quy tắc đó tại client.

## 6. Chạy và nghiệm thu tích hợp

```powershell
cd frontend2
npm.cmd ci
npm.cmd run dev
```

Vite chạy ở `http://127.0.0.1:5173`, proxy `/api/v1` về `http://localhost:8080` và bỏ tiền tố. Có thể cấu hình `API_PROXY_TARGET` nếu backend chạy ở địa chỉ khác.

Sau khi backend US-05/06 sẵn sàng:

1. Đăng nhập tài khoản RESIDENT đã gắn nhân khẩu; mở Hộ của tôi, gửi sửa thông tin và xác nhận dữ liệu gốc chưa đổi.
2. Gửi tạm vắng cho thành viên đang cư trú, gửi tạm trú cho khách mới; thử thiếu ngày, ngày tương lai, CCCD trùng và đề nghị trùng.
3. Đăng nhập BQT; kiểm tra hàng đợi, duyệt hoặc từ chối có lý do. Với tạm vắng/tạm trú, kiểm tra hồ sơ và lịch sử biến động.
4. Quay lại tài khoản cư dân, mở Đề nghị của tôi để xem kết quả; thử hủy đề nghị còn chờ.
5. Kiểm tra tài khoản khác hộ, ACCOUNTANT/AUTHORITY, tài khoản chưa liên kết nhân khẩu và hộ INACTIVE.
6. Thử hai người xử lý đồng thời và sửa dữ liệu gốc trước khi duyệt để kiểm tra `REQUEST_NOT_PENDING`/`REQUEST_OUTDATED` với DB thật.

Theo contract v7: không có thông báo đẩy; tạm trú là khách mới; US-05 không thêm/xóa thành viên hoặc đổi chủ hộ; ngày bắt đầu tạm vắng/tạm trú không ở tương lai.
