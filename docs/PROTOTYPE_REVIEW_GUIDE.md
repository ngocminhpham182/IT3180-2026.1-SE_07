# Hướng dẫn xem và duyệt thiết kế BlueMoon

## 1. Mở đúng bản thiết kế

- [Prototype desktop](https://www.figma.com/proto/Om3u1MbUXlSfRC6l8iN4px?node-id=3-2&starting-point-node-id=3%3A2): bắt đầu tại màn chọn kịch bản.
- [Prototype mobile cư dân](https://www.figma.com/proto/Om3u1MbUXlSfRC6l8iN4px?node-id=11-1992&starting-point-node-id=11%3A1992): bắt đầu tại hộ của cư dân.
- [Wireframe và cấu trúc luồng](https://www.figma.com/design/Om3u1MbUXlSfRC6l8iN4px?node-id=19-2310): mở trang `02 · Wireframes`, thu nhỏ để xem toàn bộ 5 wireframe và 5 bảng cấu trúc ở hàng phía dưới.
- [Report chi tiết](PROTOTYPE_WIREFRAME_REPORT.md): phạm vi, số liệu kiểm tra, dữ liệu giả và giới hạn.

Nếu Figma yêu cầu quyền truy cập, cần dùng tài khoản có quyền hoặc gửi yêu cầu truy cập cho chủ file. Liên kết GitHub không cấp thêm quyền Figma.

## 2. Kịch bản duyệt desktop

Mỗi kịch bản dùng dữ liệu điền sẵn. Chọn luồng từ màn đầu, bấm các nút trong màn để chuyển trạng thái. Quay lại màn chọn kịch bản hoặc mở lại liên kết desktop để bắt đầu luồng khác. Không cần nhập dữ liệu thật.

| Luồng | Các bước xem | Nội dung cần đánh giá |
|---|---|---|
| A — Hộ gia đình | Danh sách hộ → tạo hộ → tạo thành công → thêm cư dân → thêm thành công | Mã hộ/căn hộ, thông tin chủ hộ, nhóm trường nhập, kết quả thao tác |
| B — Biến động | Danh sách cư dân → chi tiết → lập biến động → xác nhận → kết quả | Ngữ cảnh cư dân, thông tin biến động, nội dung xác nhận và trạng thái cuối |
| C — Yêu cầu | Hộ của tôi → gửi chỉnh sửa → chờ duyệt → BQT xem yêu cầu → xác nhận duyệt → kết quả phía cư dân | Thông tin trước/sau, vai trò người xử lý, thông báo và khả năng nhận biết trạng thái |
| D — Kỳ thu | Khoản thu → xem trước → kỳ thu → tạo kỳ → xem trước phân bổ → sinh khoản phải thu | Cách thể hiện đơn giá, hộ được áp dụng/bỏ qua, tổng tiền và trạng thái kỳ |
| E — Tài khoản | Danh sách → tạo → đổi quyền → khóa → mở khóa | Liên kết cư dân, vai trò, cảnh báo thao tác và trạng thái tài khoản |

Trong file thiết kế, có thể mở màn `scenarioStates` để xem nhóm trạng thái bổ sung. Các nhánh được dựng để trình diễn có cả từ chối/hủy yêu cầu và xác nhận/lỗi; không suy diễn rằng mọi giá trị nhập bất kỳ đều được xử lý.

## 3. Kịch bản duyệt mobile

1. Từ hộ của tôi, mở biểu mẫu chỉnh sửa thành viên, xem trạng thái chờ duyệt, kết quả được duyệt và thông tin đã cập nhật.
2. Xem các màn yêu cầu tạm vắng và tạm trú, kiểm tra khả năng đọc thông tin và cuộn biểu mẫu.
3. Xem hồ sơ và luồng đổi mật khẩu; trạng thái đăng nhập/đổi mật khẩu là mô phỏng giao diện.
4. Kiểm tra vị trí nút chính, độ dễ đọc của nhãn, trạng thái và thông báo trên khung mobile.

## 4. Mẫu phản hồi

Ghi mỗi vấn đề thành một mục; ưu tiên kèm link frame hoặc tên màn chính xác.

| Trường | Nội dung cần ghi |
|---|---|
| Luồng / màn | Ví dụ: C / màn duyệt yêu cầu |
| Thao tác | Nút vừa bấm hoặc thông tin đang xem |
| Hiện trạng | Điều đang hiển thị trong thiết kế |
| Mong muốn | Cách hiển thị hoặc hành vi cần thay đổi |
| Ưu tiên | Cần sửa trước khi duyệt / có thể chỉnh sau |
| Dữ liệu thay thế | Giá trị mới nếu đã được xác nhận |

Sau khi duyệt, gửi dữ liệu thay thế theo nhóm: hộ/cư dân, yêu cầu, khoản thu/kỳ thu và tài khoản/vai trò. Các dữ liệu hiện tại đều là dữ liệu giả. Không cần cung cấp mật khẩu thật để duyệt hoặc cập nhật prototype.
