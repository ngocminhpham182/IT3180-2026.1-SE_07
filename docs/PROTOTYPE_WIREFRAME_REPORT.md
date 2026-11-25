# BlueMoon — Báo cáo Prototype và Wireframe Sprint 1

Ngày bàn giao tài liệu: 25/11/2026. Trạng thái: đã hoàn thiện bản thiết kế Figma cho 5 luồng đã thống nhất, sẵn sàng để người dùng duyệt. Dữ liệu trong thiết kế là dữ liệu giả; chưa phải bản được người dùng nghiệm thu.

## 1. Phạm vi và hình thức bàn giao

Yêu cầu đã thống nhất: thiết kế trên Figma, giao diện hoàn chỉnh (high fidelity), bao gồm cả 5 luồng, dùng dữ liệu giả trước và thay bằng dữ liệu người dùng cung cấp sau khi duyệt. Không sửa mã nguồn ứng dụng trong đợt thiết kế này.

Repository lưu báo cáo và hướng dẫn truy cập. Các frame, component và tương tác nằm trong file Figma được liên kết bên dưới. Đây không phải bản xuất `.fig`, PDF, ảnh hoặc mã nguồn frontend từ Figma. Git không lưu lịch sử thay đổi nội dung file Figma qua các liên kết này.

## 2. Liên kết bàn giao

| Nội dung | Liên kết |
|---|---|
| File thiết kế và màn chọn kịch bản | [BlueMoon · Sprint 1 · Prototype & Wireframe](https://www.figma.com/design/Om3u1MbUXlSfRC6l8iN4px?node-id=3-2) |
| Prototype desktop | [Mở chế độ trình diễn desktop](https://www.figma.com/proto/Om3u1MbUXlSfRC6l8iN4px?node-id=3-2&starting-point-node-id=3%3A2) |
| Prototype mobile cư dân | [Mở chế độ trình diễn mobile](https://www.figma.com/proto/Om3u1MbUXlSfRC6l8iN4px?node-id=11-1992&starting-point-node-id=11%3A1992) |
| Wireframe | [Mở trang wireframe tại luồng C](https://www.figma.com/design/Om3u1MbUXlSfRC6l8iN4px?node-id=19-2310) |
| Hướng dẫn duyệt | [Kịch bản xem và ghi nhận phản hồi](PROTOTYPE_REVIEW_GUIDE.md) |

Quyền truy cập phụ thuộc vào thiết lập chia sẻ của file Figma; việc đưa liên kết lên GitHub không tự thay đổi quyền Figma.

## 3. Cơ sở thiết kế

Thiết kế được đối chiếu với tài liệu và mã nguồn BlueMoon tại commit `8493524` của `sprint_1`, gồm backend Java/Spring và frontend React/Vite trong `frontend2`. Các nhóm nghiệp vụ được xem xét gồm hộ gia đình, cư dân, biến động, yêu cầu thay đổi, khoản thu, kỳ thu và tài khoản.

Khi chuẩn bị push tài liệu, remote đã có thêm các commit đến `bcca824` (cập nhật UI, UML và SDD). Các nhận xét mã nguồn trong báo cáo phản ánh mốc `8493524`, chưa phải đánh giá lại những cập nhật mới này.

Các vai trò được xem xét là BQT, ACCOUNTANT, RESIDENT và AUTHORITY. Cách trình bày chức năng theo vai trò nhằm thể hiện quyền thao tác trong kịch bản; prototype không thực thi cơ chế phân quyền bảo mật.

## 4. Danh mục sản phẩm trên Figma

| Trang | Nội dung đã làm |
|---|---|
| `00 · Components` | 11 component gốc; nền tảng màu sắc, khoảng cách, bo góc và các thành phần dùng lại |
| `01 · Desktop & Prototype` | 109 frame cấp cao nhất, gồm màn chính và các biến thể xác nhận, thành công, lỗi, trạng thái theo vai trò |
| `02 · Wireframes` | 5 wireframe giao diện xám và 5 bảng mô tả cấu trúc luồng |
| `03 · Mobile cư dân` | 19 frame cấp cao nhất cho các kịch bản cư dân trên màn hình nhỏ |

Số lượng frame bao gồm các trạng thái mô phỏng, không đồng nghĩa với 109 chức năng desktop riêng biệt. Kích thước desktop là 1440 × 1000; mobile là 390 × 844, có cuộn đối với nội dung dài.

### Wireframe

Năm wireframe giao diện tương ứng với danh sách hộ, biến động cư dân, duyệt yêu cầu, xem trước kỳ thu và tạo tài khoản. Năm bảng cấu trúc bổ sung giúp đọc tiến trình nghiệp vụ. Hai nhóm được bố trí thành hai hàng riêng để tránh chồng lấn. Đã chỉnh độ tương phản chữ trong các instance của wireframe. Các wireframe là bản bố cục tĩnh; tương tác trình diễn nằm ở các trang prototype.

### Giao diện hoàn chỉnh

Giao diện sử dụng font Be Vietnam Pro, màu chủ đạo xanh teal `#117784`, chữ chính `#1E3441`, nền `#F5F8FA`, bề mặt trắng và thanh điều hướng `#123D48`. Màu thành công, cảnh báo và lỗi được tách biệt. Các component gồm nút chính/phụ/nguy hiểm/vô hiệu, trường nhập, điều hướng, dòng dữ liệu, thẻ thống kê, thành viên và thông báo; có thêm thành phần phục vụ wireframe.

Thiết kế dùng text và component có thể chỉnh sửa trong Figma. Khoảng cách và bo góc được chuẩn hóa; đã gắn biến cho các giá trị phù hợp trên 901 container bố cục trong đợt thiết kế. Bản mobile ưu tiên tác vụ cư dân và thông tin hộ của chính cư dân.

## 5. Năm luồng nghiệp vụ

| Luồng | Đường đi chính và kết quả mô phỏng |
|---|---|
| A — Hộ gia đình | Danh sách hộ → tạo hộ → xác nhận tạo thành công → thêm cư dân → xác nhận đã thêm thành viên |
| B — Biến động cư dân | Danh sách cư dân → chi tiết cư dân → lập biến động → xác nhận → kết quả biến động |
| C — Yêu cầu cư dân và xử lý | Hộ của tôi → gửi yêu cầu chỉnh sửa → chờ duyệt → BQT xem và duyệt → cư dân xem kết quả và thông tin cập nhật |
| D — Khoản thu và kỳ thu | Khoản thu → xem trước → danh sách kỳ → tạo kỳ → kỳ chưa sinh khoản phải thu → xem trước phân bổ → kết quả sinh khoản phải thu |
| E — Tài khoản và quyền | Danh sách tài khoản → tạo tài khoản → cập nhật vai trò → khóa → mở khóa tài khoản |

Ngoài đường đi chính, thiết kế có các trạng thái phục vụ trình diễn như xác nhận, lỗi, từ chối/hủy yêu cầu, tạm vắng, tạm trú, trạng thái kỳ thu, khóa tài khoản và bảo vệ thao tác trên tài khoản của chính mình. Các biến thể giúp đánh giá cách giao diện phản hồi; chúng không phải kiểm thử backend.

Phân vai trong kịch bản: BQT thực hiện quản lý hộ/cư dân/tài khoản và duyệt yêu cầu; kế toán có ngữ cảnh khoản thu/kỳ thu; cư dân thực hiện tác vụ của hộ mình. Đóng/mở lại kỳ thu được thể hiện theo quyền BQT. Không xây dựng thêm một bộ luồng riêng cho AUTHORITY ngoài phạm vi 5 luồng đã thống nhất.

## 6. Dữ liệu giả và tính nhất quán

Các kịch bản có bộ dữ liệu mẫu riêng để trình diễn, không tạo thành một cơ sở dữ liệu dùng chung giữa mọi frame.

| Nhóm | Dữ liệu minh họa |
|---|---|
| Hộ cư dân chính | H001, căn A101, diện tích 75,5 m²; Nguyễn Văn An, Nguyễn Thị Bình, Nguyễn Minh Anh |
| Yêu cầu chỉnh sửa | Yêu cầu 031: Nguyễn Minh Anh → Nguyễn Minh Ánh; ngày kịch bản 23/11/2026 |
| Tạm vắng/tạm trú | Yêu cầu 032 cho Nguyễn Thị Bình; yêu cầu 033 cho Trần Minh Khang; khoảng ngày 23–30/11/2026 |
| Tạo hộ | H005, căn A105, diện tích 72 m²; Vũ Thị Mai và Vũ Minh Khôi |
| Tài khoản | `binh.a101`, liên kết cư dân Bình; mô phỏng cập nhật vai trò và khóa/mở khóa |

Ví dụ tính khoản thu trong luồng D:

- Phí quản lý: 7.000 đồng/m²; H001 là 528.500 đồng và H002 (80 m²) là 560.000 đồng.
- Phí vệ sinh cố định: 50.000 đồng × 3 hộ = 150.000 đồng.
- Tổng minh họa: **1.238.500 đồng**, gồm 5 khoản phải thu cho 3 hộ.
- H003 thiếu diện tích nên bỏ qua phí quản lý theo diện tích trong ví dụ; H004 không hoạt động nên được loại khỏi phân bổ.

Các ngày trong màn hình là dữ liệu kịch bản, không phải ngày hệ thống tự cập nhật. Sau khi duyệt, cần thay dữ liệu ở các frame liên quan và kiểm tra lại tổng tiền, tên, mã hộ, ngày và trạng thái. Component hỗ trợ chỉnh sửa giao diện nhưng chưa có cơ chế đồng bộ dữ liệu tập trung.

## 7. Kiểm tra đã thực hiện trong đợt thiết kế

| Nội dung | Kết quả ghi nhận |
|---|---|
| Thống kê desktop/mobile | 109 frame desktop, 19 frame mobile; 1.477 component instance trên hai trang |
| Tương tác điều hướng | 1.090 tương tác NODE NAVIGATE; không ghi nhận đích liên kết lỗi trong lần kiểm tra cuối |
| Đường đi A/B/C/D/E | Đã kiểm tra chuỗi chuyển màn chính; không thiếu cạnh trong các chuỗi kiểm tra |
| Mobile cư dân | Đã kiểm tra chuỗi gửi yêu cầu → chờ duyệt → được duyệt → cập nhật |
| Mobile tài khoản | Đã kiểm tra chuỗi đăng nhập → hộ → hồ sơ → đổi mật khẩu → kết quả → đăng nhập |
| Tràn nội dung | Không ghi nhận lỗi trong phép kiểm tra tràn ngang và tràn đáy desktop; mobile cho phép cuộn dọc |
| Kiểm tra hình ảnh | Đã xem các màn đại diện: danh sách/form hộ, duyệt yêu cầu, xem trước kỳ thu, mobile, màn chọn kịch bản và wireframe |

Các số liệu trên là kết quả kiểm tra Figma ở đợt hoàn thiện thiết kế trước khi viết tài liệu này. Không đồng nghĩa mọi frame đã được kiểm tra thủ công hoặc đã thực hiện usability test với người dùng thật. Tài liệu bàn giao này không chạy lại kiểm thử ứng dụng vì không sửa mã nguồn ứng dụng.

## 8. Giới hạn và lưu ý khi triển khai

Prototype dùng các màn và trạng thái điền sẵn. Các nút điều hướng mô phỏng kết quả; không có nhập liệu thực, tìm kiếm động, tính toán động, lưu dữ liệu, xác thực hay kết nối backend. Việc bấm chuyển tới màn thành công không chứng minh API đã hoạt động.

Các điểm đã phát hiện khi rà soát repo cần được đối chiếu lại khi triển khai:

- Frontend có fallback dữ liệu giả cho danh sách khoản phải thu theo kỳ trong `frontend2/src/api/services.js`; chưa có controller GET tương ứng trong backend được rà soát.
- Một số thao tác xóa hộ/khoản thu trên giao diện chưa có endpoint DELETE tương ứng; không coi đây là chức năng đã tích hợp.
- Backend có xử lý đặt lại mật khẩu nhưng phần client/UI tương ứng còn thiếu trong mã nguồn được rà soát.
- Giao diện đăng nhập có ngữ cảnh `ACCOUNT_LOCKED`, nhưng logic khóa do đăng nhập sai trong AuthService chưa tương ứng.
- Tài liệu cũ và mã nguồn có điểm lệch; ví dụ `FeeCategoryResponse` trong source có `inUse`. Cần ưu tiên đối chiếu mã nguồn thực tế khi nối API.

Đây là ghi nhận phục vụ thiết kế, không phải kết quả kiểm thử chạy backend hoặc cam kết sửa các khoảng trống trên trong đợt bàn giao tài liệu.

## 9. Bước duyệt và cập nhật tiếp theo

1. Duyệt bố cục wireframe của đủ 5 luồng.
2. Chạy prototype desktop và mobile theo hướng dẫn kèm theo.
3. Ghi phản hồi theo màn, thao tác, kết quả mong muốn và mức ưu tiên.
4. Sau khi duyệt, cung cấp bộ dữ liệu thay thế để cập nhật các frame và đối chiếu tính nhất quán.
5. Việc chuyển thiết kế thành mã nguồn frontend và nối API là phạm vi triển khai tiếp theo, chưa thực hiện trong đợt này.
