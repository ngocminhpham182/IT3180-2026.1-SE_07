# API Contract Tổng Hợp sprint 1

## 1. Quy ước dùng chung

| Chủ đề | Quy ước thực tế ở Backend |
| --- | --- |
| **Xác thực** | `Authorization: Bearer <accessToken>` cho mọi endpoint trừ `POST /auth/login`. JWT có thời hạn mặc định `28800` giây (8 giờ). Mỗi request filter đọc lại DB kiểm tra `user.active`, `passwordChangedAt`, và `roles` mới nhất. |
| **Response thành công** | `{ "success": true, "message": "...", "data": ... }`. `204 No Content` không có body. |
| **Phân trang** | `data = { content, page, size, totalElements, totalPages }`. `page` bắt đầu từ `1`, `size` mặc định `20` (tối đa `100` ở các API có kiểm tra). |
| **Response lỗi** | Chuẩn chung: `{ "success": false, "code": "...", "message": "...", "errors"?: {...}, "details"?: {...} }`. **Lưu ý ngoại lệ** ở mục 0 (#2) đối với 4 exception của `FeeCategory` và `BillingPeriod`. |
| **Ngày & Thời gian** | Ngày: `yyyy-MM-dd` (`LocalDate`). Thời điểm: ISO-8601 UTC (`Instant`). Múi giờ nghiệp vụ "hôm nay": `Asia/Ho_Chi_Minh` (riêng `UpdateResidentRequest.isDateOfBirthInRange` dùng `LocalDate.now()` JVM). |
| **Dữ liệu số** | Diện tích: `BigDecimal` tối đa 2 chữ số thập phân. Đơn giá `unitPrice`: tối đa 2 chữ số thập phân (`NUMERIC(12,2)`). Thành tiền: làm tròn `HALF_UP` về số nguyên VND. |

---

## 2. Enum & Schema dùng chung

### 2.1 Enum

| Enum | Giá trị hợp lệ |
| --- | --- |
| `RoleCode` | `BQT`, `ACCOUNTANT`, `RESIDENT`, `AUTHORITY` |
| `Gender` | `MALE`, `FEMALE` |
| `HouseholdStatus` | `ACTIVE`, `INACTIVE`, `ALL` (chỉ dùng cho filter `GET /households`) |
| `ResidencyStatus` | `RESIDING`, `TEMPORARILY_ABSENT`, `TEMPORARY_RESIDENCE`, `MOVED_OUT`, `DECEASED` |
| `ResidentEntryType` | `MOVE_IN`, `BIRTH`, `TEMPORARY_RESIDENCE`, `EXISTING` |
| `MovementType` | `MOVE_IN`, `MOVE_OUT`, `TRANSFER`, `BIRTH`, `DEATH`, `TEMPORARY_ABSENCE`, `TEMPORARY_RESIDENCE`, `RETURN` |
| `SearchType` | `ALL`, `HOUSEHOLD`, `RESIDENT` |
| `ResidentRequestType` | `PROFILE_UPDATE`, `TEMPORARY_ABSENCE`, `TEMPORARY_RESIDENCE` |
| `ResidentRequestStatus` | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `FeeKind` | `MANDATORY`, `VOLUNTARY` |
| `CalculationMethod` | `PER_AREA`, `FIXED`, `MANUAL` |
| `BillingCycle` | `MONTHLY`, `ONE_TIME` |
| `BillingPeriodStatus` | `OPEN`, `CLOSED` |

### 2.2 Các DTO Response chính

#### `UserResponse`
```json
{
  "id": 3,
  "username": "kt01",
  "fullName": "Trần Thị Kế Toán",
  "email": "kt01@example.com",
  "phone": "0901234567",
  "residentId": null,
  "householdId": null,
  "active": true,
  "roles": ["ACCOUNTANT"],
  "mustChangePassword": true
}
```

#### `HouseholdResponse`
```json
{
  "id": 1,
  "householdCode": "H001",
  "apartmentNo": "A101",
  "floor": 1,
  "area": 75.5,
  "status": "ACTIVE",
  "createdAt": "2026-10-01T02:00:00Z",
  "updatedAt": "2026-10-01T02:00:00Z",
  "head": {
    "id": 1,
    "fullName": "Nguyễn Văn An",
    "dateOfBirth": "1980-01-01",
    "gender": "MALE",
    "identityNumber": "012345678901",
    "phone": "0901234567",
    "isHead": true,
    "residencyStatus": "RESIDING"
  },
  "residentCount": 1,
  "presentCount": 1
}
```

#### `HouseholdDetailResponse` (dùng cho `GET /households/{id}`)
```json
{
  "id": 1,
  "householdCode": "H001",
  "apartmentNo": "A101",
  "floor": 1,
  "area": 75.5,
  "status": "ACTIVE",
  "createdAt": "2026-10-01T02:00:00Z",
  "updatedAt": "2026-10-01T02:00:00Z",
  "residents": [ /* ResidentInHouseholdResponse[] */ ]
}
```

#### `ResidentInHouseholdResponse`
```json
{
  "id": 5,
  "fullName": "Nguyễn Văn A",
  "dateOfBirth": "2000-01-01",
  "gender": "MALE",
  "identityNumber": "012345678901",
  "phone": "0901234567",
  "relationshipToHead": "Con",
  "isHead": false,
  "residencyStatus": "RESIDING",
  "statusUntil": null,
  "statusExpired": false,
  "moveInDate": "2024-01-01",
  "moveOutDate": null,
  "createdAt": "2026-10-01T02:00:00Z",
  "updatedAt": "2026-10-01T02:00:00Z"
}
```

#### `ResidentResponse`
```json
{
  "id": 5,
  "householdId": 1,
  "householdCode": "H001",
  "apartmentNo": "A101",
  "fullName": "Nguyễn Văn A",
  "dateOfBirth": "2000-01-01",
  "gender": "MALE",
  "identityNumber": "012345678901",
  "phone": "0901234567",
  "relationshipToHead": "Con",
  "isHead": false,
  "residencyStatus": "TEMPORARILY_ABSENT",
  "statusUntil": "2026-12-20",
  "statusExpired": false,
  "moveInDate": "2024-01-01",
  "moveOutDate": null,
  "createdAt": "2026-10-01T02:00:00Z",
  "updatedAt": "2026-10-20T03:00:00Z"
}
```

#### `MovementResponse`
```json
{
  "id": 12,
  "residentId": 5,
  "movementType": "TEMPORARY_ABSENCE",
  "effectiveDate": "2026-10-20",
  "startDate": "2026-10-20",
  "endDate": "2026-12-20",
  "oldHousehold": null,
  "newHousehold": null,
  "note": "Đi công tác nước ngoài",
  "isInitial": false,
  "recordedBy": { "id": 1, "fullName": "Nguyễn Văn B" },
  "recordedAt": "2026-10-20T03:00:00Z"
}
```

#### `ResidentRequestResponse`
```json
{
  "id": 31,
  "type": "PROFILE_UPDATE",
  "status": "PENDING",
  "household": { "id": 1, "householdCode": "H001", "apartmentNo": "A101" },
  "resident": { "id": 5, "fullName": "Nguyễn Văn A" },
  "submittedBy": { "id": 7, "fullName": "Nguyễn Văn An" },
  "submittedAt": "2026-11-02T03:00:00Z",
  "note": "Sai họ tên trên hồ sơ",
  "proposed": { "...": "..." },
  "before": { "...": "..." },
  "changes": ["fullName"],
  "decision": null,
  "result": null
}
```

#### `FeeCategoryResponse`
```json
{
  "id": 2,
  "code": "MANAGEMENT",
  "name": "Phí quản lý",
  "description": "Thu hằng tháng theo diện tích",
  "feeKind": "MANDATORY",
  "calculationMethod": "PER_AREA",
  "unitPrice": 7000,
  "billingCycle": "MONTHLY",
  "dueDay": 10,
  "active": true,
  "createdAt": "2026-10-01T02:00:00Z",
  "updatedAt": "2026-10-01T02:00:00Z"
}
```

#### `BillingPeriodResponse` & `BillingPeriodDetailResponse`
- `BillingPeriodResponse`: `{ id, code, name, startDate, endDate, status, generatedAt, closedAt }`.
- `BillingPeriodDetailResponse`: bổ sung `generated` (boolean), `generatedBy` (`{ id, fullName }`), `closedBy` (`{ id, fullName }`), `summary` (`{ receivableCount, householdCount, totalDue }`).

---

## 3. Bảng mã lỗi tổng hợp (`GlobalExceptionHandler` & Security)

| HTTP | `code` | Khi nào | Cấu trúc payload bổ sung |
| --- | --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Sai dữ liệu đầu vào / JSON không đọc được | `errors` ở top-level (trừ `FeeCategoryValidationException` đặt ở `details.errors`, và `InvalidBillingPeriodDateRangeException` đặt `details.endDate`) |
| 400 | *(không có `code`)* | Sai query `status` / `page` / `size` ở `ResidentService` (`ResponseStatusException`) | `ApiResponse(false, message, null)` |
| 401 | `UNAUTHORIZED` | Thiếu header `Authorization: Bearer ...` | Chỉ `success`, `code`, `message` |
| 401 | `TOKEN_INVALID` | JWT sai/hết hạn, tài khoản bị khóa (`!active`), hoặc token cấp trước `passwordChangedAt` | Chỉ `success`, `code`, `message` |
| 401 | `INVALID_CREDENTIALS` | Sai username, sai mật khẩu, hoặc tài khoản bị vô hiệu hóa khi login | Chỉ `success`, `code`, `message` |
| 403 | `FORBIDDEN` | Không đủ vai trò theo `SecurityConfig` | Chỉ `success`, `code`, `message` |
| 403 | `RESIDENT_NOT_IN_HOUSEHOLD` | Cư dân gửi đề nghị cho nhân khẩu không thuộc hộ mình | Chỉ `success`, `code`, `message` |
| 404 | `NOT_FOUND` | Không tìm thấy tài nguyên | Chỉ `success`, `code`, `message` |
| 409 | `DUPLICATE` | Trùng dữ liệu duy nhất | `errors` ở top-level (riêng `FeeCategoryDuplicateException` đặt ở `details.errors`) |
| 409 | `HAS_HISTORY` | Xóa nhân khẩu đã có biến động (`isInitial = false`) | `details: { movementCount, suggestedAction: "RECORD_MOVEMENT" }` |
| 409 | `HAS_ACCOUNT` | Xóa nhân khẩu đang liên kết `app_user` | `details: { residentId }` |
| 409 | `HAS_TRANSACTIONS` | Đổi field khóa của `FeeCategory` khi đã có `receivable` | `details: { errors, suggestedAction: "CREATE_NEW" }` |
| 409 | `ALREADY_GENERATED` | Sửa/xóa/lập lại kỳ thu đã lập danh sách | `details: { generatedAt }` |
| 409 | `PENDING_REQUEST_EXISTS` | Đã có đề nghị chờ duyệt cùng loại | `details: { requestId }` (nếu tìm thấy) |
| 409 | `REQUEST_NOT_PENDING` | Duyệt/từ chối/hủy đề nghị không ở `PENDING` | `details: { status }` |
| 409 | `REQUEST_OUTDATED` | Dữ liệu nhân khẩu đã đổi hoặc nhân khẩu bị xóa trước khi duyệt | `details: { fields }` |
| 422 | `SELF_LOCKOUT` | BQT tự khóa hoặc tự bỏ vai trò `BQT` của chính mình | Chỉ `success`, `code`, `message` |
| 422 | `SELF_RESET_NOT_ALLOWED` | BQT tự đặt lại mật khẩu của chính mình | Chỉ `success`, `code`, `message` |
| 422 | `RESIDENT_LINK_REQUIRED` | Tài khoản có role `RESIDENT` nhưng chưa gắn `residentId` | Chỉ `success`, `code`, `message` |
| 422 | `HOUSEHOLD_INACTIVE` | Thao tác trên hộ `INACTIVE` | `details: null` |
| 422 | `INVALID_NEW_HEAD` | Chủ hộ mới không hợp lệ hoặc xóa nhân khẩu đang là chủ hộ | `details: { reason }` (`NOT_FOUND`, `NOT_IN_HOUSEHOLD`, `NOT_RESIDING`, `ALREADY_HEAD`, `SAME_PERSON`) |
| 422 | `HEAD_REQUIRED` | Chủ hộ rời hộ nhưng hộ còn ứng viên mà chưa truyền `newHeadResidentId` | `details: { candidates: [{ id, fullName, relationshipToHead }] }` |
| 422 | `RESIDENT_NOT_RESIDING` | Nhân khẩu đang `MOVED_OUT` hoặc `DECEASED` | `details: { residencyStatus }` |
| 422 | `INVALID_TRANSITION` | Cặp `(movementType, currentStatus)` hoặc trạng thái đề nghị không hợp lệ | `details: { currentStatus, allowedFrom }` |
| 422 | `NO_CHANGES` | Đề nghị `PROFILE_UPDATE` không có trường nào thay đổi | Chỉ `success`, `code`, `message` |
| 422 | `MISSING_AREA` | Khoản `PER_AREA` nhưng hộ chưa có `area` | `details: { householdId }` |
| 422 | `FEE_INACTIVE` | Khoản thu đã ngừng hoạt động | `details: null` |
| 422 | `FEE_NOT_BILLABLE` | Khoản `VOLUNTARY` hoặc `MANUAL` không tính phí tự động | `details: null` |
| 422 | `PERIOD_OVERLAP` | Khoảng ngày kỳ thu chồng lấn kỳ khác | `details: { overlapping: { id, code } }` |
| 422 | `PERIOD_CLOSED` | Kỳ thu đã `CLOSED` | `details: null` |
| 422 | `NO_ACTIVE_FEE` | Không có khoản bắt buộc nào đủ điều kiện lập danh sách | `details: null` |
| 422 | `NOTHING_TO_GENERATE` | Không sinh được khoản phải thu nào (ví dụ mọi hộ thiếu `area`) | `details: { skipped }` |
| 422 | `PERIOD_NOT_GENERATED` | Đóng kỳ thu khi chưa lập danh sách | `details: null` |

---

## 4. Ma trận phân quyền thực tế (`SecurityConfig.java`)

| Nhóm Endpoint | Công khai | `BQT` | `ACCOUNTANT` | `RESIDENT` | `AUTHORITY` |
| --- | :---: | :---: | :---: | :---: | :---: |
| `POST /auth/login` | có | có | có | có | có |
| `POST /auth/logout`, `/me`, `/me/password` | không | có | có | có | có |
| `GET /me/household`, `/me/resident-requests/**` | không | không | không | có | không |
| `/users`, `/users/**`, `/roles`, `/roles/**` | không | có | không | không | không |
| `/resident-requests`, `/resident-requests/**` | không | có | không | không | không |
| `GET /households/**`, `GET /residents/**`, `GET /residents/*/movements`, `GET /search` | không | có | có | không | không |
| Ghi `/households/**`, `/residents/**`, `POST /residents/*/movements` | không | có | không | không | không |
| `/fee-categories/**`, `/fee-calculation/preview` | không | có | có | không | không |
| `POST/GET/PUT/DELETE /billing-periods/**`, `POST /billing-periods/*/generate` | không | có | có | không | không |
| `PATCH /billing-periods/*/status` | không | có | không | không | không |

---

## 5. Đặc tả chi tiết các Endpoint theo User Story

### 5.1 US-08: Xác thực (`AuthController`)

1. **`POST /auth/login`** (Công khai)
   - Body: `{ "username": String (@NotBlank, max 100), "password": String (@NotBlank, max 128) }`
   - `200 OK`: `LoginResponse { accessToken, tokenType: "Bearer", expiresIn: 28800, expiresAt, user: { id, username, fullName, roles, mustChangePassword } }`
   - Lỗi: `400 VALIDATION_ERROR`, `401 INVALID_CREDENTIALS`.
2. **`POST /auth/logout`** (Authenticated)
   - Không body. Trả `204 No Content`.

### 5.2 US-09: Quản lý tài khoản & vai trò (`UserController`, `RoleController`)

Quyền: `BQT`.

1. **`GET /roles`**: `200 OK`, `data` là mảng `RoleResponse { code, name }` (4 vai trò).
2. **`POST /users`**:
   - Body (`CreateUserRequest`): `username` (`@NotBlank`, max 100), `password` (`@NotBlank`, 6..128, khác `username`), `fullName` (`@NotBlank`, max 150), `email` (max 255, optional), `phone` (max 20, optional), `residentId` (bắt buộc khi `roles` chứa `RESIDENT`; nhân khẩu phải đang `RESIDING`), `roles` (`@NotEmpty Set<RoleCode>`).
   - `201 Created`: trả `UserResponse` (`active = true`, `mustChangePassword = true`).
   - Lỗi: `400 VALIDATION_ERROR`, `409 DUPLICATE` (`username`, `email`, `residentId`), `422 RESIDENT_LINK_REQUIRED`.
3. **`GET /users`**: Query `keyword`, `role`, `active`, `page` (1), `size` (20). Sắp xếp `username` ASC. Trả `PageResponse<UserResponse>`.
4. **`GET /users/{id}`**: Trả `UserResponse` hoặc `404 NOT_FOUND`.
5. **`PUT /users/{id}/roles`**: Body `{ "roles": List<RoleCode> (@NotNull, @NotEmpty, không trùng) }`. Trả `200` `UserResponse`. Lỗi: `404`, `422 RESIDENT_LINK_REQUIRED`, `422 SELF_LOCKOUT`.
6. **`PATCH /users/{id}/status`**: Body `{ "active": Boolean (@NotNull) }`. Trả `200` `UserResponse`. Lỗi: `404`, `422 SELF_LOCKOUT`.
7. **`POST /users/{id}/password-reset`**: Body `{ "newPassword": String (@Size 8..128, optional - bỏ trống tự sinh 10 ký tự ngẫu nhiên) }`. Đặt `mustChangePassword = true` và cập nhật `passwordChangedAt`. Trả `200` `PasswordResetResponse { userId, username, temporaryPassword, mustChangePassword }`. Lỗi: `404`, `422 SELF_RESET_NOT_ALLOWED`.

### 5.3 US-10: Hồ sơ cá nhân (`MeController`)

Quyền: Mọi vai trò đã đăng nhập.

1. **`GET /me`**: Trả `200` `UserResponse`.
2. **`PUT /me`**: Body `{ "fullName" (2..150, @NotBlank), "email" (@Email, optional), "phone" (`0xxxxxxxxx` | `+84xxxxxxxxx`, optional) }`. Trả `200` `UserResponse`. Lỗi: `400`, `409 DUPLICATE` (`email`).
3. **`PATCH /me/password`**: Body `{ "oldPassword" (@NotBlank), "newPassword" (8..128, khác cũ & khác username), "confirmPassword" (@NotBlank, khớp `newPassword`) }`. Đặt `mustChangePassword = false`, cập nhật `passwordChangedAt`. Trả `204 No Content`. Lỗi: `400 VALIDATION_ERROR` (`errors.oldPassword` / `newPassword` / `confirmPassword`).

### 5.4 US-01: Quản lý hộ gia đình (`HouseholdController`)

1. **`POST /households`** (`BQT`):
   - Body (`CreateHouseholdRequest`): `householdCode` (1..30, `^[a-zA-Z0-9._-]+$`), `apartmentNo` (1..20, `^[a-zA-Z0-9._-]+$`), `floor` (1..60, optional), `area` (> 0, <= 1000, max 2 thập phân), `head` (`fullName` 2..150, `dateOfBirth` 1900-01-01..hôm nay, `gender` `MALE|FEMALE` optional, `identityNumber` 9|12 số optional, `phone` optional).
   - Tạo hộ `ACTIVE`, chủ hộ `RESIDING` (`isHead = true`), và 1 bản ghi `resident_movement` (`MOVE_IN`, `isInitial = true`).
   - `201 Created`: trả `HouseholdResponse`. Lỗi: `400`, `409 DUPLICATE` (`householdCode`, `apartmentNo`, `head.identityNumber`).
2. **`GET /households`** (`BQT`, `ACCOUNTANT`): Query `keyword`, `status` (`ALL`|`ACTIVE`|`INACTIVE`, mặc định `ALL`), `page` (1), `size` (20). Sắp xếp `householdCode` ASC. Trả `PageResponse<HouseholdListItemResponse>`.
3. **`GET /households/{id}`** (`BQT`, `ACCOUNTANT`): Trả `200` `HouseholdDetailResponse` (gồm danh sách `residents` chưa xóa mềm, sắp xếp theo `id`), hoặc `404 NOT_FOUND`.
4. **`PUT /households/{id}`** (`BQT`): Body `{ householdCode, apartmentNo, floor (1..60), area (>0, <=1000, @Digits(integer=4, fraction=2)) }`. Trả `200` `HouseholdResponse`. Lỗi: `400`, `404`, `409 DUPLICATE`.
5. **`PUT /households/{id}/head`** (`BQT`): Body `{ "residentId": Long (@NotNull) }`. Nhân khẩu mới phải thuộc hộ, `RESIDING`, chưa phải chủ hộ; hộ phải `ACTIVE`. Trả `200` `HouseholdResponse`. Lỗi: `404`, `422 HOUSEHOLD_INACTIVE`, `422 INVALID_NEW_HEAD`.
6. **`PATCH /households/{id}/status`** (`BQT`): Body `{ "status": "ACTIVE" | "INACTIVE" }`. Trả `200` `HouseholdResponse`. Lỗi: `404`.

### 5.5 US-02: Quản lý nhân khẩu (`ResidentController`)

1. **`POST /households/{id}/residents`** (`BQT`):
   - Body (`CreateResidentRequest`): `fullName` (2..150), `dateOfBirth` (1900-01-01..hôm nay), `gender` (`MALE|FEMALE`), `identityNumber` (9|12 số), `phone`, `relationshipToHead` (1..50), `entryType` (`MOVE_IN`|`BIRTH`|`TEMPORARY_RESIDENCE`|`EXISTING`, mặc định `MOVE_IN`), `startDate`/`endDate` (chỉ dùng khi `TEMPORARY_RESIDENCE`, `startDate` bắt buộc, `endDate >= startDate`), `entryDate` (`@PastOrPresent`).
   - Tự ghi biến động ban đầu `isInitial = true` (trừ `EXISTING`).
   - `201 Created`: trả `ResidentResponse`. Lỗi: `400`, `404`, `409 DUPLICATE`, `422 HOUSEHOLD_INACTIVE`.
2. **`GET /households/{id}/residents`** (`BQT`, `ACCOUNTANT`): Query `status` (mặc định `RESIDING,TEMPORARILY_ABSENT,TEMPORARY_RESIDENCE`, hoặc `ALL`). Sắp xếp `head` DESC, `dateOfBirth` ASC. Trả `List<ResidentInHouseholdResponse>`.
3. **`GET /residents`** (`BQT`, `ACCOUNTANT`): Query `keyword` (không dấu), `householdId`, `status`, `statusExpired` (`true` lọc quá hạn), `page` (1), `size` (20, max 100). Trả `PageResponse<ResidentResponse>`.
4. **`GET /residents/{id}`** (`BQT`, `ACCOUNTANT`): Trả `ResidentResponse` hoặc `404`.
5. **`PUT /residents/{id}`** (`BQT`): Body `UpdateResidentRequest` (`fullName`, `dateOfBirth`, `gender`, `identityNumber`, `phone`, `relationshipToHead`; các trường khác bị bỏ qua). Trả `200` `ResidentResponse`. Lỗi: `400`, `404`, `409 DUPLICATE`.
6. **`DELETE /residents/{id}`** (`BQT`): Xóa mềm (`deletedAt = now`). Trả `204 No Content`. Lỗi: `404`, `422 INVALID_NEW_HEAD` (`ALREADY_HEAD`), `409 HAS_ACCOUNT` (`details.residentId`), `409 HAS_HISTORY`.

### 5.6 US-03: Biến động nhân khẩu (`ResidentController`)

1. **`POST /residents/{id}/movements`** (`BQT`):
   - Body (`CreateMovementRequest`): `movementType` (`MOVE_IN`, `MOVE_OUT`, `TRANSFER`, `DEATH`, `TEMPORARY_ABSENCE`, `TEMPORARY_RESIDENCE`, `RETURN` — cấm `BIRTH`), `effectiveDate`, `startDate`, `endDate`, `newHouseholdId`, `relationshipToHead`, `newHeadResidentId`, `note` (max 500).
   - Khóa dòng `resident` (`PESSIMISTIC_WRITE`). Nếu chủ hộ `MOVE_OUT`/`DEATH`/`TRANSFER` và hộ còn người `RESIDING` thì bắt buộc truyền `newHeadResidentId` (không thì `422 HEAD_REQUIRED`). Nếu `MOVE_OUT`/`DEATH` sẽ tự khóa `app_user` liên kết (`isActive = false`).
   - `201 Created`: trả `RecordMovementResponse { movement, resident }`.
   - Lỗi: `400 VALIDATION_ERROR`, `404 NOT_FOUND` (resident hoặc `newHouseholdId` không tồn tại), `422 RESIDENT_NOT_RESIDING`, `422 INVALID_TRANSITION`, `422 HOUSEHOLD_INACTIVE`, `422 HEAD_REQUIRED`, `422 INVALID_NEW_HEAD`.
2. **`GET /residents/{id}/movements`** (`BQT`, `ACCOUNTANT`): Trả `200` mảng `MovementResponse` sắp xếp `effectiveDate` DESC, `recordedAt` DESC, `id` DESC.

### 5.7 US-04: Tìm kiếm nhanh (`SearchController`)

1. **`GET /search`** (`BQT`, `ACCOUNTANT`):
   - Query: `keyword` (bắt buộc, sau trim 2..100 ký tự), `type` (`ALL`|`HOUSEHOLD`|`RESIDENT`, mặc định `ALL`), `limit` (1..50, mặc định 20).
   - Tìm hộ theo `householdCode`/`apartmentNo` (`ACTIVE` & `INACTIVE`, trừ xóa mềm); tìm nhân khẩu theo `fullName` không phân biệt dấu (`f_unaccent`) thuộc mọi `residencyStatus` (trừ xóa mềm).
   - `200 OK`: `SearchResponse { empty, households, residents, totals, hasMore }`.

### 5.8 US-05 & US-06: Cổng cư dân & duyệt đề nghị (`MeController`, `ResidentRequestController`)

#### Phía Cư dân (`RESIDENT`)
1. **`GET /me/household`**: Trả `200` `MyHouseholdResponse { household, myResidentId, residents, pendingRequests }` hoặc `404 NOT_FOUND` nếu chưa gắn nhân khẩu/hộ.
2. **`POST /me/resident-requests`**:
   - Body (`SubmitResidentRequest`): `type` (`PROFILE_UPDATE` | `TEMPORARY_ABSENCE` | `TEMPORARY_RESIDENCE`), `residentId` (bắt buộc trừ `TEMPORARY_RESIDENCE`), `profile` (`ResidentInfo`, khi `PROFILE_UPDATE`), `person` (`ResidentInfo`, khi `TEMPORARY_RESIDENCE`), `startDate` & `endDate` (bắt buộc khi tạm vắng/tạm trú; `startDate <= today`, `endDate >= startDate`), `note` (max 500).
   - `201 Created`: `ResidentRequestResponse` (`PENDING`).
   - Lỗi: `400 VALIDATION_ERROR`, `403 RESIDENT_NOT_IN_HOUSEHOLD`, `409 DUPLICATE`, `409 PENDING_REQUEST_EXISTS`, `422 RESIDENT_LINK_REQUIRED`, `422 HOUSEHOLD_INACTIVE`, `422 RESIDENT_NOT_RESIDING`, `422 INVALID_TRANSITION`, `422 NO_CHANGES`.
3. **`GET /me/resident-requests`**: Query `status` (mặc định `ALL`), `type`, `page` (1), `size` (20, max 100). Chỉ trả đề nghị do chính user gửi, mới nhất trước. Trả `PageResponse<ResidentRequestResponse>`.
4. **`GET /me/resident-requests/{id}`**: Trả `ResidentRequestResponse` nếu do chính mình gửi; không phải hoặc không tồn tại trả `404 NOT_FOUND`.
5. **`POST /me/resident-requests/{id}/cancel`**: Không body. Khóa dòng, chuyển `CANCELLED` (gán `decidedBy = currentUser`, `decidedAt = now`). Lỗi: `404`, `409 REQUEST_NOT_PENDING`.

#### Phía Ban quản trị (`BQT`)
6. **`GET /resident-requests`**: Query `status` (mặc định `PENDING`), `type`, `householdId`, `keyword` (`householdCode`/`apartmentNo`), `page` (1), `size` (20, max 100). Khi lọc chỉ `PENDING` sắp xếp cũ nhất trước (`submittedAt` ASC), ngược lại mới nhất trước.
7. **`GET /resident-requests/{id}`**: Trả `200` `ResidentRequestResponse` hoặc `404`.
8. **`POST /resident-requests/{id}/approve`**: Body optional `{ "note": String (max 500) }`. Khóa dòng (`PESSIMISTIC_WRITE`), kiểm tra hộ `ACTIVE`:
   - `PROFILE_UPDATE`: đối chiếu `beforeValues` với hiện tại (lệch -> `409 REQUEST_OUTDATED`), cập nhật các trường trong `changes`.
   - `TEMPORARY_ABSENCE`: gọi `residentService.recordMovement(...)` với `note = "Theo khai báo #<id>..."`, lưu `resultResidentId` & `resultMovementId`.
   - `TEMPORARY_RESIDENCE`: gọi `residentService.createResident(..., TEMPORARY_RESIDENCE, ...)` tạo nhân khẩu mới, lưu `resultResidentId`.
   - Trả `200` `ResidentRequestResponse` (`APPROVED`).
9. **`POST /resident-requests/{id}/reject`**: Body `{ "reason": String (@NotBlank, max 500) }`. Trả `200` `ResidentRequestResponse` (`REJECTED`). Lỗi: `400`, `404`, `409 REQUEST_NOT_PENDING`.

### 5.9 US-12: Danh mục khoản thu (`FeeCategoryController`)

Quyền: `ACCOUNTANT`, `BQT`.

1. **`POST /fee-categories`**:
   - Body (`CreateFeeCategoryRequest`): `code` (2..30, `[A-Za-z0-9_]+`), `name` (1..150), `description` (max 1000), `feeKind` (`MANDATORY`|`VOLUNTARY`), `calculationMethod` (`PER_AREA`|`FIXED` cho `MANDATORY`; `MANUAL` cho `VOLUNTARY`), `unitPrice` (> 0, <= 10^9, max 2 thập phân cho `MANDATORY`; `0` cho `VOLUNTARY`), `billingCycle` (`MONTHLY`|`ONE_TIME` cho `MANDATORY`; `ONE_TIME` cho `VOLUNTARY`), `dueDay` (1..28 cho `MANDATORY`; `null` cho `VOLUNTARY`).
   - `201 Created`: trả `FeeCategoryResponse` (không có `inUse`).
   - Lỗi: `400 VALIDATION_ERROR` (chú ý `details.errors`), `409 DUPLICATE` (`details.errors.code`).
2. **`GET /fee-categories`**: Query `kind`, `active`, `keyword`. Trả `200` mảng `FeeCategoryResponse` (`MANDATORY` trước, rồi `code` ASC).
3. **`GET /fee-categories/{id}`**: Trả `200` `FeeCategoryResponse` hoặc `404`.
4. **`PUT /fee-categories/{id}`**: Body `CreateFeeCategoryRequest`. Nếu `receivableRepository.existsByFeeCategory_Id(id) == true`, cấm đổi `code`, `feeKind`, `calculationMethod`, `billingCycle` -> `409 HAS_TRANSACTIONS` (`details.errors`, `details.suggestedAction = "CREATE_NEW"`). Trả `200` `FeeCategoryResponse`.
5. **`PATCH /fee-categories/{id}/status`**: Body `{ "active": Boolean (@NotNull) }`. Trả `200` `FeeCategoryResponse`.

### 5.10 US-13: Tính thử phí (`FeeCalculationController`)

Quyền: `ACCOUNTANT`, `BQT`.

1. **`POST /fee-calculation/preview`**:
   - Body: `{ "householdId": Long (@NotNull, @Positive), "feeCategoryId": Long (@NotNull, @Positive) }`.
   - `200 OK`: `FeeCalculationPreviewResponse { householdId, householdCode, apartmentNo, feeCategoryId, feeCode, feeName, calculationMethod, area, unitPrice, amount, formula }`.
   - Lỗi: `400`, `404`, `422 HOUSEHOLD_INACTIVE`, `422 FEE_INACTIVE`, `422 FEE_NOT_BILLABLE`, `422 MISSING_AREA`.

### 5.11 US-14: Kỳ thu & Lập danh sách khoản phải thu (`BillingPeriodController`, `BillingGenerationController`)

1. **`POST /billing-periods`** (`ACCOUNTANT`, `BQT`):
   - Body (`BillingPeriodRequest`): `code` (1..30, `^[A-Za-z0-9._-]+$`), `name` (1..100), `startDate`, `endDate` (`>= startDate`).
   - `201 Created`: `BillingPeriodResponse`. Lỗi: `400 VALIDATION_ERROR` (`details.endDate` khi `endDate < startDate`), `409 DUPLICATE`, `422 PERIOD_OVERLAP`.
2. **`GET /billing-periods`** (`ACCOUNTANT`, `BQT`): Query `year`, `status` (`OPEN`|`CLOSED`), `generated` (`true`|`false`), `page` (1), `size` (20, max 100). Sắp xếp `startDate` DESC. Trả `PageResponse<BillingPeriodResponse>`.
3. **`GET /billing-periods/{id}`** (`ACCOUNTANT`, `BQT`): Trả `200` `BillingPeriodDetailResponse` (gồm `generated`, `generatedBy`, `closedBy`, `summary`).
4. **`PUT /billing-periods/{id}`** (`ACCOUNTANT`, `BQT`): Body `BillingPeriodRequest`. Chỉ sửa được khi `generatedAt == null`. Lỗi: `400`, `404`, `409 ALREADY_GENERATED`, `409 DUPLICATE`, `422 PERIOD_OVERLAP`.
5. **`DELETE /billing-periods/{id}`** (`ACCOUNTANT`, `BQT`): Chỉ xóa được khi `generatedAt == null`. Trả `204 No Content`. Lỗi: `404`, `409 ALREADY_GENERATED`.
6. **`PATCH /billing-periods/{id}/status`** (**chỉ `BQT`**): Body `{ "status": "OPEN" | "CLOSED" }`. Đóng kỳ yêu cầu `generatedAt != null`. Trả `200` `BillingPeriodResponse`. Lỗi: `400`, `403`, `404`, `422 PERIOD_NOT_GENERATED`.
7. **`POST /billing-periods/{id}/generate`** (`ACCOUNTANT`, `BQT`):
   - Query `dryRun` (mặc định `false`). Không body.
   - Khóa `billing_period` và các `fee_category` (`MANDATORY`, `is_active = TRUE`). Khoản `ONE_TIME` chỉ tính nếu chưa từng có bản ghi `receivable` nào.
   - Hộ `ACTIVE` & `deleted_at IS NULL`; nếu `PER_AREA` mà `area IS NULL` thì bỏ qua và đưa vào `skipped` (`MISSING_AREA`).
   - Hạn nộp: `MAKE_DATE(YEAR(start_date), MONTH(start_date), due_day)`.
   - `200 OK`: `BillingGenerationResponse { billingPeriodId, dryRun, generatedReceivables, householdCount, totalAmount, byFee, skipped, inactiveHouseholdCount }`.
   - Lỗi: `404`, `422 PERIOD_CLOSED`, `409 ALREADY_GENERATED`, `422 NO_ACTIVE_FEE`, `422 NOTHING_TO_GENERATE`.

---

## 6. Bảng tổng hợp toàn bộ 49 Endpoint thực tế trong Backend

| # | US | Method | Endpoint | Vai trò |
| --- | --- | --- | --- | --- |
| 1 | US-08 | POST | `/auth/login` | Công khai |
| 2 | US-08 | POST | `/auth/logout` | Đã đăng nhập |
| 3 | US-09 | GET | `/roles` | `BQT` |
| 4 | US-09 | POST | `/users` | `BQT` |
| 5 | US-09 | GET | `/users` | `BQT` |
| 6 | US-09 | GET | `/users/{id}` | `BQT` |
| 7 | US-09 | PUT | `/users/{id}/roles` | `BQT` |
| 8 | US-09 | PATCH | `/users/{id}/status` | `BQT` |
| 9 | US-09 | POST | `/users/{id}/password-reset` | `BQT` |
| 10 | US-10 | GET | `/me` | Đã đăng nhập |
| 11 | US-10 | PUT | `/me` | Đã đăng nhập |
| 12 | US-10 | PATCH | `/me/password` | Đã đăng nhập |
| 13 | US-05 | GET | `/me/household` | `RESIDENT` |
| 14 | US-01 | POST | `/households` | `BQT` |
| 15 | US-01 | GET | `/households` | `BQT`, `ACCOUNTANT` |
| 16 | US-01 | GET | `/households/{id}` | `BQT`, `ACCOUNTANT` |
| 17 | US-01 | PUT | `/households/{id}` | `BQT` |
| 18 | US-01 | PUT | `/households/{id}/head` | `BQT` |
| 19 | US-01 | PATCH | `/households/{id}/status` | `BQT` |
| 20 | US-02 | POST | `/households/{id}/residents` | `BQT` |
| 21 | US-02 | GET | `/households/{id}/residents` | `BQT`, `ACCOUNTANT` |
| 22 | US-02 | GET | `/residents` | `BQT`, `ACCOUNTANT` |
| 23 | US-02 | GET | `/residents/{id}` | `BQT`, `ACCOUNTANT` |
| 24 | US-02 | PUT | `/residents/{id}` | `BQT` |
| 25 | US-02 | DELETE | `/residents/{id}` | `BQT` |
| 26 | US-03 | POST | `/residents/{id}/movements` | `BQT` |
| 27 | US-03 | GET | `/residents/{id}/movements` | `BQT`, `ACCOUNTANT` |
| 28 | US-04 | GET | `/search` | `BQT`, `ACCOUNTANT` |
| 29 | US-05,06 | POST | `/me/resident-requests` | `RESIDENT` |
| 30 | US-05,06 | GET | `/me/resident-requests` | `RESIDENT` |
| 31 | US-05,06 | GET | `/me/resident-requests/{id}` | `RESIDENT` |
| 32 | US-05,06 | POST | `/me/resident-requests/{id}/cancel` | `RESIDENT` |
| 33 | US-05,06 | GET | `/resident-requests` | `BQT` |
| 34 | US-05,06 | GET | `/resident-requests/{id}` | `BQT` |
| 35 | US-05,06 | POST | `/resident-requests/{id}/approve` | `BQT` |
| 36 | US-05,06 | POST | `/resident-requests/{id}/reject` | `BQT` |
| 37 | US-12 | POST | `/fee-categories` | `ACCOUNTANT`, `BQT` |
| 38 | US-12 | GET | `/fee-categories` | `ACCOUNTANT`, `BQT` |
| 39 | US-12 | GET | `/fee-categories/{id}` | `ACCOUNTANT`, `BQT` |
| 40 | US-12 | PUT | `/fee-categories/{id}` | `ACCOUNTANT`, `BQT` |
| 41 | US-12 | PATCH | `/fee-categories/{id}/status` | `ACCOUNTANT`, `BQT` |
| 42 | US-13 | POST | `/fee-calculation/preview` | `ACCOUNTANT`, `BQT` |
| 43 | US-14 | POST | `/billing-periods` | `ACCOUNTANT`, `BQT` |
| 44 | US-14 | GET | `/billing-periods` | `ACCOUNTANT`, `BQT` |
| 45 | US-14 | GET | `/billing-periods/{id}` | `ACCOUNTANT`, `BQT` |
| 46 | US-14 | PUT | `/billing-periods/{id}` | `ACCOUNTANT`, `BQT` |
| 47 | US-14 | DELETE | `/billing-periods/{id}` | `ACCOUNTANT`, `BQT` |
| 48 | US-14 | PATCH | `/billing-periods/{id}/status` | `BQT` |
| 49 | US-14 | POST | `/billing-periods/{id}/generate` | `ACCOUNTANT`, `BQT` |
