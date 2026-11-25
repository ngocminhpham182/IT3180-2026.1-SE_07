### 9.1 Đăng nhập và khởi tạo phiên

*Mã nguồn:* LoginPage.jsx · AuthContext.login() · AuthController.login() · AuthService.login() · JwtService.generateToken() · UserService.getMe()

Ba trường hợp sai (không có user, tài khoản bị khóa, sai mật khẩu) đều trả cùng một lỗi INVALID_CREDENTIALS để không lộ thông tin tài khoản. Token chỉ nằm trong sessionStorage của trình duyệt, backend không lưu phiên (STATELESS).

```mermaid
flowchart TD
    S((" ")):::start --> A1["Người dùng nhập tên đăng nhập, mật khẩu<br/>và bấm Đăng nhập"]:::user
    A1 --> D1{"Đã nhập đủ<br/>cả hai trường?"}
    D1 -- "Thiếu" --> X1("LoginPage báo thiếu thông tin<br/>(không gọi API)"):::err
    D1 -- "Đủ" --> A2["AuthContext.login gọi<br/>POST /auth/login (không kèm token)"]:::fe
    A2 --> D2{"LoginRequest<br/>qua được @Valid?"}
    D2 -- "Không" --> X2("400 VALIDATION_ERROR"):::err
    D2 -- "Có" --> A3["AuthService tìm user theo username<br/>(không phân biệt hoa thường)"]:::be
    A3 --> D3{"Tìm thấy user?"}
    D3 -- "Không" --> X3("401 INVALID_CREDENTIALS<br/>Sai tài khoản hoặc mật khẩu"):::err
    D3 -- "Có" --> D4{"Tài khoản<br/>đang active?"}
    D4 -- "Không" --> X3
    D4 -- "Có" --> D5{"BCrypt khớp<br/>mật khẩu?"}
    D5 -- "Không" --> X3
    D5 -- "Có" --> A4["Ghi lastLoginAt = thời điểm hiện tại"]:::be
    A4 --> A5["JwtService.generateToken:<br/>sub = username, userId, roles, iat, exp = iat + 8 giờ"]:::be
    A5 --> A6["200 LoginResponse:<br/>accessToken, tokenType, expiresIn, expiresAt, user"]:::be
    A6 --> A7["Lưu accessToken và hạn dùng<br/>vào sessionStorage"]:::fe
    A7 --> A8["Gọi GET /me (kèm Bearer token)<br/>để xác minh tài khoản"]:::fe
    A8 --> D6{"Kết quả GET /me?"}
    D6 -- "User hợp lệ, active" --> A9["Lưu user vào sessionStorage,<br/>cập nhật state token + user"]:::fe
    D6 -- "404 NOT_FOUND" --> A9b["Dùng thông tin user<br/>trong LoginResponse"]:::fe
    D6 -- "Lỗi khác" --> X4("signOut: xóa phiên,<br/>LoginPage hiển thị lỗi"):::err
    A9b --> A9
    A9 --> D7{"homeForRoles(roles)"}
    D7 -- "BQT hoặc ACCOUNTANT" --> P1["Chuyển tới /ho-gia-dinh"]:::fe
    D7 -- "RESIDENT" --> P2["Chuyển tới /ho-cua-toi"]:::fe
    D7 -- "Vai trò khác" --> P3["Chuyển tới /ho-so"]:::fe
    P1 --> E(((" "))):::stop
    P2 --> E
    P3 --> E
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.2 Xác thực JWT và phân quyền cho mỗi request

*Mã nguồn:* api/http.js request() · JwtAuthenticationFilter.doFilterInternal() · SecurityConfig.securityFilterChain() · GlobalExceptionHandler · AuthContext (sự kiện bluemoon:session-expired)

Đây là luồng dùng chung: mọi sơ đồ còn lại đều giả định request đã đi qua luồng này. Vai trò được đọc lại từ DB ở mỗi request nên việc khóa tài khoản, đổi vai trò hay đổi mật khẩu có hiệu lực ngay, không cần chờ token hết hạn.

```mermaid
flowchart TD
    S((" ")):::start --> A1["api.request: gắn header<br/>Authorization: Bearer token (nếu có token)"]:::fe
    A1 --> D0{"Request có header<br/>Bearer?"}
    D0 -- "Không" --> Z1
    D0 -- "Có" --> D1{"Token đúng chữ ký<br/>và còn hạn?"}
    D1 -- "Không" --> X1["401 TOKEN_INVALID"]:::err
    D1 -- "Có" --> A2["Lấy username và iat từ token,<br/>tìm AppUser trong DB"]:::be
    A2 --> D2{"User tồn tại<br/>và đang active?"}
    D2 -- "Không" --> X1
    D2 -- "Có" --> D3{"iat của token trước<br/>passwordChangedAt?"}
    D3 -- "Có" --> X1
    D3 -- "Không" --> A3["Nạp vai trò mới nhất từ DB thành ROLE_x,<br/>đặt AppUser vào SecurityContext"]:::be
    A3 --> Z1{"SecurityConfig:<br/>endpoint yêu cầu gì?"}
    Z1 -- "permitAll: /auth/login, OPTIONS" --> C1
    Z1 -- "Cần đăng nhập hoặc vai trò" --> D4{"Đã xác thực?"}
    D4 -- "Chưa" --> X2["401 UNAUTHORIZED<br/>(authenticationEntryPoint)"]:::err
    D4 -- "Rồi" --> D5{"Có vai trò<br/>được phép?"}
    D5 -- "Không" --> X3("403 FORBIDDEN (accessDeniedHandler)<br/>FE hiển thị không có quyền"):::err
    D5 -- "Có" --> C1["Controller nhận request,<br/>gọi Service xử lý nghiệp vụ"]:::be
    C1 --> D6{"Service ném<br/>exception?"}
    D6 -- "Có" --> X4("GlobalExceptionHandler trả ApiErrorResponse:<br/>success=false, code, message, errors, details"):::err
    D6 -- "Không" --> R1["Trả ApiResponse:<br/>success=true, message, data"]:::be
    R1 --> R2["api.request trả payload.data<br/>cho trang gọi"]:::fe
    R2 --> E(((" "))):::stop
    X1 --> F1["api.request phát sự kiện<br/>bluemoon:session-expired"]:::fe
    X2 --> F1
    F1 --> F2["AuthContext.signOut: xóa sessionStorage,<br/>Protected chuyển về /dang-nhap"]:::fe
    F2 --> E
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.3 Tạo tài khoản người dùng

*Mã nguồn:* UserForm.jsx · UserController.createUser() · UserService.createUser() — chỉ vai trò BQT

Tài khoản mới luôn có active = true và mustChangePassword = true. Tài khoản có vai trò RESIDENT bắt buộc gắn với một nhân khẩu đang cư trú và mỗi nhân khẩu chỉ có tối đa một tài khoản.

```mermaid
flowchart TD
    S((" ")):::start --> A1["BQT mở form Tạo tài khoản, nhập thông tin,<br/>chọn vai trò (và nhân khẩu nếu là RESIDENT)"]:::user
    A1 --> D1{"validateUser<br/>ở FE đạt?"}
    D1 -- "Không" --> X1("Hiển thị lỗi từng trường<br/>(không gọi API)"):::err
    D1 -- "Đạt" --> A2["POST /users"]:::fe
    A2 --> D2{"CreateUserRequest<br/>qua được @Valid?"}
    D2 -- "Không" --> X2("400 VALIDATION_ERROR"):::err
    D2 -- "Có" --> D3{"Username<br/>đã tồn tại?"}
    D3 -- "Có" --> X3("409 DUPLICATE (username)"):::err
    D3 -- "Chưa" --> D4{"Có email và email<br/>đã tồn tại?"}
    D4 -- "Có" --> X4("409 DUPLICATE (email)"):::err
    D4 -- "Không" --> D5{"Password trùng<br/>username?"}
    D5 -- "Có" --> X5("400 VALIDATION_ERROR"):::err
    D5 -- "Không" --> D6{"Có vai trò RESIDENT<br/>mà thiếu residentId?"}
    D6 -- "Có" --> X6("422 RESIDENT_LINK_REQUIRED"):::err
    D6 -- "Không" --> D7{"Có residentId?"}
    D7 -- "Không" --> A3
    D7 -- "Có" --> D8{"Nhân khẩu tồn tại<br/>và đang RESIDING?"}
    D8 -- "Không" --> X7("400 VALIDATION_ERROR<br/>Nhân khẩu không tồn tại / phải đang cư trú"):::err
    D8 -- "Có" --> D9{"Nhân khẩu đã có<br/>tài khoản?"}
    D9 -- "Có" --> X8("409 DUPLICATE (residentId)"):::err
    D9 -- "Chưa" --> A3["Tra AppRole theo từng mã vai trò"]:::be
    A3 --> A4["Tạo AppUser: mật khẩu băm BCrypt,<br/>active = true, mustChangePassword = true"]:::be
    A4 --> A5["Lưu app_user và user_role"]:::be
    A5 --> A6["201 UserResponse"]:::be
    A6 --> A7["Đóng form, tải lại danh sách tài khoản"]:::fe
    A7 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.4 Quản trị tài khoản: đổi vai trò, khóa / mở khóa, đặt lại mật khẩu

*Mã nguồn:* UsersPage.jsx · UserController · UserService.updateUserRoles() / updateUserStatus() / resetPassword() — chỉ vai trò BQT

Lưu ý khi đối chiếu với code: ba kiểm tra "chính mình" dùng SecurityContext.getAuthentication().getName(), trong khi principal do JwtAuthenticationFilter đặt là đối tượng AppUser (không implement UserDetails/Principal). Cần kiểm thử lại các nhánh SELF_LOCKOUT và SELF_RESET_NOT_ALLOWED ở tầng API; hiện phía FE đã chặn bằng isSelf() và canAssignRoles().

```mermaid
flowchart TD
    S((" ")):::start --> A1["BQT chọn một tài khoản<br/>trong danh sách"]:::user
    A1 --> D0{"Thao tác?"}
    D0 -- "Đổi vai trò" --> R1["PUT /users/{id}/roles"]:::fe
    D0 -- "Khóa / mở khóa" --> T1["PATCH /users/{id}/status"]:::fe
    D0 -- "Đặt lại mật khẩu" --> P1["POST /users/{id}/password-reset"]:::fe

    R1 --> R2{"Tìm thấy<br/>tài khoản?"}
    R2 -- "Không" --> XN1("404 NOT_FOUND"):::err
    R2 -- "Có" --> R3{"Danh sách vai trò<br/>có phần tử trùng?"}
    R3 -- "Có" --> XR1("400 VALIDATION_ERROR"):::err
    R3 -- "Không" --> R4{"Thêm RESIDENT nhưng tài khoản<br/>chưa gắn nhân khẩu?"}
    R4 -- "Có" --> XR2("422 RESIDENT_LINK_REQUIRED"):::err
    R4 -- "Không" --> R5{"BQT tự gỡ vai trò BQT<br/>của chính mình?"}
    R5 -- "Có" --> XL1("422 SELF_LOCKOUT"):::err
    R5 -- "Không" --> R6["Thay toàn bộ tập vai trò, lưu"]:::be
    R6 --> OK1["200 UserResponse"]:::be

    T1 --> T2{"Tìm thấy<br/>tài khoản?"}
    T2 -- "Không" --> XN2("404 NOT_FOUND"):::err
    T2 -- "Có" --> T3{"Tự khóa<br/>chính mình?"}
    T3 -- "Có" --> XL2("422 SELF_LOCKOUT"):::err
    T3 -- "Không" --> T4["Đặt active theo yêu cầu, lưu"]:::be
    T4 --> OK1

    P1 --> P2{"Tìm thấy<br/>tài khoản?"}
    P2 -- "Không" --> XN3("404 NOT_FOUND"):::err
    P2 -- "Có" --> P3{"Đặt lại mật khẩu<br/>của chính mình?"}
    P3 -- "Có" --> XP1("422 SELF_RESET_NOT_ALLOWED"):::err
    P3 -- "Không" --> P4{"Có gửi<br/>newPassword?"}
    P4 -- "Không" --> P5["Sinh mật khẩu tạm 10 ký tự<br/>bằng SecureRandom"]:::be
    P4 -- "Có" --> P6
    P5 --> P6{"Mật khẩu trùng<br/>username?"}
    P6 -- "Có" --> XP2("400 VALIDATION_ERROR"):::err
    P6 -- "Không" --> P7["Băm BCrypt, mustChangePassword = true,<br/>passwordChangedAt = now (vô hiệu token cũ), lưu"]:::be
    P7 --> P8["200 PasswordResetResponse<br/>kèm mật khẩu tạm"]:::be
    P8 --> P9["Hiển thị mật khẩu tạm một lần<br/>để BQT chuyển cho người dùng"]:::fe

    OK1 --> F1["Tải lại danh sách tài khoản"]:::fe
    F1 --> E(((" "))):::stop
    P9 --> E
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.5 Đổi mật khẩu

*Mã nguồn:* ProfilePage.jsx · MeController.changePassword() · MeService.changePassword()

Sau khi đổi, passwordChangedAt được cập nhật nên mọi token phát hành trước đó bị JwtAuthenticationFilter từ chối. FE chủ động đăng xuất để người dùng đăng nhập lại bằng mật khẩu mới.

```mermaid
flowchart TD
    S((" ")):::start --> A1["Người dùng nhập mật khẩu cũ,<br/>mật khẩu mới và xác nhận"]:::user
    A1 --> D1{"validatePassword<br/>ở FE đạt?"}
    D1 -- "Không" --> X1("Hiển thị lỗi từng trường<br/>(không gọi API)"):::err
    D1 -- "Đạt" --> A2["PATCH /me/password"]:::fe
    A2 --> D2{"@Valid: đủ 3 trường,<br/>mật khẩu mới 8–128 ký tự?"}
    D2 -- "Không" --> X2("400 VALIDATION_ERROR"):::err
    D2 -- "Có" --> A3["MeService lấy user hiện tại<br/>từ SecurityContext"]:::be
    A3 --> D3{"Mật khẩu cũ<br/>khớp BCrypt?"}
    D3 -- "Không" --> X3("400 VALIDATION_ERROR<br/>oldPassword: Mật khẩu cũ không đúng"):::err
    D3 -- "Có" --> D4{"Mật khẩu mới trùng<br/>mật khẩu hiện tại?"}
    D4 -- "Có" --> X4("400 VALIDATION_ERROR<br/>newPassword: phải khác mật khẩu cũ"):::err
    D4 -- "Không" --> D5{"Mật khẩu mới<br/>trùng username?"}
    D5 -- "Có" --> X5("400 VALIDATION_ERROR<br/>newPassword: không được trùng username"):::err
    D5 -- "Không" --> D6{"confirmPassword khớp<br/>newPassword?"}
    D6 -- "Không" --> X6("400 VALIDATION_ERROR<br/>confirmPassword: không khớp"):::err
    D6 -- "Có" --> A4["Băm BCrypt mật khẩu mới,<br/>mustChangePassword = false,<br/>passwordChangedAt = now, lưu"]:::be
    A4 --> A5["204 No Content"]:::be
    A5 --> A6["signOut kèm thông báo:<br/>đổi mật khẩu thành công, hãy đăng nhập lại"]:::fe
    A6 --> A7["Chuyển về /dang-nhap"]:::fe
    A7 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.6 Tạo hộ gia đình kèm chủ hộ

*Mã nguồn:* HouseholdForm.jsx · HouseholdController.createHousehold() · HouseholdService.createHousehold() — chỉ vai trò BQT

Ba bản ghi (household, resident chủ hộ, resident_movement MOVE_IN ban đầu) được tạo trong cùng một transaction: lỗi ở bất kỳ bước nào sẽ rollback toàn bộ.

```mermaid
flowchart TD
    S((" ")):::start --> A1["BQT nhập mã hộ, số căn hộ, tầng, diện tích<br/>và thông tin chủ hộ"]:::user
    A1 --> D1{"validateHousehold<br/>ở FE đạt?"}
    D1 -- "Không" --> X1("Hiển thị lỗi từng trường<br/>(không gọi API)"):::err
    D1 -- "Đạt" --> A2["POST /households"]:::fe
    A2 --> D2{"CreateHouseholdRequest<br/>qua được @Valid?"}
    D2 -- "Không" --> X2("400 VALIDATION_ERROR"):::err
    D2 -- "Có" --> A3["Chuẩn hóa: trim + IN HOA mã hộ và số căn hộ,<br/>CCCD / SĐT rỗng thành null"]:::be
    A3 --> D3{"Ngày sinh chủ hộ<br/>trước 01/01/1900?"}
    D3 -- "Có" --> X3("400 VALIDATION_ERROR"):::err
    D3 -- "Không" --> A4["Kiểm tra trùng và gom lỗi:<br/>householdCode, apartmentNo, head.identityNumber"]:::be
    A4 --> D4{"Có trường<br/>bị trùng?"}
    D4 -- "Có" --> X4("409 DUPLICATE<br/>kèm errors theo từng trường"):::err
    D4 -- "Không" --> A5["Tạo Household: status = ACTIVE<br/>(saveAndFlush)"]:::be
    A5 --> A6["Tạo Resident chủ hộ: head = true,<br/>status = RESIDING, moveInDate = hôm nay"]:::be
    A6 --> A7["Tạo ResidentMovement MOVE_IN:<br/>isInitial = true, recordedBy = BQT hiện tại"]:::be
    A7 --> A8["201 HouseholdResponse<br/>residentCount = 1, presentCount = 1"]:::be
    A8 --> A9["Chuyển tới trang chi tiết hộ vừa tạo<br/>/ho-gia-dinh/{id}"]:::fe
    A9 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.7 Đổi chủ hộ

*Mã nguồn:* HouseholdDetailPage.jsx · HouseholdController.changeHouseholdHead() · HouseholdService.changeHouseholdHead() — chỉ vai trò BQT

Chủ hộ cũ được bỏ cờ head và flush trước khi gán chủ hộ mới để không vi phạm ràng buộc "mỗi hộ một chủ hộ" trong DB.

```mermaid
flowchart TD
    S((" ")):::start --> A1["BQT chọn một thành viên trong hộ<br/>làm chủ hộ mới"]:::user
    A1 --> A2["PUT /households/{id}/head<br/>body: residentId"]:::fe
    A2 --> D1{"Tìm thấy hộ<br/>(chưa xóa mềm)?"}
    D1 -- "Không" --> X1("404 NOT_FOUND"):::err
    D1 -- "Có" --> D2{"Hộ đang<br/>ACTIVE?"}
    D2 -- "Không" --> X2("422 HOUSEHOLD_INACTIVE"):::err
    D2 -- "Có" --> D3{"Tìm thấy nhân khẩu<br/>được chọn?"}
    D3 -- "Không" --> X3("422 INVALID_NEW_HEAD<br/>reason: NOT_FOUND"):::err
    D3 -- "Có" --> D4{"Nhân khẩu<br/>thuộc hộ này?"}
    D4 -- "Không" --> X4("422 INVALID_NEW_HEAD<br/>reason: NOT_IN_HOUSEHOLD"):::err
    D4 -- "Có" --> D5{"Trạng thái<br/>RESIDING?"}
    D5 -- "Không" --> X5("422 INVALID_NEW_HEAD<br/>reason: NOT_RESIDING"):::err
    D5 -- "Có" --> D6{"Đã là chủ hộ?"}
    D6 -- "Có" --> X6("422 INVALID_NEW_HEAD<br/>reason: ALREADY_HEAD"):::err
    D6 -- "Chưa" --> D7{"Hộ đang có<br/>chủ hộ?"}
    D7 -- "Có" --> A3["Chủ hộ cũ: head = false<br/>(saveAndFlush)"]:::be
    D7 -- "Không" --> A4
    A3 --> A4["Chủ hộ mới: head = true<br/>(saveAndFlush)"]:::be
    A4 --> A5["200 HouseholdResponse"]:::be
    A5 --> A6["Tải lại chi tiết hộ,<br/>thông báo đổi chủ hộ thành công"]:::fe
    A6 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.8 Thêm nhân khẩu vào hộ

*Mã nguồn:* ResidentForm.jsx · ResidentController.createResident() · ResidentService.createResident() — chỉ vai trò BQT

Endpoint này không bao giờ tạo chủ hộ (head luôn = false). Hình thức EXISTING dùng để nhập người đã ở sẵn nên không sinh bản ghi biến động.

```mermaid
flowchart TD
    S((" ")):::start --> A1["BQT nhập thông tin nhân khẩu,<br/>quan hệ với chủ hộ và hình thức vào hộ"]:::user
    A1 --> D1{"validateResident<br/>ở FE đạt?"}
    D1 -- "Không" --> X1("Hiển thị lỗi từng trường<br/>(không gọi API)"):::err
    D1 -- "Đạt" --> A2["POST /households/{id}/residents"]:::fe
    A2 --> D2{"CreateResidentRequest qua được<br/>@Valid và các @AssertTrue?"}
    D2 -- "Không" --> X2("400 VALIDATION_ERROR"):::err
    D2 -- "Có" --> D3{"Tìm thấy hộ?"}
    D3 -- "Không" --> X3("404 NOT_FOUND"):::err
    D3 -- "Có" --> D4{"Hộ đang<br/>ACTIVE?"}
    D4 -- "Không" --> X4("422 HOUSEHOLD_INACTIVE"):::err
    D4 -- "Có" --> D5{"Có CCCD/CMND và<br/>đã tồn tại?"}
    D5 -- "Có" --> X5("409 DUPLICATE (identityNumber)"):::err
    D5 -- "Không" --> A3["Mặc định: entryType = MOVE_IN,<br/>entryDate = hôm nay (giờ Việt Nam)"]:::be
    A3 --> D6{"entryType là<br/>TEMPORARY_RESIDENCE?"}
    D6 -- "Có" --> A4["status = TEMPORARY_RESIDENCE,<br/>statusUntil = endDate, moveInDate = startDate"]:::be
    D6 -- "Không" --> A5["status = RESIDING,<br/>moveInDate = entryDate"]:::be
    A4 --> A6["Lưu Resident (head = false)"]:::be
    A5 --> A6
    A6 --> D7{"entryType là<br/>EXISTING?"}
    D7 -- "Có" --> A8
    D7 -- "Không" --> A7["Lưu ResidentMovement ban đầu (isInitial = true):<br/>MOVE_IN, BIRTH hoặc TEMPORARY_RESIDENCE"]:::be
    A7 --> A8["201 ResidentResponse"]:::be
    A8 --> A9["Đóng form, tải lại danh sách nhân khẩu"]:::fe
    A9 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.9 Xóa nhân khẩu (xóa mềm)

*Mã nguồn:* ResidentDetailPage.jsx · ResidentController.deleteResident() · ResidentService.deleteResident() — chỉ vai trò BQT

Chỉ xóa được nhân khẩu nhập nhầm: chưa là chủ hộ, chưa có tài khoản và chưa có biến động nào ngoài bản ghi ban đầu. Các trường hợp khác phải ghi nhận biến động (chuyển đi, qua đời).

```mermaid
flowchart TD
    S((" ")):::start --> A1["BQT bấm Xóa và xác nhận<br/>trên trang chi tiết nhân khẩu"]:::user
    A1 --> A2["DELETE /residents/{id}"]:::fe
    A2 --> D1{"Tìm thấy nhân khẩu<br/>(chưa xóa mềm)?"}
    D1 -- "Không" --> X1("404 NOT_FOUND"):::err
    D1 -- "Có" --> D2{"Đang là<br/>chủ hộ?"}
    D2 -- "Có" --> X2("422 INVALID_NEW_HEAD<br/>Hãy đổi chủ hộ trước khi xóa"):::err
    D2 -- "Không" --> D3{"Đang gắn với<br/>tài khoản đăng nhập?"}
    D3 -- "Có" --> X3("409 HAS_ACCOUNT"):::err
    D3 -- "Không" --> D4{"Có biến động khác<br/>bản ghi ban đầu?"}
    D4 -- "Có" --> X4("409 HAS_HISTORY<br/>suggestedAction: RECORD_MOVEMENT"):::err
    D4 -- "Không" --> A3["Đặt deletedAt = now, lưu<br/>(không xóa vật lý)"]:::be
    A3 --> A4["204 No Content"]:::be
    A4 --> A5["Chuyển về trang chi tiết hộ<br/>/ho-gia-dinh/{householdId}"]:::fe
    A5 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.10a Ghi nhận biến động nhân khẩu — phần 1: kiểm tra hợp lệ

*Mã nguồn:* MovementPanel.jsx (MovementForm) · utils/supplement.js · ResidentController.recordMovement() · ResidentService.recordMovement() bước 1–7 — chỉ vai trò BQT

Bảng chuyển trạng thái hợp lệ: MOVE_OUT và DEATH từ RESIDING / TEMPORARILY_ABSENT / TEMPORARY_RESIDENCE · TRANSFER từ RESIDING / TEMPORARY_RESIDENCE · MOVE_IN từ MOVED_OUT · TEMPORARY_ABSENCE và TEMPORARY_RESIDENCE từ RESIDING · RETURN từ TEMPORARILY_ABSENT. BIRTH chỉ được ghi khi thêm nhân khẩu.

```mermaid
flowchart TD
    S((" ")):::start --> A1["BQT mở form Ghi nhận biến động, chọn loại<br/>(FE chỉ liệt kê loại hợp với trạng thái hiện tại)"]:::user
    A1 --> D1{"validateMovement<br/>ở FE đạt?"}
    D1 -- "Không" --> X1("Hiển thị lỗi từng trường<br/>(không gọi API)"):::err
    D1 -- "Đạt" --> A2["POST /residents/{id}/movements"]:::fe
    A2 --> A3["Khóa dòng resident<br/>(SELECT ... FOR UPDATE)"]:::be
    A3 --> D2{"Tìm thấy<br/>nhân khẩu?"}
    D2 -- "Không" --> X2("404 NOT_FOUND"):::err
    D2 -- "Có" --> D3{"movementType có giá trị, khác BIRTH<br/>và note không quá 500 ký tự?"}
    D3 -- "Không" --> X3("400 VALIDATION_ERROR"):::err
    D3 -- "Có" --> D4{{"Đủ trường bắt buộc theo loại?<br/>effectiveDate · newHouseholdId + relationshipToHead<br/>· startDate, endDate không trước startDate"}}
    D4 -- "Không" --> X4("400 VALIDATION_ERROR<br/>kèm errors theo từng trường"):::err
    D4 -- "Có" --> D5{"Đang MOVED_OUT hoặc DECEASED<br/>mà loại khác MOVE_IN?"}
    D5 -- "Có" --> X5("422 RESIDENT_NOT_RESIDING"):::err
    D5 -- "Không" --> D6{"Trạng thái hiện tại nằm trong<br/>allowedFrom của loại biến động?"}
    D6 -- "Không" --> X6("422 INVALID_TRANSITION<br/>kèm danh sách allowedFrom"):::err
    D6 -- "Có" --> D7{{"Ngày hiệu lực hợp lệ?<br/>không ở tương lai · không trước ngày sinh<br/>· không trước moveInDate (MOVE_OUT, DEATH)"}}
    D7 -- "Không" --> X7("400 VALIDATION_ERROR"):::err
    D7 -- "Có" --> D8{"Ngày hiệu lực trước ngày<br/>của biến động gần nhất?"}
    D8 -- "Có" --> X8("400 VALIDATION_ERROR"):::err
    D8 -- "Không" --> D9{"Loại là TRANSFER<br/>hoặc MOVE_IN?"}
    D9 -- "Không" --> NEXT["Sang phần 2:<br/>xử lý chủ hộ và ghi dữ liệu"]:::be
    D9 -- "Có" --> D10{"TRANSFER tới chính<br/>hộ hiện tại?"}
    D10 -- "Có" --> X9("400 VALIDATION_ERROR<br/>Hộ đích phải khác hộ hiện tại"):::err
    D10 -- "Không" --> D11{"Tìm thấy<br/>hộ đích?"}
    D11 -- "Không" --> X10("404 NOT_FOUND"):::err
    D11 -- "Có" --> D12{"Hộ đích<br/>đang ACTIVE?"}
    D12 -- "Không" --> X11("422 HOUSEHOLD_INACTIVE"):::err
    D12 -- "Có" --> NEXT
    NEXT --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.10b Ghi nhận biến động nhân khẩu — phần 2: xử lý chủ hộ và ghi dữ liệu

*Mã nguồn:* ResidentService.recordMovement() bước 8–11 · MovementPanel.jsx (xử lý HEAD_REQUIRED)

Toàn bộ nằm trong một transaction với dòng resident đã bị khóa. Bản ghi resident_movement chỉ thêm mới (append-only), không sửa hay xóa.

```mermaid
flowchart TD
    S((" ")):::start --> D1{"Nhân khẩu là chủ hộ và loại là<br/>MOVE_OUT, DEATH hoặc TRANSFER?"}
    D1 -- "Không" --> U0
    D1 -- "Có" --> A1["Tìm ứng viên chủ hộ mới: thành viên khác<br/>trong hộ đang RESIDING"]:::be
    A1 --> D2{"Có ứng viên?"}
    D2 -- "Không" --> D3{"Request có gửi<br/>newHeadResidentId?"}
    D3 -- "Có" --> X1("422 INVALID_NEW_HEAD<br/>reason: NOT_FOUND"):::err
    D3 -- "Không" --> A2["Bỏ cờ head của người rời hộ<br/>(hộ tạm thời không có chủ hộ)"]:::be
    A2 --> U0
    D2 -- "Có" --> D4{"Request có gửi<br/>newHeadResidentId?"}
    D4 -- "Không" --> X2["422 HEAD_REQUIRED<br/>kèm danh sách candidates"]:::err
    X2 --> F1["Form hiển thị danh sách ứng viên,<br/>BQT chọn chủ hộ mới và gửi lại"]:::fe
    F1 --> E2(((" "))):::stop
    D4 -- "Có" --> D5{{"Chủ hộ mới hợp lệ?<br/>khác người rời hộ · tồn tại · cùng hộ<br/>· chưa là chủ hộ · đang RESIDING"}}
    D5 -- "Không" --> X3("422 INVALID_NEW_HEAD<br/>reason: SAME_PERSON, NOT_FOUND, NOT_IN_HOUSEHOLD,<br/>ALREADY_HEAD hoặc NOT_RESIDING"):::err
    D5 -- "Có" --> A3["Chủ hộ cũ: head = false (saveAndFlush)<br/>rồi chủ hộ mới: head = true (saveAndFlush)"]:::be
    A3 --> U0{"Loại biến động?"}
    U0 -- "MOVE_OUT" --> U1["status = MOVED_OUT,<br/>moveOutDate = ngày hiệu lực"]:::be
    U0 -- "DEATH" --> U2["status = DECEASED,<br/>moveOutDate = ngày hiệu lực"]:::be
    U0 -- "TRANSFER / MOVE_IN" --> U3["status = RESIDING, household = hộ đích,<br/>moveInDate = ngày hiệu lực, moveOutDate = null,<br/>cập nhật relationshipToHead"]:::be
    U0 -- "TEMPORARY_ABSENCE" --> U4["status = TEMPORARILY_ABSENT,<br/>statusUntil = endDate"]:::be
    U0 -- "TEMPORARY_RESIDENCE" --> U5["status = TEMPORARY_RESIDENCE,<br/>statusUntil = endDate"]:::be
    U0 -- "RETURN" --> U6["status = RESIDING,<br/>statusUntil = null"]:::be
    U1 --> SV["Lưu Resident (saveAndFlush)"]:::be
    U2 --> SV
    U3 --> SV
    U4 --> SV
    U5 --> SV
    U6 --> SV
    SV --> D6{"Loại là MOVE_OUT<br/>hoặc DEATH?"}
    D6 -- "Có" --> A4["Nếu nhân khẩu có tài khoản liên kết:<br/>đặt active = false (khóa đăng nhập)"]:::be
    D6 -- "Không" --> A5
    A4 --> A5["Thêm ResidentMovement: isInitial = false,<br/>oldHousehold / newHousehold theo loại,<br/>recordedBy = BQT hiện tại"]:::be
    A5 --> A6["201 RecordMovementResponse:<br/>movement + resident sau cập nhật"]:::be
    A6 --> A7["Cập nhật thông tin nhân khẩu trên trang,<br/>tải lại lịch sử biến động"]:::fe
    A7 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.11 Tìm kiếm nhanh hộ gia đình và nhân khẩu

*Mã nguồn:* SearchPage.jsx · SearchController.search() · SearchService.search() — vai trò BQT, ACCOUNTANT

FE chờ 300 ms sau lần gõ cuối và hủy request cũ bằng AbortController. Mỗi nhóm trả tối đa limit kết quả kèm tổng số để FE biết còn kết quả hay không (hasMore).

```mermaid
flowchart TD
    S((" ")):::start --> A1["Người dùng gõ từ khóa,<br/>chọn loại (ALL / HOUSEHOLD / RESIDENT)"]:::user
    A1 --> D1{"Từ khóa dài<br/>2–100 ký tự?"}
    D1 -- "Không" --> X0("Không gọi API,<br/>chờ người dùng nhập tiếp"):::err
    D1 -- "Có" --> A2["Chờ 300 ms (debounce), hủy request trước đó"]:::fe
    A2 --> A3["GET /search?keyword&type&limit"]:::fe
    A3 --> D2{"keyword 2–100 ký tự, type hợp lệ,<br/>limit trong 1–50?"}
    D2 -- "Không" --> X1("400 VALIDATION_ERROR<br/>kèm errors theo tham số"):::err
    D2 -- "Có" --> A4["Escape ký tự đặc biệt của LIKE:<br/>dấu gạch chéo ngược, %, _"]:::be
    A4 --> D3{"type là ALL<br/>hoặc HOUSEHOLD?"}
    D3 -- "Có" --> A5["quickSearchHouseholds(keyword, limit)<br/>và countQuickSearchHouseholds"]:::be
    D3 -- "Không" --> A6["households = danh sách rỗng"]:::be
    A5 --> D4{"type là ALL<br/>hoặc RESIDENT?"}
    A6 --> D4
    D4 -- "Có" --> A7["quickSearchResidents(keyword, limit)<br/>và countQuickSearchResidents"]:::be
    D4 -- "Không" --> A8["residents = danh sách rỗng"]:::be
    A7 --> A9["Tính statusExpired cho người tạm vắng / tạm trú<br/>có statusUntil trước hôm nay"]:::be
    A9 --> A10["Tổng hợp: empty, totals, hasMore"]:::be
    A8 --> A10
    A10 --> A11["200 SearchResponse"]:::be
    A11 --> A12["Hiển thị hai nhóm kết quả,<br/>hoặc trạng thái không tìm thấy"]:::fe
    A12 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.12 Cư dân gửi đề nghị (cập nhật hồ sơ, tạm vắng, tạm trú)

*Mã nguồn:* MyHouseholdPage.jsx · ResidentRequestForm.jsx · ResidentRequestController.submitRequest() · ResidentRequestService.submit() — chỉ vai trò RESIDENT

Gửi đề nghị không làm thay đổi dữ liệu nhân khẩu: chỉ tạo bản ghi resident_request ở trạng thái PENDING. Với PROFILE_UPDATE, hệ thống chụp lại giá trị hiện tại (beforeValues) để phát hiện dữ liệu đã đổi khi BQT duyệt.

```mermaid
flowchart TD
    S((" ")):::start --> A0["Cư dân mở Hộ của tôi (GET /me/household),<br/>chọn loại đề nghị và điền form"]:::user
    A0 --> D0{"validateRequest<br/>ở FE đạt?"}
    D0 -- "Không" --> X0("Hiển thị lỗi từng trường<br/>(không gọi API)"):::err
    D0 -- "Đạt" --> A1["POST /me/resident-requests"]:::fe
    A1 --> D1{"Tài khoản gắn với<br/>nhân khẩu còn tồn tại?"}
    D1 -- "Không" --> X1("422 RESIDENT_LINK_REQUIRED"):::err
    D1 -- "Có" --> D2{"Body có type?"}
    D2 -- "Không" --> X2("400 VALIDATION_ERROR"):::err
    D2 -- "Có" --> D3{"Hộ của người gửi<br/>đang ACTIVE?"}
    D3 -- "Không" --> X3("422 HOUSEHOLD_INACTIVE"):::err
    D3 -- "Có" --> T{"Loại đề nghị?"}

    T -- "PROFILE_UPDATE" --> P1{"residentId thuộc<br/>hộ của người gửi?"}
    P1 -- "Không" --> XP1("403 RESIDENT_NOT_IN_HOUSEHOLD<br/>(thiếu residentId: 400)"):::err
    P1 -- "Có" --> P2{"Trường hồ sơ và note<br/>hợp lệ?"}
    P2 -- "Không" --> XP2("400 VALIDATION_ERROR"):::err
    P2 -- "Có" --> P3{"Nhân khẩu MOVED_OUT<br/>hoặc DECEASED?"}
    P3 -- "Có" --> XP3("422 RESIDENT_NOT_RESIDING"):::err
    P3 -- "Không" --> P4{"Có khác biệt so với<br/>dữ liệu hiện tại?"}
    P4 -- "Không" --> XP4("422 NO_CHANGES"):::err
    P4 -- "Có" --> P5{"CCCD mới trùng<br/>người khác?"}
    P5 -- "Có" --> XP5("409 DUPLICATE"):::err
    P5 -- "Không" --> P6{"Đã có PROFILE_UPDATE đang<br/>PENDING cho nhân khẩu này?"}
    P6 -- "Có" --> XP6("409 PENDING_REQUEST_EXISTS"):::err
    P6 -- "Không" --> P7["proposed = hồ sơ đề nghị,<br/>beforeValues = hồ sơ hiện tại"]:::be

    T -- "TEMPORARY_ABSENCE" --> B1{"residentId thuộc<br/>hộ của người gửi?"}
    B1 -- "Không" --> XB1("403 RESIDENT_NOT_IN_HOUSEHOLD<br/>(thiếu residentId: 400)"):::err
    B1 -- "Có" --> B2{{"startDate, endDate, note hợp lệ?<br/>(không ở tương lai, không trước ngày sinh<br/>và biến động gần nhất)"}}
    B2 -- "Không" --> XB2("400 VALIDATION_ERROR"):::err
    B2 -- "Có" --> B3{"Nhân khẩu đang<br/>RESIDING?"}
    B3 -- "Không" --> XB3("422 RESIDENT_NOT_RESIDING<br/>hoặc INVALID_TRANSITION"):::err
    B3 -- "Có" --> B4{"Đã có TEMPORARY_ABSENCE đang<br/>PENDING cho nhân khẩu này?"}
    B4 -- "Có" --> XB4("409 PENDING_REQUEST_EXISTS"):::err
    B4 -- "Không" --> B5["proposed = startDate, endDate"]:::be

    T -- "TEMPORARY_RESIDENCE" --> G1{"Ngày, thông tin khách<br/>và note hợp lệ?"}
    G1 -- "Không" --> XG1("400 VALIDATION_ERROR"):::err
    G1 -- "Có" --> G2{"CCCD của khách đã có<br/>trong bảng resident?"}
    G2 -- "Có" --> XG2("409 DUPLICATE"):::err
    G2 -- "Không" --> G3{"Đã có đề nghị tạm trú PENDING<br/>cùng CCCD?"}
    G3 -- "Có" --> XG3("409 PENDING_REQUEST_EXISTS"):::err
    G3 -- "Không" --> G4["proposed = startDate, endDate, person"]:::be

    P7 --> SV["Lưu ResidentRequest: status = PENDING,<br/>submittedBy, submittedAt (saveAndFlush)"]:::be
    B5 --> SV
    G4 --> SV
    SV --> D9{"DB báo vi phạm ràng buộc<br/>(hai request gửi đồng thời)?"}
    D9 -- "Có" --> X9("409 PENDING_REQUEST_EXISTS"):::err
    D9 -- "Không" --> OK["201 ResidentRequestResponse"]:::be
    OK --> F1["Chuyển tới /de-nghi/{id}, thông báo<br/>đã gửi và đang chờ Ban quản trị duyệt"]:::fe
    F1 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.13 Ban quản trị duyệt đề nghị

*Mã nguồn:* ResidentRequestDetailPage.jsx · ResidentRequestController.approveRequest() · ResidentRequestService.approve() · ResidentService.recordMovement() / createResident() — chỉ vai trò BQT

Việc duyệt và việc áp dụng thay đổi chạy trong cùng một transaction: nếu ghi nhận tạm vắng hoặc tạo nhân khẩu tạm trú thất bại thì đề nghị vẫn giữ trạng thái PENDING.

```mermaid
flowchart TD
    S((" ")):::start --> A1["BQT mở chi tiết đề nghị, nhập ghi chú<br/>(không bắt buộc) và xác nhận Duyệt"]:::user
    A1 --> A2["POST /resident-requests/{id}/approve"]:::fe
    A2 --> A3["Khóa dòng đề nghị<br/>(SELECT ... FOR UPDATE)"]:::be
    A3 --> D1{"Tìm thấy<br/>đề nghị?"}
    D1 -- "Không" --> X1("404 NOT_FOUND"):::err
    D1 -- "Có" --> D2{"Trạng thái<br/>là PENDING?"}
    D2 -- "Không" --> X2("409 REQUEST_NOT_PENDING<br/>FE tải lại trạng thái mới nhất"):::err
    D2 -- "Có" --> D3{"Hộ của đề nghị<br/>đang ACTIVE?"}
    D3 -- "Không" --> X3("422 HOUSEHOLD_INACTIVE"):::err
    D3 -- "Có" --> T{"Loại đề nghị?"}

    T -- "PROFILE_UPDATE" --> P1["Khóa dòng resident<br/>(SELECT ... FOR UPDATE)"]:::be
    P1 --> P2{"Nhân khẩu<br/>còn tồn tại?"}
    P2 -- "Không" --> XP1("409 REQUEST_OUTDATED"):::err
    P2 -- "Có" --> P3{"Các trường đề nghị đổi còn giống<br/>beforeValues lúc gửi?"}
    P3 -- "Không" --> XP2("409 REQUEST_OUTDATED<br/>kèm danh sách trường đã đổi"):::err
    P3 -- "Có" --> P4{"Đổi CCCD và CCCD mới<br/>trùng người khác?"}
    P4 -- "Có" --> XP3("409 DUPLICATE"):::err
    P4 -- "Không" --> P5["Áp các trường thay đổi vào Resident, lưu<br/>resultResidentId = id nhân khẩu"]:::be

    T -- "TEMPORARY_ABSENCE" --> B1["Dựng CreateMovementRequest TEMPORARY_ABSENCE<br/>note: Theo khai báo số {id}"]:::be
    B1 --> B2["Gọi ResidentService.recordMovement<br/>(toàn bộ kiểm tra của luồng biến động)"]:::be
    B2 --> B3{"Ghi nhận<br/>thành công?"}
    B3 -- "Không" --> XB1("Lỗi của luồng biến động: 400 / 422<br/>đề nghị vẫn PENDING"):::err
    B3 -- "Có" --> B4["resultMovementId = id biến động,<br/>resultResidentId = id nhân khẩu"]:::be

    T -- "TEMPORARY_RESIDENCE" --> G1{"CCCD của khách đã có<br/>trong bảng resident?"}
    G1 -- "Có" --> XG1("409 DUPLICATE"):::err
    G1 -- "Không" --> G2["Gọi ResidentService.createResident<br/>entryType = TEMPORARY_RESIDENCE"]:::be
    G2 --> G3["resultResidentId = id nhân khẩu vừa tạo"]:::be

    P5 --> F["Cập nhật đề nghị: status = APPROVED, decidedBy,<br/>decidedAt, decisionNote, kết quả; lưu"]:::be
    B4 --> F
    G3 --> F
    F --> OK["200 ResidentRequestResponse"]:::be
    OK --> U["Hiển thị trạng thái Đã duyệt,<br/>cập nhật số đề nghị chờ xử lý"]:::fe
    U --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.14 Từ chối đề nghị (BQT) và hủy đề nghị (cư dân)

*Mã nguồn:* ResidentRequestDetailPage.jsx · ResidentRequestService.reject() / cancel()

Cả hai thao tác đều khóa dòng đề nghị trước khi kiểm tra trạng thái, nên khi BQT duyệt và cư dân hủy cùng lúc thì chỉ một bên thành công, bên còn lại nhận REQUEST_NOT_PENDING. Cư dân truy cập đề nghị của người khác nhận 404 thay vì 403 để không lộ sự tồn tại của đề nghị.

```mermaid
flowchart TD
    S((" ")):::start --> D0{"Ai thao tác?"}
    D0 -- "BQT từ chối" --> R1["BQT nhập lý do từ chối và xác nhận"]:::user
    R1 --> R2["POST /resident-requests/{id}/reject"]:::fe
    R2 --> R3{"reason không rỗng,<br/>tối đa 500 ký tự?"}
    R3 -- "Không" --> XR1("400 VALIDATION_ERROR"):::err
    R3 -- "Có" --> R4["Khóa dòng đề nghị (FOR UPDATE)"]:::be
    R4 --> R5{"Tìm thấy<br/>đề nghị?"}
    R5 -- "Không" --> XR2("404 NOT_FOUND"):::err
    R5 -- "Có" --> R6{"Trạng thái<br/>là PENDING?"}
    R6 -- "Không" --> XR3("409 REQUEST_NOT_PENDING"):::err
    R6 -- "Có" --> R7["status = REJECTED, decidedBy = BQT,<br/>decidedAt = now, decisionReason; lưu"]:::be

    D0 -- "Cư dân hủy" --> C1["Cư dân bấm Hủy đề nghị và xác nhận"]:::user
    C1 --> C2["POST /me/resident-requests/{id}/cancel"]:::fe
    C2 --> C3["Khóa dòng đề nghị (FOR UPDATE)"]:::be
    C3 --> C4{"Tìm thấy và do chính<br/>người này gửi?"}
    C4 -- "Không" --> XC1("404 NOT_FOUND"):::err
    C4 -- "Có" --> C5{"Trạng thái<br/>là PENDING?"}
    C5 -- "Không" --> XC2("409 REQUEST_NOT_PENDING"):::err
    C5 -- "Có" --> C6["status = CANCELLED, decidedBy = người gửi,<br/>decidedAt = now; lưu"]:::be

    R7 --> OK["200 ResidentRequestResponse"]:::be
    C6 --> OK
    OK --> U["Hiển thị trạng thái mới của đề nghị"]:::fe
    U --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.15 Tạo và cập nhật khoản thu

*Mã nguồn:* FeeForm.jsx · FeeCategoryController.create() / update() · FeeCategoryService.create() / update() — vai trò ACCOUNTANT, BQT

Khoản bắt buộc (MANDATORY) chỉ tính theo diện tích (PER_AREA) hoặc cố định (FIXED) và phải có đơn giá, chu kỳ, hạn nộp. Khoản tự nguyện (VOLUNTARY) luôn là MANUAL, hệ thống tự đặt đơn giá = 0, chu kỳ ONE_TIME, không có hạn nộp. Khi tạo mới, kiểm tra trùng mã chạy ngay sau bước kiểm tra định dạng mã, trước khi trả các lỗi validation khác.

```mermaid
flowchart TD
    S((" ")):::start --> A1["Kế toán / BQT nhập mã, tên, loại khoản thu,<br/>cách tính, đơn giá, chu kỳ, hạn nộp"]:::user
    A1 --> D1{"validateFee<br/>ở FE đạt?"}
    D1 -- "Không" --> X1("Hiển thị lỗi từng trường<br/>(không gọi API)"):::err
    D1 -- "Đạt" --> D2{"Tạo mới hay<br/>cập nhật?"}
    D2 -- "Tạo mới" --> C1["POST /fee-categories"]:::fe
    D2 -- "Cập nhật" --> U1["PUT /fee-categories/{id}"]:::fe

    C1 --> C2{"Mã (đã trim + IN HOA)<br/>đã tồn tại?"}
    C2 -- "Có" --> XC1("409 DUPLICATE"):::err
    C2 -- "Chưa" --> V

    U1 --> U2{"Tìm thấy<br/>khoản thu?"}
    U2 -- "Không" --> XU1("404 NOT_FOUND"):::err
    U2 -- "Có" --> V

    V{{"Kiểm tra chung đạt?<br/>tên không rỗng · mô tả tối đa 1000 ký tự<br/>· mã khớp [A-Z0-9_]{2,30} · có feeKind và calculationMethod"}}
    V -- "Không" --> XV("400 VALIDATION_ERROR<br/>kèm errors theo từng trường"):::err
    V -- "Có" --> K{"feeKind?"}
    K -- "MANDATORY" --> M1{{"Cách tính PER_AREA hoặc FIXED,<br/>0 < đơn giá ≤ 1 tỷ (tối đa 2 số lẻ),<br/>có chu kỳ, hạn nộp 1–28?"}}
    M1 -- "Không" --> XV
    K -- "VOLUNTARY" --> M2{"Cách tính<br/>là MANUAL?"}
    M2 -- "Không" --> XV
    M2 -- "Có" --> M3["Chuẩn hóa: unitPrice = 0,<br/>billingCycle = ONE_TIME, dueDay = null"]:::be
    M1 -- "Có" --> W{"Tạo mới hay<br/>cập nhật?"}
    M3 --> W

    W -- "Tạo mới" --> C3["Lưu FeeCategory, active = true"]:::be
    C3 --> C4["201 FeeCategoryResponse"]:::be

    W -- "Cập nhật" --> U3{"Mã trùng với<br/>khoản thu khác?"}
    U3 -- "Có" --> XU2("409 DUPLICATE"):::err
    U3 -- "Không" --> U4{"Khoản thu đã phát sinh công nợ và<br/>đổi mã, loại, cách tính hoặc chu kỳ?"}
    U4 -- "Có" --> XU3("409 HAS_TRANSACTIONS<br/>suggestedAction: CREATE_NEW"):::err
    U4 -- "Không" --> U5["Cập nhật các trường, lưu"]:::be
    U5 --> U6["200 FeeCategoryResponse"]:::be

    C4 --> F["Đóng form, tải lại danh mục khoản thu"]:::fe
    U6 --> F
    F --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.16 Tính thử phí cho một hộ

*Mã nguồn:* FeePreview.jsx · FeeCalculationController.preview() · FeeCalculationService.preview() — vai trò ACCOUNTANT, BQT

Chỉ tính và trả kết quả để xem trước, không ghi gì vào DB (transaction readOnly). Số tiền làm tròn tới đồng theo HALF_UP.

```mermaid
flowchart TD
    S((" ")):::start --> A1["Người dùng mở Tính thử trên một khoản thu<br/>và chọn hộ gia đình"]:::user
    A1 --> D1{"Đã chọn hộ?"}
    D1 -- "Chưa" --> X1("Báo: Chọn hộ cần tính phí<br/>(không gọi API)"):::err
    D1 -- "Rồi" --> A2["POST /fee-calculation/preview<br/>body: householdId, feeCategoryId"]:::fe
    A2 --> D2{"Hai id có giá trị<br/>và lớn hơn 0?"}
    D2 -- "Không" --> X2("400 VALIDATION_ERROR"):::err
    D2 -- "Có" --> D3{"Tìm thấy hộ?"}
    D3 -- "Không" --> X3("404 NOT_FOUND"):::err
    D3 -- "Có" --> D4{"Hộ đang<br/>ACTIVE?"}
    D4 -- "Không" --> X4("422 HOUSEHOLD_INACTIVE"):::err
    D4 -- "Có" --> D5{"Tìm thấy<br/>khoản thu?"}
    D5 -- "Không" --> X5("404 NOT_FOUND"):::err
    D5 -- "Có" --> D6{"Khoản thu<br/>đang active?"}
    D6 -- "Không" --> X6("422 FEE_INACTIVE"):::err
    D6 -- "Có" --> D7{"Là khoản<br/>VOLUNTARY?"}
    D7 -- "Có" --> X7("422 FEE_NOT_BILLABLE"):::err
    D7 -- "Không" --> D8{"Cách tính?"}
    D8 -- "PER_AREA" --> D9{"Hộ có<br/>diện tích?"}
    D9 -- "Không" --> X8("422 MISSING_AREA"):::err
    D9 -- "Có" --> A3["amount = làm tròn(area × unitPrice)<br/>formula = area x unitPrice"]:::be
    D8 -- "FIXED" --> A4["amount = làm tròn(unitPrice)<br/>formula = unitPrice"]:::be
    D8 -- "MANUAL" --> X9("422 FEE_NOT_BILLABLE"):::err
    A3 --> A5["200 FeeCalculationPreviewResponse:<br/>hộ, khoản thu, area, unitPrice, amount, formula"]:::be
    A4 --> A5
    A5 --> A6["Hiển thị công thức và số tiền tính thử"]:::fe
    A6 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.17 Quản lý kỳ thu: tạo, sửa, xóa

*Mã nguồn:* PeriodForm.jsx · PeriodDetailPage.jsx · BillingPeriodController · BillingPeriodService.create() / update() / delete() — vai trò ACCOUNTANT, BQT

Kỳ thu mới luôn ở trạng thái OPEN và chưa lập danh sách. Sau khi đã lập danh sách khoản phải thu (generatedAt khác null) thì không được sửa hay xóa kỳ nữa.

```mermaid
flowchart TD
    S((" ")):::start --> D0{"Thao tác?"}
    D0 -- "Tạo" --> C0["Nhập mã, tên, ngày bắt đầu, ngày kết thúc"]:::user
    C0 --> C1["POST /billing-periods"]:::fe
    C1 --> C2{"@Valid đạt và endDate<br/>không trước startDate?"}
    C2 -- "Không" --> XC1("400 VALIDATION_ERROR"):::err
    C2 -- "Có" --> C3{"Mã kỳ thu<br/>đã tồn tại?"}
    C3 -- "Có" --> XC2("409 DUPLICATE (code)"):::err
    C3 -- "Chưa" --> C4{"Khoảng ngày chồng lấn<br/>kỳ thu khác?"}
    C4 -- "Có" --> XC3("422 PERIOD_OVERLAP<br/>kèm id và mã kỳ bị chồng"):::err
    C4 -- "Không" --> C5["Lưu BillingPeriod: status = OPEN,<br/>generatedAt = null, closedAt = null"]:::be
    C5 --> C6["201 BillingPeriodResponse"]:::be
    C6 --> C7["Chuyển tới /ky-thu/{id}"]:::fe

    D0 -- "Sửa" --> U0["Sửa mã, tên hoặc khoảng ngày"]:::user
    U0 --> U1["PUT /billing-periods/{id}"]:::fe
    U1 --> U2["Khóa dòng kỳ thu (FOR UPDATE)"]:::be
    U2 --> U3{"Tìm thấy<br/>kỳ thu?"}
    U3 -- "Không" --> XU1("404 NOT_FOUND"):::err
    U3 -- "Có" --> U4{"Kỳ đã lập<br/>danh sách?"}
    U4 -- "Có" --> XU2("409 ALREADY_GENERATED"):::err
    U4 -- "Chưa" --> U5{"Khoảng ngày hợp lệ, mã không trùng,<br/>không chồng lấn kỳ khác?"}
    U5 -- "Không" --> XU3("400 VALIDATION_ERROR, 409 DUPLICATE<br/>hoặc 422 PERIOD_OVERLAP"):::err
    U5 -- "Có" --> U6["Cập nhật code, name, startDate, endDate<br/>(giữ nguyên status và các mốc lập / đóng)"]:::be
    U6 --> U7["200 BillingPeriodResponse"]:::be
    U7 --> U8["Tải lại chi tiết kỳ thu"]:::fe

    D0 -- "Xóa" --> R0["Bấm Xóa kỳ thu và xác nhận"]:::user
    R0 --> R1["DELETE /billing-periods/{id}"]:::fe
    R1 --> R2["Khóa dòng kỳ thu (FOR UPDATE)"]:::be
    R2 --> R3{"Tìm thấy<br/>kỳ thu?"}
    R3 -- "Không" --> XR1("404 NOT_FOUND"):::err
    R3 -- "Có" --> R4{"Kỳ đã lập<br/>danh sách?"}
    R4 -- "Có" --> XR2("409 ALREADY_GENERATED"):::err
    R4 -- "Chưa" --> R5["Xóa bản ghi kỳ thu"]:::be
    R5 --> R6["204 No Content"]:::be
    R6 --> R7["Chuyển về /ky-thu"]:::fe

    C7 --> E(((" "))):::stop
    U8 --> E
    R7 --> E
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.18 Lập danh sách khoản phải thu cho kỳ thu

*Mã nguồn:* PeriodDetailPage.jsx perform() · BillingGenerationController.generate() · BillingGenerationService.generate() · ReceivableRepository — vai trò ACCOUNTANT, BQT

Khoản thu đủ điều kiện: MANDATORY, đang active, chu kỳ MONTHLY hoặc ONE_TIME chưa từng phát sinh công nợ. Mỗi hộ ACTIVE nhận một khoản phải thu cho mỗi khoản thu đủ điều kiện; khoản PER_AREA bị bỏ qua với hộ chưa có diện tích (MISSING_AREA). Xem trước (dryRun) và lập thật dùng chung một đường xử lý, chỉ khác ở bước ghi.

```mermaid
flowchart TD
    S((" ")):::start --> A1["Người dùng bấm Xem trước<br/>trên trang chi tiết kỳ thu"]:::user
    A1 --> A2["POST /billing-periods/{id}/generate?dryRun=true"]:::fe
    A2 --> B0
    B0["BillingGenerationService.generate:<br/>khóa dòng kỳ thu (FOR UPDATE)"]:::be --> D1{"Tìm thấy<br/>kỳ thu?"}
    D1 -- "Không" --> X1("404 NOT_FOUND"):::err
    D1 -- "Có" --> D2{"Kỳ đã<br/>CLOSED?"}
    D2 -- "Có" --> X2("422 PERIOD_CLOSED"):::err
    D2 -- "Không" --> D3{"Kỳ đã lập<br/>danh sách?"}
    D3 -- "Có" --> X3("409 ALREADY_GENERATED"):::err
    D3 -- "Chưa" --> B1["Khóa các khoản thu MANDATORY đang active<br/>(FOR UPDATE)"]:::be
    B1 --> D4{"Có khoản thu<br/>đủ điều kiện?"}
    D4 -- "Không" --> X4("422 NO_ACTIVE_FEE"):::err
    D4 -- "Có" --> B2["Tính theo từng khoản thu: số khoản phải thu, tổng tiền<br/>và danh sách bị bỏ qua vì thiếu diện tích"]:::be
    B2 --> D5{"Số khoản phải thu<br/>sinh được = 0?"}
    D5 -- "Có" --> X5("422 NOTHING_TO_GENERATE<br/>kèm danh sách skipped"):::err
    D5 -- "Không" --> B3["Đếm số hộ được lập, số hộ INACTIVE,<br/>cộng tổng tiền"]:::be
    B3 --> D6{"dryRun?"}
    D6 -- "false" --> B4["INSERT ... SELECT hàng loạt vào receivable:<br/>amount_due, due_date = ngày due_day của tháng bắt đầu kỳ"]:::be
    B4 --> D7{"Số dòng INSERT khớp<br/>số đã tính?"}
    D7 -- "Không" --> X6("500 IllegalStateException<br/>rollback toàn bộ"):::err
    D7 -- "Có" --> B5["Đánh dấu kỳ: generatedAt = now,<br/>generatedBy = người dùng; lưu"]:::be
    D6 -- "true" --> P1["200 BillingGenerationResponse (dryRun = true)<br/>không ghi dữ liệu"]:::be
    P1 --> P2["Hiển thị bản xem trước: số khoản, số hộ, tổng tiền,<br/>theo từng khoản thu, các hộ bị bỏ qua"]:::fe
    P2 --> D8{"Người dùng<br/>xác nhận lập?"}
    D8 -- "Hủy" --> E(((" "))):::stop
    D8 -- "Xác nhận" --> P3["POST /billing-periods/{id}/generate"]:::fe
    P3 -. "chạy lại toàn bộ kiểm tra" .-> B0
    B5 --> G1["200 BillingGenerationResponse (dryRun = false)"]:::be
    G1 --> G2["Thông báo đã lập danh sách,<br/>GET /billing-periods/{id} để tải lại tổng hợp"]:::fe
    G2 --> E
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```

### 9.19 Đóng sổ và mở lại kỳ thu

*Mã nguồn:* PeriodDetailPage.jsx · BillingPeriodController.updateStatus() · BillingPeriodService.updateStatus() — chỉ vai trò BQT

Thao tác có tính idempotent: gửi lại trạng thái đang có sẽ trả 200 mà không thay đổi gì. Kỳ đã CLOSED thì không lập danh sách được nữa (PERIOD_CLOSED).

```mermaid
flowchart TD
    S((" ")):::start --> A1["BQT bấm Đóng sổ hoặc Mở lại<br/>và xác nhận"]:::user
    A1 --> A2["PATCH /billing-periods/{id}/status<br/>body: status = CLOSED hoặc OPEN"]:::fe
    A2 --> A3["Khóa dòng kỳ thu (FOR UPDATE)"]:::be
    A3 --> D1{"Tìm thấy<br/>kỳ thu?"}
    D1 -- "Không" --> X1("404 NOT_FOUND"):::err
    D1 -- "Có" --> D2{"Trạng thái yêu cầu trùng<br/>trạng thái hiện tại?"}
    D2 -- "Có" --> OK
    D2 -- "Không" --> D3{"Trạng thái<br/>yêu cầu?"}
    D3 -- "CLOSED" --> D4{"Kỳ đã lập<br/>danh sách?"}
    D4 -- "Chưa" --> X2("422 PERIOD_NOT_GENERATED"):::err
    D4 -- "Rồi" --> A4["status = CLOSED, closedAt = now,<br/>closedBy = BQT hiện tại"]:::be
    D3 -- "OPEN" --> A5["status = OPEN,<br/>closedAt = null, closedBy = null"]:::be
    A4 --> OK["200 BillingPeriodResponse"]:::be
    A5 --> OK
    OK --> A6["GET /billing-periods/{id},<br/>hiển thị trạng thái mới"]:::fe
    A6 --> E(((" "))):::stop
    classDef start fill:#1f2937,stroke:#1f2937,color:#ffffff
    classDef stop fill:#ffffff,stroke:#1f2937,stroke-width:3px
    classDef user fill:#e8f0fe,stroke:#3b6fd4,color:#12284d
    classDef fe fill:#e7f6ee,stroke:#2f8f5b,color:#103b25
    classDef be fill:#fff6dd,stroke:#b7791f,color:#4a3204
    classDef err fill:#fdeceb,stroke:#c0392b,color:#6e1710
```