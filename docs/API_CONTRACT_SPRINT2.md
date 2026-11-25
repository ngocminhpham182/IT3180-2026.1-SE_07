# API CONTRACT ĐẶC TẢ CHI TIẾT SPRINT 2 (US-07, US-15, US-16, US-17, US-18)

> **Dự án**: BlueMoon – Hệ thống Quản lý Chung cư & Khu Dân cư  
> **Phiên bản tài liệu**: 2.0 (Chuẩn hóa cho Frontend & Backend)  
> **Trạng thái**: Sẵn sàng triển khai độc lập và tích hợp  
> **Phạm vi User Story**: 
> - **US-07**: Tra cứu, xác thực cư trú theo thẩm quyền (Cơ quan chức năng)
> - **US-15**: Ghi nhận thu tiền mặt tại quầy và cập nhật công nợ (Kế toán)
> - **US-16**: Theo dõi danh sách hộ đã nộp, chưa nộp và nợ đọng (Ban quản trị)
> - **US-17**: Cư dân tra cứu công nợ và lịch sử nộp phí của hộ (Cư dân)
> - **US-18**: Quản lý các đợt vận động đóng góp tự nguyện (Ban quản trị)

---

## 1. QUY ƯỚC DÙNG CHUNG TOÀN HỆ THỐNG

### 1.1 Cơ chế xác thực & Phân quyền (Authentication & Authorization)
- **Header xác thực**: `Authorization: Bearer <accessToken>` cho tất cả các endpoint được bảo vệ.
- **Thời hạn JWT**: Mặc định `28800` giây (8 giờ). Mỗi request đều kiểm tra tài khoản `user.active == true`, `passwordChangedAt` và vai trò (`roles`) mới nhất từ DB.
- **Các vai trò (RoleCode)**:
  - `BQT`: Ban quản trị tòa nhà (toàn quyền quản trị cư dân, duyệt đề nghị, thiết lập kỳ thu, đợt vận động).
  - `ACCOUNTANT`: Kế toán (quản lý khoản thu, lập danh sách phí, thu tiền mặt, theo dõi công nợ).
  - `RESIDENT`: Cư dân (chỉ xem thông tin, công nợ, phiếu thu và gửi đề nghị liên quan đến căn hộ của chính mình).
  - `AUTHORITY`: Cơ quan chức năng (Công an khu vực, chính quyền - chỉ có quyền tra cứu, xác thực thông tin cư trú và xem báo cáo theo thẩm quyền; mọi lượt tra cứu đều bị ghi log kiểm toán).

### 1.2 Khuôn dạng dữ liệu trả về (Unified Response Envelope)
Toàn bộ API trả về kết quả tuân thủ nghiêm ngặt định dạng chuẩn của hệ thống (lớp `ApiResponse<T>` trong backend và trình xử lý `http.js` trong frontend):

#### a. Response thành công (200 OK / 201 Created)
```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": { ... }
}
```
*Lưu ý: Đối với HTTP `204 No Content` (như hủy bản ghi, xóa), HTTP header trả về 204 và không có body.*

#### b. Response phân trang chuẩn (`PageResponse<T>`)
Áp dụng cho mọi API danh sách có phân trang:
```json
{
  "success": true,
  "message": "Lấy danh sách thành công",
  "data": {
    "content": [ ... ],
    "page": 1,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```
- Quy ước phân trang: `page` bắt đầu từ `1` (1-based index). Tham số mặc định: `page=1`, `size=20` (tối đa `100`).

#### c. Response lỗi chuẩn (`ApiErrorResponse`)
Đồng bộ hoàn toàn với `GlobalExceptionHandler` và lớp `ApiError` của Frontend:
```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "Thông báo lỗi tiếng Việt thân thiện với người dùng",
  "errors": {
    "fieldName": "Chi tiết lỗi của trường cụ thể (dành cho form validation)"
  },
  "details": {
    "key": "Thông tin phụ trợ bổ sung cho nghiệp vụ (nếu có)"
  }
}
```
*Ghi chú: `@JsonInclude(NON_NULL)` tự động ẩn các trường `errors` hoặc `details` nếu mang giá trị `null`.*

### 1.3 Quy ước kiểu dữ liệu, Thời gian & Tiền tệ
- **Ngày tháng (`LocalDate`)**: Định dạng chuẩn `yyyy-MM-dd` (ví dụ: `2026-11-23`). Múi giờ nghiệp vụ: `Asia/Ho_Chi_Minh`.
- **Thời điểm (`Instant`)**: Định dạng chuẩn ISO-8601 UTC (ví dụ: `2026-11-23T08:30:00Z`).
- **Tiền tệ (VND)**: Kiểu số nguyên `Long` hoặc `BigDecimal` quy tròn nguyên tệ (`scale = 0`, làm tròn `HALF_UP`), không dùng số âm cho số tiền nộp hoặc phát sinh nợ.
- **Diện tích & Đơn giá**: Diện tích `NUMERIC(10,2)` (tối đa 2 chữ số thập phân), đơn giá `NUMERIC(12,2)`.

---

## 2. BẢNG MÃ LỖI TỔNG HỢP CHO SPRINT 2

| HTTP Status | Mã lỗi (`code`) | Mô tả nguyên nhân | Cấu trúc payload bổ sung (`errors`/`details`) |
| :--- | :--- | :--- | :--- |
| **400** | `VALIDATION_ERROR` | Dữ liệu gửi lên sai định dạng, thiếu trường bắt buộc, ngày kết thúc < ngày bắt đầu, số tiền <= 0 | `errors: { field: "message" }` |
| **401** | `UNAUTHORIZED` | Chưa truyền Bearer token hoặc phiên làm việc đã hết | Không có body phụ |
| **401** | `TOKEN_INVALID` | Token không hợp lệ, tài khoản bị vô hiệu hóa hoặc đã đổi mật khẩu | Không có body phụ |
| **403** | `FORBIDDEN` | Sai vai trò hoặc người dùng cố truy cập dữ liệu hộ khác (Cư dân xem hộ khác, xem báo cáo toàn tòa) | Không có body phụ |
| **404** | `NOT_FOUND` | Không tìm thấy Hộ, Nhân khẩu, Khoản thu, Phiếu thu, Đợt quyên góp | Không có body phụ |
| **409** | `DUPLICATE` | Trùng mã đợt vận động, trùng mã biên lai | `errors: { field: "Đã tồn tại" }` |
| **409** | `VERSION_CONFLICT` | **Xung đột phiên bản / Ghi nhận đồng thời (AC5 - US-15)**: Hai kế toán cùng thu một khoản nợ tại cùng một thời điểm | `details: { receivableId, currentVersion, expectedVersion }` |
| **409** | `RECEIVABLE_ALREADY_PAID` | Khoản phải thu đã được nộp đủ trước đó, không thể nộp thêm | `details: { receivableId, paidAmount, outstandingAmount: 0 }` |
| **422** | `PERIOD_CLOSED` | **Kỳ thu đã đóng sổ (AC4 - US-15)**: Không được phép ghi nhận nộp tiền vào kỳ đã đóng | `details: { billingPeriodId, closedAt }` |
| **422** | `PERIOD_NOT_GENERATED` | **Kỳ thu chưa được lập danh sách (AC3 - US-16)** | `details: { billingPeriodId }` |
| **422** | `CAMPAIGN_CLOSED` | **Đợt quyên góp đã kết thúc / đã đóng (AC4 - US-18)**: Không tiếp nhận đóng góp mới | `details: { campaignId, status: "CLOSED", endDate }` |
| **422** | `INVALID_PAYMENT_AMOUNT` | Số tiền thanh toán lớn hơn số nợ còn lại của khoản thu | `details: { outstandingAmount, requestedAmount }` |
| **422** | `HOUSEHOLD_INACTIVE` | Thao tác trên căn hộ đang ở trạng thái ngừng hoạt động (`INACTIVE`) | `details: { householdId }` |
| **422** | `RESIDENT_LINK_REQUIRED` | Tài khoản role `RESIDENT` nhưng chưa được gắn với nhân khẩu trong hệ thống | Không có body phụ |

---

## 3. CƠ SỞ DỮ LIỆU & BỔ SUNG SCHEMA (SPRINT 2)

Để các API của Sprint 2 hoạt động độc lập và toàn vẹn với hệ thống hiện hữu (không tác động các bảng đã có ngoài việc bổ sung trường / bảng mới):

### 3.1 Bổ sung vào bảng `receivable` hiện tại:
- `paid_amount NUMERIC(14, 2) NOT NULL DEFAULT 0`: Tổng số tiền đã thanh toán cho khoản này.
- `payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID'))`: Trạng thái thanh toán của khoản phải thu.
*(Cột `version BIGINT NOT NULL DEFAULT 0` đã có sẵn trong bảng `receivable` để hỗ trợ cơ chế Optimistic Locking).*

### 3.2 Bảng mới: `payment_receipt` (Phiếu thu tiền mặt - US-15, US-16, US-17)
```sql
CREATE TABLE payment_receipt (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    receipt_code   VARCHAR(30)    NOT NULL UNIQUE, -- e.g. "PT-202611-0001"
    household_id   BIGINT         NOT NULL REFERENCES household(id) ON DELETE RESTRICT,
    total_amount   NUMERIC(14, 2) NOT NULL CHECK (total_amount > 0),
    payment_method VARCHAR(20)    NOT NULL DEFAULT 'CASH' CHECK (payment_method IN ('CASH', 'TRANSFER')),
    payment_date   DATE           NOT NULL,
    note           VARCHAR(500),
    created_by     BIGINT         NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_receipt_household ON payment_receipt (household_id);
CREATE INDEX idx_receipt_date ON payment_receipt (payment_date DESC);
```

### 3.3 Bảng mới: `payment_receipt_detail` (Chi tiết phân bổ khoản thu trong phiếu)
```sql
CREATE TABLE payment_receipt_detail (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    receipt_id     BIGINT         NOT NULL REFERENCES payment_receipt(id) ON DELETE CASCADE,
    receivable_id  BIGINT         NOT NULL REFERENCES receivable(id) ON DELETE RESTRICT,
    amount_paid    NUMERIC(14, 2) NOT NULL CHECK (amount_paid > 0),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_receipt_detail_receivable ON payment_receipt_detail (receivable_id);
```

### 3.4 Bảng mới: `residence_audit_log` (Nhật ký tra cứu cư trú của cơ quan chức năng - US-07)
```sql
CREATE TABLE residence_audit_log (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT         NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    action_type    VARCHAR(50)    NOT NULL, -- "VERIFY_HOUSEHOLD", "VERIFY_RESIDENT", "SEARCH_RESIDENCE"
    search_keyword VARCHAR(255),
    target_type    VARCHAR(30),   -- "HOUSEHOLD", "RESIDENT"
    target_id      BIGINT,
    ip_address     VARCHAR(50),
    viewed_at      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_user_date ON residence_audit_log (user_id, viewed_at DESC);
```

### 3.5 Bảng mới: `donation_campaign` & `donation_record` (Đợt vận động tự nguyện - US-18)
```sql
CREATE TABLE donation_campaign (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code           VARCHAR(30)    NOT NULL UNIQUE, -- e.g. "BAO_SO_3_2026"
    title          VARCHAR(200)   NOT NULL,
    description    TEXT,
    target_amount  NUMERIC(14, 2) DEFAULT 0 CHECK (target_amount >= 0),
    start_date     DATE           NOT NULL,
    end_date       DATE           NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    created_by     BIGINT         NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (end_date >= start_date)
);

CREATE TABLE donation_record (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    campaign_id    BIGINT         NOT NULL REFERENCES donation_campaign(id) ON DELETE RESTRICT,
    household_id   BIGINT         REFERENCES household(id) ON DELETE RESTRICT,
    resident_id    BIGINT         REFERENCES resident(id)  ON DELETE RESTRICT,
    donor_name     VARCHAR(150)   NOT NULL, -- Họ tên người/hộ đóng góp
    amount         NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    donation_date  DATE           NOT NULL,
    note           VARCHAR(500),
    recorded_by    BIGINT         NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    recorded_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_donation_campaign ON donation_record (campaign_id);
CREATE INDEX idx_donation_household ON donation_record (household_id);
```

---

## 4. MA TRẬN PHÂN QUYỀN API SPRINT 2

| Nhóm Endpoint | Endpoint URL | HTTP Method | Public | `BQT` | `ACCOUNTANT` | `RESIDENT` | `AUTHORITY` |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **US-07** | `/authority/residence/verify` | GET | ❌ | ❌ | ❌ | ❌ | ✅ |
| **US-07** | `/authority/households/{id}` | GET | ❌ | ❌ | ❌ | ❌ | ✅ |
| **US-07** | `/authority/residents/{id}` | GET | ❌ | ❌ | ❌ | ❌ | ✅ |
| **US-07** | `/authority/audit-logs` | GET | ❌ | ✅ | ❌ | ❌ | ✅ |
| **US-15** | `/payments/cash` | POST | ❌ | ✅ | ✅ | ❌ | ❌ |
| **US-15** | `/payments/receipts` | GET | ❌ | ✅ | ✅ | ❌ | ❌ |
| **US-15** | `/payments/receipts/{id}` | GET | ❌ | ✅ | ✅ | ❌ | ❌ |
| **US-16** | `/billing-periods/{id}/household-debts` | GET | ❌ | ✅ | ✅ | ❌ | ❌ |
| **US-16** | `/billing-periods/{id}/debt-summary` | GET | ❌ | ✅ | ✅ | ❌ | ❌ |
| **US-16** | `/households/{id}/debt-details` | GET | ❌ | ✅ | ✅ | ❌ | ❌ |
| **US-17** | `/me/receivables` | GET | ❌ | ❌ | ❌ | ✅ | ❌ |
| **US-17** | `/me/receivables/summary` | GET | ❌ | ❌ | ❌ | ✅ | ❌ |
| **US-17** | `/me/payments` | GET | ❌ | ❌ | ❌ | ✅ | ❌ |
| **US-17** | `/me/payments/{id}` | GET | ❌ | ❌ | ❌ | ✅ | ❌ |
| **US-18** | `/donation-campaigns` | POST | ❌ | ✅ | ❌ | ❌ | ❌ |
| **US-18** | `/donation-campaigns` | GET | ❌ | ✅ | ✅ | ✅ | ❌ |
| **US-18** | `/donation-campaigns/{id}` | GET | ❌ | ✅ | ✅ | ✅ | ❌ |
| **US-18** | `/donation-campaigns/{id}` | PUT | ❌ | ✅ | ❌ | ❌ | ❌ |
| **US-18** | `/donation-campaigns/{id}/status` | PATCH | ❌ | ✅ | ❌ | ❌ | ❌ |
| **US-18** | `/donation-campaigns/{id}/donations` | POST | ❌ | ✅ | ✅ | ❌ | ❌ |
| **US-18** | `/donation-campaigns/{id}/donations` | GET | ❌ | ✅ | ✅ | ❌ | ❌ |
| **US-18** | `/me/donations` | GET | ❌ | ❌ | ❌ | ✅ | ❌ |

---

## 5. ĐẶC TẢ CHI TIẾT ENDPOINTS CHO TỪNG USER STORY

---

### 5.1 US-07: Tra cứu & Xác thực Cư trú (Cơ quan chức năng)

#### 1. `GET /authority/residence/verify`
Tra cứu, kiểm tra xác thực thông tin cư trú của cư dân hoặc hộ gia đình theo từ khóa định danh.
- **Quyền**: `AUTHORITY`
- **Query Parameters**:
  - `keyword` (*bắt buộc, String 2..100*): Số CMND/CCCD, họ tên, mã hộ khẩu (`householdCode`), hoặc số căn hộ (`apartmentNo`).
  - `searchType` (*optional, Enum*): `IDENTITY_NUMBER`, `HOUSEHOLD_CODE`, `APARTMENT_NO`, `FULL_NAME` (mặc định: tự động nhận diện).
- **Hệ thống xử lý (AC1, AC4)**:
  - Chỉ xem thông tin (Read-only), không chỉnh sửa.
  - Tự động ghi bản ghi vào `residence_audit_log` lưu lại `user_id`, thời điểm, nội dung tìm kiếm.
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Tra cứu thông tin cư trú thành công",
  "data": {
    "totalMatches": 1,
    "residents": [
      {
        "id": 15,
        "fullName": "Nguyễn Văn Tuấn",
        "dateOfBirth": "1992-05-14",
        "gender": "MALE",
        "identityNumber": "001092008899",
        "phone": "0987654321",
        "residencyStatus": "RESIDING",
        "isHead": false,
        "relationshipToHead": "Con",
        "statusUntil": null,
        "household": {
          "id": 3,
          "householdCode": "H003",
          "apartmentNo": "A302",
          "floor": 3,
          "status": "ACTIVE",
          "headFullName": "Nguyễn Văn An"
        },
        "moveInDate": "2024-02-01",
        "historyMovementsCount": 2
      }
    ],
    "households": [
      {
        "id": 3,
        "householdCode": "H003",
        "apartmentNo": "A302",
        "floor": 3,
        "status": "ACTIVE",
        "head": {
          "id": 10,
          "fullName": "Nguyễn Văn An",
          "identityNumber": "001068001122",
          "phone": "0912345678"
        },
        "residentCount": 4,
        "presentCount": 3
      }
    ]
  }
}
```
- **Lỗi thường gặp (AC2, AC3)**:
  - `400 VALIDATION_ERROR`: `keyword` rỗng hoặc < 2 ký tự.
  - `403 FORBIDDEN`: Tài khoản không có vai trò `AUTHORITY`.
  - `404 NOT_FOUND`: Không tìm thấy bản ghi cư trú nào khớp từ khóa (hoặc trả danh sách rỗng kèm message rõ ràng).

#### 2. `GET /authority/households/{id}`
Xem chi tiết hồ sơ xác thực của một hộ gia đình kèm toàn bộ nhân khẩu đang cư trú/tạm trú/tạm vắng.
- **Quyền**: `AUTHORITY`
- **Path Parameter**: `id` (*Long, ID của hộ*).
- **Hệ thống xử lý**: Tự động ghi audit log với `action_type = "VERIFY_HOUSEHOLD"`, `target_id = {id}`.
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy chi tiết xác thực hộ gia đình thành công",
  "data": {
    "id": 3,
    "householdCode": "H003",
    "apartmentNo": "A302",
    "floor": 3,
    "area": 85.5,
    "status": "ACTIVE",
    "head": {
      "id": 10,
      "fullName": "Nguyễn Văn An",
      "dateOfBirth": "1968-08-15",
      "gender": "MALE",
      "identityNumber": "001068001122",
      "phone": "0912345678",
      "residencyStatus": "RESIDING"
    },
    "residents": [
      {
        "id": 10,
        "fullName": "Nguyễn Văn An",
        "dateOfBirth": "1968-08-15",
        "gender": "MALE",
        "identityNumber": "001068001122",
        "relationshipToHead": "Chủ hộ",
        "isHead": true,
        "residencyStatus": "RESIDING",
        "statusUntil": null
      },
      {
        "id": 15,
        "fullName": "Nguyễn Văn Tuấn",
        "dateOfBirth": "1992-05-14",
        "gender": "MALE",
        "identityNumber": "001092008899",
        "relationshipToHead": "Con",
        "isHead": false,
        "residencyStatus": "TEMPORARILY_ABSENT",
        "statusUntil": "2026-12-31"
      }
    ],
    "auditLogged": true
  }
}
```
- **Lỗi thường gặp**: `403 FORBIDDEN`, `404 NOT_FOUND` ("Hộ gia đình không tồn tại").

#### 3. `GET /authority/residents/{id}`
Xem chi tiết hồ sơ nhân khẩu và toàn bộ lịch sử biến động cư trú.
- **Quyền**: `AUTHORITY`
- **Path Parameter**: `id` (*Long, ID của nhân khẩu*).
- **Hệ thống xử lý**: Tự động ghi audit log `VERIFY_RESIDENT`.
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy hồ sơ nhân khẩu thành công",
  "data": {
    "id": 15,
    "fullName": "Nguyễn Văn Tuấn",
    "dateOfBirth": "1992-05-14",
    "gender": "MALE",
    "identityNumber": "001092008899",
    "phone": "0987654321",
    "relationshipToHead": "Con",
    "isHead": false,
    "residencyStatus": "TEMPORARILY_ABSENT",
    "statusUntil": "2026-12-31",
    "household": {
      "id": 3,
      "householdCode": "H003",
      "apartmentNo": "A302"
    },
    "movements": [
      {
        "id": 45,
        "movementType": "TEMPORARY_ABSENCE",
        "effectiveDate": "2026-06-01",
        "startDate": "2026-06-01",
        "endDate": "2026-12-31",
        "note": "Học tập tại Nhật Bản",
        "recordedAt": "2026-05-28T09:00:00Z"
      },
      {
        "id": 12,
        "movementType": "MOVE_IN",
        "effectiveDate": "2024-02-01",
        "startDate": null,
        "endDate": null,
        "note": "Chuyển vào cùng gia đình",
        "recordedAt": "2024-02-01T08:00:00Z"
      }
    ]
  }
}
```

---

### 5.2 US-15: Ghi nhận Thu tiền mặt tại quầy & Cập nhật Công nợ (Kế toán)

#### 1. `POST /payments/cash`
Ghi nhận thu tiền mặt tại quầy cho một hoặc nhiều khoản phải thu của một hộ gia đình.
- **Quyền**: `ACCOUNTANT`, `BQT`
- **Request Body (`CashPaymentRequest`)**:
```json
{
  "householdId": 1,
  "paymentDate": "2026-11-23",
  "note": "Cư dân nộp tiền mặt tại quầy",
  "items": [
    {
      "receivableId": 101,
      "amount": 420000,
      "expectedVersion": 0
    },
    {
      "receivableId": 102,
      "amount": 200000,
      "expectedVersion": 0
    }
  ]
}
```
- **Validation rules**:
  - `householdId`: `@NotNull`, `@Positive`.
  - `paymentDate`: `@NotNull`, không được là ngày trong tương lai (`@PastOrPresent`).
  - `items`: `@NotEmpty`, mỗi phần tử có:
    - `receivableId`: `@NotNull`, `@Positive`.
    - `amount`: `@NotNull`, `@Positive` (> 0).
    - `expectedVersion`: `@NotNull` (phiên bản `version` của dòng `receivable` hiện tại trên frontend để chống ghi nhận đồng thời - AC5).
- **Hệ thống xử lý & Nghiệp vụ**:
  1. Kiểm tra hộ `householdId` có đang `ACTIVE` không. Nếu không -> `422 HOUSEHOLD_INACTIVE`.
  2. Với từng `receivableId`:
     - Khóa dòng (`SELECT ... FOR UPDATE`).
     - Kiểm tra kỳ thu `billingPeriod.status`: Nếu đã `CLOSED` -> từ chối `422 PERIOD_CLOSED` (AC4).
     - Kiểm tra `version`: Nếu `receivable.version != item.expectedVersion` -> từ chối `409 VERSION_CONFLICT` (AC5).
     - Tính số tiền còn nợ: `outstanding = amountDue - paidAmount`.
     - Nếu `outstanding <= 0` -> từ chối `409 RECEIVABLE_ALREADY_PAID`.
     - Nếu `item.amount > outstanding` -> từ chối `422 INVALID_PAYMENT_AMOUNT` ("Số tiền nộp vượt quá dư nợ còn lại").
     - Cập nhật `paidAmount = paidAmount + item.amount`.
     - Nếu `paidAmount == amountDue`: Cập nhật `paymentStatus = 'PAID'` (AC1).
     - Nếu `paidAmount < amountDue`: Cập nhật `paymentStatus = 'PARTIALLY_PAID'` (AC2).
  3. Tạo 1 bản ghi `payment_receipt` và các bản ghi con `payment_receipt_detail`.
- **Response 201 Created (`CashPaymentResponse`)**:
```json
{
  "success": true,
  "message": "Ghi nhận thu tiền mặt thành công",
  "data": {
    "receiptId": 501,
    "receiptCode": "PT-202611-0001",
    "householdId": 1,
    "householdCode": "H001",
    "apartmentNo": "A101",
    "totalAmount": 620000,
    "paymentDate": "2026-11-23",
    "paymentMethod": "CASH",
    "recordedBy": {
      "id": 2,
      "fullName": "Trần Thị Kế Toán"
    },
    "createdAt": "2026-11-23T08:45:00Z",
    "items": [
      {
        "receivableId": 101,
        "feeName": "Phí quản lý",
        "amountDue": 420000,
        "paidAmount": 420000,
        "outstandingAmount": 0,
        "status": "PAID",
        "newVersion": 1
      },
      {
        "receivableId": 102,
        "feeName": "Phí dịch vụ vệ sinh",
        "amountDue": 420000,
        "paidAmount": 200000,
        "outstandingAmount": 220000,
        "status": "PARTIALLY_PAID",
        "newVersion": 1
      }
    ]
  }
}
```
- **Lỗi thường gặp (AC3, AC4, AC5)**:
  - `400 VALIDATION_ERROR`: `amount <= 0` hoặc sai định dạng ngày.
  - `409 VERSION_CONFLICT`: Hai người cùng thu một khoản nợ tại cùng thời điểm (Optimistic Locking).
  - `422 PERIOD_CLOSED`: Kỳ thu của khoản phải thu này đã đóng sổ.
  - `422 INVALID_PAYMENT_AMOUNT`: Nộp số tiền vượt quá công nợ còn lại.

#### 2. `GET /payments/receipts/{id}`
Xem chi tiết biên lai thu tiền đã lập (hỗ trợ in biên lai hoặc xuất PDF).
- **Quyền**: `ACCOUNTANT`, `BQT`
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy chi tiết phiếu thu thành công",
  "data": {
    "id": 501,
    "receiptCode": "PT-202611-0001",
    "household": {
      "id": 1,
      "householdCode": "H001",
      "apartmentNo": "A101",
      "headFullName": "Nguyễn Văn An"
    },
    "totalAmount": 620000,
    "paymentMethod": "CASH",
    "paymentDate": "2026-11-23",
    "note": "Cư dân nộp tiền mặt tại quầy",
    "createdByName": "Trần Thị Kế Toán",
    "createdAt": "2026-11-23T08:45:00Z",
    "details": [
      {
        "receivableId": 101,
        "feeName": "Phí quản lý",
        "billingPeriodName": "Kỳ thu tháng 11/2026",
        "amountPaid": 420000
      },
      {
        "receivableId": 102,
        "feeName": "Phí dịch vụ vệ sinh",
        "billingPeriodName": "Kỳ thu tháng 11/2026",
        "amountPaid": 200000
      }
    ]
  }
}
```

---

### 5.3 US-16: Theo dõi Danh sách Hộ đã nộp, Chưa nộp và Nợ đọng (Ban quản trị)

#### 1. `GET /billing-periods/{id}/household-debts`
Theo dõi tình trạng hoàn thành nộp phí của từng hộ gia đình trong một kỳ thu cụ thể.
- **Quyền**: `BQT`, `ACCOUNTANT` (`RESIDENT` bị chặn -> `403 FORBIDDEN` - AC4).
- **Path Parameter**: `id` (*Long, ID của kỳ thu `billing_period`*).
- **Query Parameters**:
  - `status` (*optional, Enum*): `ALL`, `PAID` (đã đóng đủ), `PARTIALLY_PAID` (đóng một phần), `UNPAID` (chưa đóng đồng nào), `OVERDUE` (còn nợ và đã quá hạn).
  - `keyword` (*optional, String*): Tìm theo `householdCode`, `apartmentNo`, `headFullName`.
  - `page` (*optional, Integer*, mặc định: 1).
  - `size` (*optional, Integer*, mặc định: 20, max: 100).
- **Nghiệp vụ quan trọng (AC3, AC5)**:
  - Nếu kỳ thu chưa lập danh sách (`generatedAt == null`) -> trả về lỗi `422 PERIOD_NOT_GENERATED` ("Kỳ thu chưa được lập danh sách khoản phải thu") (AC3).
  - **Khoản thu tự nguyện (`fee_kind = 'VOLUNTARY'`) tuyệt đối KHÔNG được tính vào công nợ hay làm cho hộ bị xem là "chưa nộp / còn nợ"** (AC5).
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy danh sách công nợ hộ gia đình thành công",
  "data": {
    "content": [
      {
        "householdId": 1,
        "householdCode": "H001",
        "apartmentNo": "A101",
        "headFullName": "Nguyễn Văn An",
        "phone": "0901234567",
        "totalDue": 420000,
        "totalPaid": 420000,
        "outstandingAmount": 0,
        "status": "PAID",
        "receivablesCount": 1,
        "earliestDueDate": "2026-11-25",
        "isOverdue": false
      },
      {
        "householdId": 2,
        "householdCode": "H002",
        "apartmentNo": "A102",
        "headFullName": "Lê Văn Bình",
        "phone": "0912345678",
        "totalDue": 850000,
        "totalPaid": 300000,
        "outstandingAmount": 550000,
        "status": "PARTIALLY_PAID",
        "receivablesCount": 2,
        "earliestDueDate": "2026-11-25",
        "isOverdue": true
      },
      {
        "householdId": 3,
        "householdCode": "H003",
        "apartmentNo": "A103",
        "headFullName": "Phạm Thị Cúc",
        "phone": "0988776655",
        "totalDue": 600000,
        "totalPaid": 0,
        "outstandingAmount": 600000,
        "status": "UNPAID",
        "receivablesCount": 1,
        "earliestDueDate": "2026-11-25",
        "isOverdue": true
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1
  }
}
```

#### 2. `GET /billing-periods/{id}/debt-summary`
Bảng số liệu tổng hợp nhanh tình hình thu nộp của cả kỳ.
- **Quyền**: `BQT`, `ACCOUNTANT`
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy tổng quan tình hình thu phí thành công",
  "data": {
    "billingPeriodId": 5,
    "billingPeriodCode": "KY_2026_11",
    "billingPeriodName": "Kỳ thu tháng 11/2026",
    "totalHouseholds": 120,
    "paidHouseholdsCount": 85,
    "partiallyPaidHouseholdsCount": 15,
    "unpaidHouseholdsCount": 20,
    "overdueHouseholdsCount": 25,
    "totalAmountDue": 85000000,
    "totalAmountPaid": 68000000,
    "totalOutstandingAmount": 17000000,
    "completionRate": 80.0
  }
}
```

#### 3. `GET /households/{id}/debt-details`
Xem chi tiết tất cả các khoản phải thu, số tiền đã trả, còn nợ của một hộ cụ thể (qua mọi kỳ thu hoặc lọc theo kỳ).
- **Quyền**: `BQT`, `ACCOUNTANT`
- **Query Parameters**: `billingPeriodId` (optional), `status` (optional).
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy chi tiết công nợ hộ gia đình thành công",
  "data": {
    "householdId": 2,
    "householdCode": "H002",
    "apartmentNo": "A102",
    "headFullName": "Lê Văn Bình",
    "totalOutstanding": 550000,
    "items": [
      {
        "receivableId": 105,
        "billingPeriodCode": "KY_2026_11",
        "billingPeriodName": "Kỳ thu tháng 11/2026",
        "feeCategoryName": "Phí quản lý",
        "amountDue": 500000,
        "paidAmount": 300000,
        "outstandingAmount": 200000,
        "dueDate": "2026-11-25",
        "status": "PARTIALLY_PAID",
        "isOverdue": true,
        "version": 1
      },
      {
        "receivableId": 106,
        "billingPeriodCode": "KY_2026_11",
        "billingPeriodName": "Kỳ thu tháng 11/2026",
        "feeCategoryName": "Phí gửi xe",
        "amountDue": 350000,
        "paidAmount": 0,
        "outstandingAmount": 350000,
        "dueDate": "2026-11-25",
        "status": "UNPAID",
        "isOverdue": true,
        "version": 0
      }
    ]
  }
}
```

---

### 5.4 US-17: Cư dân Tra cứu Công nợ & Lịch sử Nộp phí của Hộ (Cư dân)

#### 1. `GET /me/receivables`
Cư dân xem danh sách các khoản phải thu, đã thu và còn nợ của chính hộ gia đình mình.
- **Quyền**: `RESIDENT` (Bắt buộc tài khoản đã liên kết với nhân khẩu thuộc hộ gia đình hợp lệ).
- **Bảo mật (AC4)**: Endpoint tự động lấy `householdId` từ token của cư dân đang đăng nhập, **tuyệt đối không cho phép cư dân xem dữ liệu của hộ khác**.
- **Query Parameters**:
  - `status` (*optional, Enum*): `ALL`, `PAID`, `UNPAID`, `PARTIALLY_PAID`, `OVERDUE` (mặc định: `ALL`).
  - `billingPeriodId` (*optional, Long*): Lọc theo kỳ thu.
  - `page` (*optional, Integer*, mặc định: 1).
  - `size` (*optional, Integer*, mặc định: 20).
- **Hệ thống xử lý (AC1, AC2, AC3, AC5)**:
  - Nếu không có khoản nào khớp bộ lọc -> trả về `content: []`, `totalElements: 0` (AC3).
  - Công thức công nợ: `outstandingAmount = amountDue - paidAmount` đảm bảo chính xác 100% (AC5).
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy danh sách khoản thu của hộ thành công",
  "data": {
    "content": [
      {
        "receivableId": 105,
        "billingPeriodId": 5,
        "billingPeriodName": "Kỳ thu tháng 11/2026",
        "feeName": "Phí quản lý vận hành",
        "feeKind": "MANDATORY",
        "calculationMethod": "PER_AREA",
        "quantity": 80.0,
        "unitPriceApplied": 10000,
        "amountDue": 800000,
        "paidAmount": 400000,
        "outstandingAmount": 400000,
        "dueDate": "2026-11-25",
        "status": "PARTIALLY_PAID",
        "isOverdue": false
      },
      {
        "receivableId": 106,
        "billingPeriodId": 5,
        "billingPeriodName": "Kỳ thu tháng 11/2026",
        "feeName": "Phí dịch vụ vệ sinh",
        "feeKind": "MANDATORY",
        "calculationMethod": "FIXED",
        "quantity": null,
        "unitPriceApplied": 50000,
        "amountDue": 50000,
        "paidAmount": 50000,
        "outstandingAmount": 0,
        "dueDate": "2026-11-25",
        "status": "PAID",
        "isOverdue": false
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

#### 2. `GET /me/receivables/summary`
Tóm tắt tổng nghĩa vụ tài chính hiện tại của hộ cư dân.
- **Quyền**: `RESIDENT`
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy thông tin tóm tắt công nợ thành công",
  "data": {
    "householdCode": "H002",
    "apartmentNo": "A102",
    "totalCurrentPeriodDue": 850000,
    "totalCurrentPeriodPaid": 450000,
    "currentPeriodOutstanding": 400000,
    "totalOverdueDebt": 0,
    "totalPayableAmount": 400000
  }
}
```

#### 3. `GET /me/payments`
Cư dân xem lịch sử các lần nộp phí (phiếu thu) của hộ mình.
- **Quyền**: `RESIDENT`
- **Query Parameters**: `page` (1), `size` (20), `year` (optional).
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy lịch sử thanh toán thành công",
  "data": {
    "content": [
      {
        "receiptId": 501,
        "receiptCode": "PT-202611-0001",
        "paymentDate": "2026-11-23",
        "totalAmount": 450000,
        "paymentMethod": "CASH",
        "note": "Cư dân nộp tiền mặt tại quầy",
        "collectorName": "Trần Thị Kế Toán",
        "details": [
          {
            "feeName": "Phí quản lý vận hành",
            "amountPaid": 400000
          },
          {
            "feeName": "Phí dịch vụ vệ sinh",
            "amountPaid": 50000
          }
        ]
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### 5.5 US-18: Quản lý Các đợt Vận động Đóng góp Tự nguyện (Ban quản trị)

#### 1. `POST /donation-campaigns`
Ban quản trị tạo mới một đợt vận động đóng góp tự nguyện (tách riêng biệt khỏi khoản bắt buộc).
- **Quyền**: `BQT`
- **Request Body (`CreateDonationCampaignRequest`)**:
```json
{
  "code": "QUY_VI_NGUOI_NGHEO_2026",
  "title": "Đợt vận động Quỹ Vì Người Nghèo 2026",
  "description": "Chung tay ủng hộ các gia đình có hoàn cảnh khó khăn tại địa phương",
  "targetAmount": 50000000,
  "startDate": "2026-11-20",
  "endDate": "2026-12-20"
}
```
- **Validation rules (AC1, AC3)**:
  - `code`: `@NotBlank`, 2..30 ký tự, định dạng `^[A-Za-z0-9_]+$`.
  - `title`: `@NotBlank`, 3..200 ký tự.
  - `targetAmount`: optional, nếu có phải >= 0.
  - `startDate`: `@NotNull`.
  - `endDate`: `@NotNull`, bắt buộc `endDate >= startDate`. Nếu `endDate < startDate` -> trả `400 VALIDATION_ERROR` (`errors.endDate = "Ngày kết thúc không được sớm hơn ngày bắt đầu"`).
- **Response 201 Created (`DonationCampaignResponse`)**:
```json
{
  "success": true,
  "message": "Tạo đợt vận động thành công",
  "data": {
    "id": 10,
    "code": "QUY_VI_NGUOI_NGHEO_2026",
    "title": "Đợt vận động Quỹ Vì Người Nghèo 2026",
    "description": "Chung tay ủng hộ các gia đình có hoàn cảnh khó khăn tại địa phương",
    "targetAmount": 50000000,
    "totalCollected": 0,
    "donorCount": 0,
    "startDate": "2026-11-20",
    "endDate": "2026-12-20",
    "status": "OPEN",
    "createdAt": "2026-11-23T09:00:00Z"
  }
}
```
- **Lỗi thường gặp (AC3)**:
  - `400 VALIDATION_ERROR`: Để trống tên quỹ hoặc `endDate < startDate`.
  - `409 DUPLICATE`: Trùng mã đợt vận động `code`.

#### 2. `GET /donation-campaigns`
Danh sách các đợt vận động quyên góp.
- **Quyền**: `BQT`, `ACCOUNTANT`, `RESIDENT`
- **Query Parameters**:
  - `status` (*optional, Enum*): `OPEN`, `CLOSED`, `ALL` (mặc định: `ALL`).
  - `keyword` (*optional, String*): Tìm theo tên hoặc mã đợt.
  - `page` (*optional, Integer*, mặc định: 1).
  - `size` (*optional, Integer*, mặc định: 20).
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy danh sách đợt vận động thành công",
  "data": {
    "content": [
      {
        "id": 10,
        "code": "QUY_VI_NGUOI_NGHEO_2026",
        "title": "Đợt vận động Quỹ Vì Người Nghèo 2026",
        "targetAmount": 50000000,
        "totalCollected": 12500000,
        "donorCount": 25,
        "startDate": "2026-11-20",
        "endDate": "2026-12-20",
        "status": "OPEN"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### 3. `GET /donation-campaigns/{id}`
Xem chi tiết một đợt vận động, tỷ lệ hoàn thành mục tiêu và tổng kết đóng góp.
- **Quyền**: `BQT`, `ACCOUNTANT`, `RESIDENT`
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy chi tiết đợt vận động thành công",
  "data": {
    "id": 10,
    "code": "QUY_VI_NGUOI_NGHEO_2026",
    "title": "Đợt vận động Quỹ Vì Người Nghèo 2026",
    "description": "Chung tay ủng hộ các gia đình có hoàn cảnh khó khăn tại địa phương",
    "targetAmount": 50000000,
    "totalCollected": 12500000,
    "donorCount": 25,
    "completionRate": 25.0,
    "startDate": "2026-11-20",
    "endDate": "2026-12-20",
    "status": "OPEN",
    "createdByName": "Nguyễn Văn BQT",
    "createdAt": "2026-11-23T09:00:00Z"
  }
}
```

#### 4. `PUT /donation-campaigns/{id}`
Cập nhật thông tin đợt vận động (khi đợt chưa kết thúc).
- **Quyền**: `BQT`
- **Request Body**: Tương tự như `POST /donation-campaigns` (không cho phép sửa `code`).

#### 5. `PATCH /donation-campaigns/{id}/status`
Đóng sớm hoặc mở lại đợt vận động.
- **Quyền**: `BQT`
- **Request Body**: `{ "status": "OPEN" | "CLOSED" }`
- **Response 200 OK**: Trả về `DonationCampaignResponse` cập nhật.

#### 6. `POST /donation-campaigns/{id}/donations`
Ghi nhận một khoản đóng góp tự nguyện từ hộ gia đình hoặc cư dân / nhà hảo tâm.
- **Quyền**: `BQT`, `ACCOUNTANT`
- **Path Parameter**: `id` (*Long, ID của đợt vận động*).
- **Request Body (`RecordDonationRequest`)**:
```json
{
  "householdId": 1,
  "residentId": 5,
  "donorName": "Hộ gia đình ông Nguyễn Văn An",
  "amount": 500000,
  "donationDate": "2026-11-23",
  "note": "Ủng hộ đồng bào"
}
```
- **Validation rules (AC2, AC3, AC4, AC5)**:
  - `donorName`: `@NotBlank`, tối đa 150 ký tự.
  - `amount`: `@NotNull`, `@Positive` (> 0). Nếu `amount <= 0` -> `400 VALIDATION_ERROR`.
  - `donationDate`: `@NotNull`, `@PastOrPresent`.
  - `householdId`: Optional (nếu là hộ trong tòa nhà).
  - `residentId`: Optional.
- **Nghiệp vụ cốt lõi (AC4, AC5)**:
  - Kiểm tra trạng thái đợt vận động: Nếu `campaign.status == 'CLOSED'` hoặc ngày hiện tại > `campaign.endDate` -> từ chối `422 CAMPAIGN_CLOSED` ("Đợt vận động đã đóng, không tiếp nhận thêm đóng góp") (AC4).
  - Khoản đóng góp tự nguyện được lưu độc lập vào bảng `donation_record`. **Hộ gia đình không tham gia đóng góp thì không bị phát sinh bất kỳ khoản nợ nào** (AC5).
- **Response 201 Created (`DonationRecordResponse`)**:
```json
{
  "success": true,
  "message": "Ghi nhận đóng góp tự nguyện thành công",
  "data": {
    "id": 1001,
    "campaignId": 10,
    "campaignTitle": "Đợt vận động Quỹ Vì Người Nghèo 2026",
    "householdId": 1,
    "householdCode": "H001",
    "apartmentNo": "A101",
    "donorName": "Hộ gia đình ông Nguyễn Văn An",
    "amount": 500000,
    "donationDate": "2026-11-23",
    "note": "Ủng hộ đồng bào",
    "recordedBy": {
      "id": 2,
      "fullName": "Trần Thị Kế Toán"
    },
    "recordedAt": "2026-11-23T09:15:00Z"
  }
}
```

#### 7. `GET /donation-campaigns/{id}/donations`
Xem danh sách các khoản đã đóng góp trong một đợt vận động.
- **Quyền**: `BQT`, `ACCOUNTANT`
- **Query Parameters**: `householdId` (optional), `page` (1), `size` (20), `keyword` (tìm theo tên người ủng hộ).
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy danh sách các khoản đóng góp thành công",
  "data": {
    "content": [
      {
        "id": 1001,
        "donorName": "Hộ gia đình ông Nguyễn Văn An",
        "householdCode": "H001",
        "apartmentNo": "A101",
        "amount": 500000,
        "donationDate": "2026-11-23",
        "note": "Ủng hộ đồng bào",
        "recordedByName": "Trần Thị Kế Toán"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### 8. `GET /me/donations`
Cư dân xem danh sách các khoản mà hộ gia đình mình đã đóng góp tự nguyện.
- **Quyền**: `RESIDENT`
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy lịch sử đóng góp của hộ thành công",
  "data": {
    "content": [
      {
        "id": 1001,
        "campaignId": 10,
        "campaignTitle": "Đợt vận động Quỹ Vì Người Nghèo 2026",
        "amount": 500000,
        "donationDate": "2026-11-23",
        "note": "Ủng hộ đồng bào"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 6. HƯỚNG DẪN TÍCH HỢP CHO FRONTEND VÀ BACKEND

### 6.1 Dành cho Backend Developer
1. **Controller Mapping**:
   - `US-07`: Tạo `AuthorityController.java` với tiền tố `@RequestMapping("/authority")`.
   - `US-15`: Tạo `PaymentController.java` với tiền tố `@RequestMapping("/payments")`.
   - `US-16`: Mở rộng `BillingPeriodController.java` (hoặc tạo `DebtTrackingController.java`) cho các route `/billing-periods/{id}/household-debts`, `/billing-periods/{id}/debt-summary`, và `/households/{id}/debt-details`.
   - `US-17`: Bổ sung vào `MeController.java` các method mapping tới `/me/receivables`, `/me/receivables/summary`, `/me/payments`.
   - `US-18`: Tạo `DonationCampaignController.java` với tiền tố `@RequestMapping("/donation-campaigns")` và endpoint `/me/donations` trong `MeController.java`.
2. **Security Config**:
   - Khai báo phân quyền chính xác trong `SecurityConfig.java` theo đúng ma trận ở Mục 4.
3. **Optimistic Locking**:
   - Trong `Receivable.java`, đảm bảo dùng `@Version private Long version;`. Khi cập nhật khoản thu ở `US-15`, đối chiếu `expectedVersion` và bắt `ObjectOptimisticLockingFailureException` để quăng `VersionConflictException` (`409 VERSION_CONFLICT`).
4. **Exception Handling**:
   - Đăng ký các Handler mới trong `GlobalExceptionHandler.java`:
     - `VersionConflictException` -> HTTP 409 `VERSION_CONFLICT`.
     - `CampaignClosedException` -> HTTP 422 `CAMPAIGN_CLOSED`.
     - `InvalidPaymentAmountException` -> HTTP 422 `INVALID_PAYMENT_AMOUNT`.
     - `ReceivableAlreadyPaidException` -> HTTP 409 `RECEIVABLE_ALREADY_PAID`.

### 6.2 Dành cho Frontend Developer
1. **File `http.js`**:
   - Thêm các mã lỗi mới vào từ điển `messages` trong `frontend2/src/api/http.js`:
     ```javascript
     VERSION_CONFLICT: "Dữ liệu khoản thu đã bị thay đổi bởi người khác. Vui lòng tải lại trang.",
     RECEIVABLE_ALREADY_PAID: "Khoản này đã được nộp đủ trước đó.",
     PERIOD_CLOSED: "Kỳ thu đã đóng sổ, không thể thu thêm.",
     PERIOD_NOT_GENERATED: "Kỳ thu chưa được lập danh sách khoản thu.",
     CAMPAIGN_CLOSED: "Đợt vận động đã kết thúc, không thể tiếp nhận đóng góp.",
     INVALID_PAYMENT_AMOUNT: "Số tiền thu không hợp lệ hoặc lớn hơn dư nợ còn lại."
     ```
2. **File `services.js`**:
   - Định nghĩa các service gọi API tương ứng:
     ```javascript
     export const authorityApi = {
       verify: (params) => api.get("/authority/residence/verify", params),
       getHousehold: (id) => api.get(`/authority/households/${id}`),
       getResident: (id) => api.get(`/authority/residents/${id}`),
     };

     export const paymentApi = {
       payCash: (data) => api.post("/payments/cash", data),
       getReceipt: (id) => api.get(`/payments/receipts/${id}`),
       getReceipts: (params) => api.get("/payments/receipts", params),
     };

     export const debtApi = {
       getHouseholdDebts: (periodId, params) => api.get(`/billing-periods/${periodId}/household-debts`, params),
       getDebtSummary: (periodId) => api.get(`/billing-periods/${periodId}/debt-summary`),
       getHouseholdDebtDetails: (householdId, params) => api.get(`/households/${householdId}/debt-details`, params),
     };

     export const meDebtApi = {
       receivables: (params) => api.get("/me/receivables", params),
       summary: () => api.get("/me/receivables/summary"),
       payments: (params) => api.get("/me/payments", params),
       donations: (params) => api.get("/me/donations", params),
     };

     export const donationApi = {
       list: (params) => api.get("/donation-campaigns", params),
       get: (id) => api.get(`/donation-campaigns/${id}`),
       create: (data) => api.post("/donation-campaigns", data),
       update: (id, data) => api.put(`/donation-campaigns/${id}`, data),
       updateStatus: (id, status) => api.patch(`/donation-campaigns/${id}/status`, { status }),
       recordDonation: (id, data) => api.post(`/donation-campaigns/${id}/donations`, data),
       getDonations: (id, params) => api.get(`/donation-campaigns/${id}/donations`, params),
     };
     ```
