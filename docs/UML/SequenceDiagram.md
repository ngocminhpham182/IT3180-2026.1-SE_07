### 10.1 Đăng nhập và khởi tạo phiên

*Mã nguồn:* LoginPage.jsx · AuthContext.login() · AuthController.login() · AuthService.login() · JwtService.generateToken() · MeController.getMe() · UserService.getMe()

POST /auth/login là endpoint duy nhất permitAll. Sau khi nhận token, FE gọi thêm GET /me để lấy hồ sơ đầy đủ (kèm householdId) trước khi coi là đăng nhập thành công.

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    box rgb(231,246,238) Frontend · React
        participant LP as LoginPage
        participant AC as AuthContext
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as AuthController
        participant S as AuthService
        participant PE as PasswordEncoder<br/>(BCrypt)
        participant J as JwtService
        participant GEH as Global<br/>ExceptionHandler
        participant MC as MeController
        participant US as UserService
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant UR as AppUserRepository
    end

    U->>LP: Nhập username, password và bấm Đăng nhập
    LP->>LP: Kiểm tra đã nhập đủ hai trường
    LP->>+AC: login(credentials)
    AC->>+C: POST /auth/login (skipAuth, không gửi token)
    Note over C: @Valid LoginRequest, sai thì 400 VALIDATION_ERROR
    C->>+S: login(request)
    S->>UR: findByUsernameIgnoreCase(username)
    UR-->>S: Optional AppUser
    break Không có user hoặc user.active = false
        S--xGEH: throw InvalidCredentialsException
        GEH-->>AC: 401 INVALID_CREDENTIALS
        AC-->>LP: throw ApiError
        LP-->>U: Sai tài khoản hoặc mật khẩu
    end
    S->>PE: matches(password, user.passwordHash)
    PE-->>S: true / false
    break Mật khẩu không khớp
        S--xGEH: throw InvalidCredentialsException
        GEH-->>AC: 401 INVALID_CREDENTIALS
        AC-->>LP: throw ApiError
        LP-->>U: Sai tài khoản hoặc mật khẩu
    end
    S->>S: user.setLastLoginAt(now)
    S->>J: generateToken(user)
    J-->>S: JWT (sub = username, userId, roles, iat, exp = iat + 28800 s)
    S-->>-C: LoginResponse(accessToken, Bearer, expiresIn, expiresAt, user)
    Note over S,UR: Kết thúc @Transactional: UPDATE app_user SET last_login_at
    C-->>-AC: 200 ApiResponse(data = LoginResponse)
    AC->>AC: sessionStorage: bluemoon_access_token, bluemoon_expires_at
    AC->>+MC: GET /me (Authorization: Bearer token)
    Note over AC,MC: Đi qua JwtAuthenticationFilter (xem SD-02)
    MC->>+US: getMe()
    US->>UR: findByUsernameIgnoreCase(principal.username)
    UR-->>US: AppUser
    US-->>-MC: UserResponse (kèm roles, residentId, householdId)
    MC-->>-AC: 200 ApiResponse(data = UserResponse)
    AC->>AC: Kiểm tra id, roles, active rồi lưu bluemoon_user, setUser, setToken
    AC-->>-LP: user
    LP->>LP: navigate(homeForRoles(user.roles))
    LP-->>U: Trang chủ theo vai trò
```

### 10.2 Xác thực JWT và phân quyền cho mỗi request

*Mã nguồn:* api/http.js request() · JwtAuthenticationFilter · JwtService · SecurityConfig · GlobalExceptionHandler · AuthContext

Sơ đồ dùng chung cho mọi API cần đăng nhập. Các sơ đồ sau chỉ ghi chú "xem SD-02" thay vì vẽ lại đoạn này.

```mermaid
sequenceDiagram
    autonumber
    box rgb(231,246,238) Frontend · React
        participant P as Trang / Component
        participant H as api/http.js
        participant AC as AuthContext
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant F as JwtAuthentication<br/>Filter
        participant J as JwtService
        participant SC as SecurityFilterChain<br/>(SecurityConfig)
        participant C as Controller
        participant S as Service
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant UR as AppUserRepository
    end

    P->>+H: api.get / post / put / patch / delete(path, body)
    H->>H: Đọc token từ sessionStorage, gắn Authorization: Bearer
    H->>F: HTTP request (timeout 15 s)
    alt Không có header Bearer
        F->>SC: filterChain.doFilter (chưa xác thực)
    else Có Bearer token
        F->>J: isTokenValid(token)
        J-->>F: true / false (chữ ký HMAC, hạn dùng)
        break Token sai chữ ký hoặc hết hạn
            F-->>H: 401 TOKEN_INVALID
        end
        F->>J: extractUsername(token), extractIssuedAt(token)
        J-->>F: username, iat
        F->>UR: findByUsernameIgnoreCase(username)
        UR-->>F: AppUser kèm roles (EAGER)
        break Không có user, user bị khóa, hoặc iat trước passwordChangedAt
            F-->>H: 401 TOKEN_INVALID
        end
        F->>F: SecurityContext.setAuthentication(principal = AppUser, ROLE_x)
        F->>SC: filterChain.doFilter (đã xác thực)
    end
    SC->>SC: Đối chiếu authorizeHttpRequests theo method + path
    break Endpoint cần đăng nhập nhưng chưa xác thực
        SC-->>H: 401 UNAUTHORIZED (authenticationEntryPoint)
    end
    break Không có vai trò được phép
        SC-->>H: 403 FORBIDDEN (accessDeniedHandler)
    end
    SC->>C: Gọi handler (@Valid body, @AuthenticationPrincipal AppUser)
    C->>S: Gọi nghiệp vụ (@Transactional)
    alt Xử lý thành công
        S-->>C: DTO kết quả
        C-->>H: 2xx ApiResponse(success = true, message, data)
        H-->>P: payload.data
    else Service ném exception
        S--xGEH: exception nghiệp vụ / validation
        GEH-->>H: 4xx ApiErrorResponse(code, message, errors, details)
        H-->>P: throw ApiError(code, status, fields, details)
    end
    opt Phản hồi 401 và token gửi đi vẫn là token hiện tại
        H->>AC: dispatchEvent(bluemoon:session-expired)
        AC->>AC: signOut: xóa token, hạn dùng, user khỏi sessionStorage
        AC-->>P: Protected chuyển hướng về /dang-nhap
    end
    deactivate H
```

### 10.3 Tạo tài khoản người dùng

*Mã nguồn:* UserForm.jsx · UserController.createUser() · UserService.createUser() — chỉ vai trò BQT

```mermaid
sequenceDiagram
    autonumber
    actor U as Ban quản trị
    box rgb(231,246,238) Frontend · React
        participant FE as UserForm<br/>(UsersPage)
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant RC as ResidentController
        participant C as UserController
        participant S as UserService
        participant PE as PasswordEncoder
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant UR as AppUserRepository
        participant RR as ResidentRepository
        participant RoR as AppRoleRepository
    end

    U->>FE: Nhập username, mật khẩu, họ tên, chọn vai trò
    opt Có chọn vai trò RESIDENT
        U->>FE: Gõ tên nhân khẩu cần liên kết
        FE->>RC: GET /residents?keyword&status=RESIDING&size=10 (debounce 300 ms)
        RC-->>FE: Danh sách nhân khẩu đang cư trú
        U->>FE: Chọn nhân khẩu (residentId)
    end
    U->>FE: Bấm Lưu
    FE->>FE: validateUser(form)
    FE->>+C: POST /users
    Note over FE,C: Bearer JWT, hasRole(BQT) — xem SD-02. @Valid CreateUserRequest, sai thì 400
    C->>+S: createUser(request)
    S->>UR: existsByUsernameIgnoreCase(username)
    UR-->>S: boolean
    break Username đã tồn tại
        S--xGEH: throw DuplicateException(username)
        GEH-->>FE: 409 DUPLICATE
    end
    opt Có email
        S->>UR: existsByEmailIgnoreCase(email)
        UR-->>S: boolean
    end
    break Email đã tồn tại
        S--xGEH: throw DuplicateException(email)
        GEH-->>FE: 409 DUPLICATE
    end
    break Password trùng username
        S--xGEH: throw IllegalArgumentException
        GEH-->>FE: 400 VALIDATION_ERROR
    end
    break Có vai trò RESIDENT nhưng residentId = null
        S--xGEH: throw ResidentLinkRequiredException
        GEH-->>FE: 422 RESIDENT_LINK_REQUIRED
    end
    opt Có residentId
        S->>RR: findById(residentId)
        RR-->>S: Optional Resident
        break Không tồn tại hoặc status khác RESIDING
            S--xGEH: throw IllegalArgumentException
            GEH-->>FE: 400 VALIDATION_ERROR
        end
        S->>UR: existsByResidentId(residentId)
        UR-->>S: boolean
        break Nhân khẩu đã có tài khoản
            S--xGEH: throw DuplicateException(residentId)
            GEH-->>FE: 409 DUPLICATE
        end
    end
    loop Với mỗi mã vai trò
        S->>RoR: findByCode(roleCode)
        RoR-->>S: AppRole
    end
    S->>PE: encode(password)
    PE-->>S: passwordHash
    S->>UR: save(AppUser: active = true, mustChangePassword = true)
    UR-->>S: AppUser đã có id
    S-->>-C: UserResponse
    C-->>-FE: 201 ApiResponse (Tạo tài khoản thành công)
    FE-->>U: Đóng form, tải lại danh sách tài khoản
```

### 10.4 Quản trị tài khoản: đổi vai trò, khóa / mở khóa, đặt lại mật khẩu

*Mã nguồn:* UsersPage.jsx · UserController · UserService.updateUserRoles() / updateUserStatus() / resetPassword() — chỉ vai trò BQT

Cả ba thao tác đều bắt đầu bằng findById (không có thì 404 NOT_FOUND). Tài khoản bị khóa hoặc bị đặt lại mật khẩu sẽ bị từ chối ngay ở request kế tiếp nhờ kiểm tra trong JwtAuthenticationFilter.

```mermaid
sequenceDiagram
    autonumber
    actor U as Ban quản trị
    box rgb(231,246,238) Frontend · React
        participant FE as UsersPage
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as UserController
        participant S as UserService
        participant PE as PasswordEncoder
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant UR as AppUserRepository
        participant RoR as AppRoleRepository
    end

    Note over FE,C: Bearer JWT, hasRole(BQT) — xem SD-02
    alt Đổi vai trò
        U->>FE: Chọn lại vai trò và lưu
        FE->>FE: canAssignRoles(current, target, roles)
        FE->>+C: PUT /users/{id}/roles
        C->>+S: updateUserRoles(id, request)
        S->>UR: findById(id)
        UR-->>S: AppUser
        break Vai trò bị lặp trong danh sách
            S--xGEH: throw IllegalArgumentException
            GEH-->>FE: 400 VALIDATION_ERROR
        end
        break Có RESIDENT nhưng user.residentId = null
            S--xGEH: throw ResidentLinkRequiredException
            GEH-->>FE: 422 RESIDENT_LINK_REQUIRED
        end
        break BQT tự gỡ vai trò BQT của chính mình
            S--xGEH: throw SelfLockoutException
            GEH-->>FE: 422 SELF_LOCKOUT
        end
        loop Với mỗi mã vai trò
            S->>RoR: findByCode(roleCode)
            RoR-->>S: AppRole
        end
        S->>UR: save(user với tập vai trò mới)
        S-->>-C: UserResponse
        C-->>-FE: 200 ApiResponse (Cập nhật vai trò thành công)
    else Khóa / mở khóa
        U->>FE: Bấm Khóa hoặc Mở khóa và xác nhận
        FE->>+C: PATCH /users/{id}/status (active)
        C->>+S: updateUserStatus(id, request)
        S->>UR: findById(id)
        UR-->>S: AppUser
        break Tự khóa chính mình
            S--xGEH: throw SelfLockoutException
            GEH-->>FE: 422 SELF_LOCKOUT
        end
        S->>UR: save(user.active = request.active)
        S-->>-C: UserResponse
        C-->>-FE: 200 ApiResponse (Cập nhật trạng thái tài khoản thành công)
    else Đặt lại mật khẩu
        U->>FE: Bấm Đặt lại mật khẩu và xác nhận
        FE->>+C: POST /users/{id}/password-reset (body rỗng)
        C->>+S: resetPassword(id, request)
        S->>UR: findById(id)
        UR-->>S: AppUser
        break Đặt lại mật khẩu của chính mình
            S--xGEH: throw SelfResetNotAllowedException
            GEH-->>FE: 422 SELF_RESET_NOT_ALLOWED
        end
        opt newPassword = null
            S->>S: generateTemporaryPassword(): 10 ký tự, SecureRandom
        end
        break Mật khẩu trùng username
            S--xGEH: throw IllegalArgumentException
            GEH-->>FE: 400 VALIDATION_ERROR
        end
        S->>PE: encode(newPassword)
        PE-->>S: passwordHash
        S->>UR: save(passwordHash, mustChangePassword = true, passwordChangedAt = now)
        S-->>-C: PasswordResetResponse(id, username, mật khẩu tạm)
        C-->>-FE: 200 ApiResponse (Đã đặt lại mật khẩu)
        FE-->>U: Hiển thị mật khẩu tạm một lần
    end
    FE-->>U: Tải lại danh sách, thông báo kết quả
```

### 10.5 Đổi mật khẩu

*Mã nguồn:* ProfilePage.jsx · MeController.changePassword() · MeService.changePassword()

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    box rgb(231,246,238) Frontend · React
        participant FE as ProfilePage
        participant AC as AuthContext
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as MeController
        participant S as MeService
        participant PE as PasswordEncoder
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant UR as AppUserRepository
    end

    U->>FE: Nhập mật khẩu cũ, mật khẩu mới, xác nhận
    FE->>FE: validatePassword(form, username)
    FE->>+C: PATCH /me/password
    Note over FE,C: Bearer JWT, authenticated — xem SD-02. @Valid: newPassword 8–128 ký tự, sai thì 400
    C->>+S: changePassword(request)
    S->>UR: findByUsernameIgnoreCase(username từ SecurityContext)
    UR-->>S: AppUser
    S->>PE: matches(oldPassword, passwordHash)
    PE-->>S: true / false
    break Mật khẩu cũ không đúng
        S--xGEH: throw PasswordValidationException(oldPassword)
        GEH-->>FE: 400 VALIDATION_ERROR (errors.oldPassword)
    end
    S->>PE: matches(newPassword, passwordHash)
    PE-->>S: true / false
    break Mật khẩu mới trùng mật khẩu hiện tại
        S--xGEH: throw PasswordValidationException(newPassword)
        GEH-->>FE: 400 VALIDATION_ERROR (errors.newPassword)
    end
    break Mật khẩu mới trùng username
        S--xGEH: throw PasswordValidationException(newPassword)
        GEH-->>FE: 400 VALIDATION_ERROR (errors.newPassword)
    end
    break confirmPassword khác newPassword
        S--xGEH: throw PasswordValidationException(confirmPassword)
        GEH-->>FE: 400 VALIDATION_ERROR (errors.confirmPassword)
    end
    S->>PE: encode(newPassword)
    PE-->>S: passwordHash mới
    S->>UR: save(passwordHash, mustChangePassword = false, passwordChangedAt = now)
    S-->>-C: void
    C-->>-FE: 204 No Content
    FE->>AC: signOut(Đổi mật khẩu thành công, vui lòng đăng nhập lại)
    AC->>AC: Xóa token và user khỏi sessionStorage
    AC-->>U: Chuyển về /dang-nhap kèm thông báo
    Note over AC,UR: Token cũ có iat trước passwordChangedAt nên không còn dùng được
```

### 10.6 Tạo hộ gia đình kèm chủ hộ

*Mã nguồn:* HouseholdForm.jsx · HouseholdController.createHousehold() · HouseholdService.createHousehold() — chỉ vai trò BQT

```mermaid
sequenceDiagram
    autonumber
    actor U as Ban quản trị
    box rgb(231,246,238) Frontend · React
        participant FE as HouseholdForm<br/>(HouseholdsPage)
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as HouseholdController
        participant S as HouseholdService
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant HR as HouseholdRepository
        participant RR as ResidentRepository
        participant MR as ResidentMovement<br/>Repository
    end

    U->>FE: Nhập thông tin hộ và chủ hộ, bấm Lưu
    FE->>FE: validateHousehold(form, creating = true)
    FE->>+C: POST /households
    Note over FE,C: Bearer JWT, hasRole(BQT) — xem SD-02. @Valid CreateHouseholdRequest, sai thì 400
    C->>+S: createHousehold(request, currentUser)
    S->>S: Chuẩn hóa: trim + IN HOA householdCode, apartmentNo
    break Ngày sinh chủ hộ trước 01/01/1900
        S--xGEH: throw IllegalArgumentException
        GEH-->>FE: 400 VALIDATION_ERROR
    end
    S->>HR: existsByHouseholdCodeAndDeletedAtIsNull(code)
    HR-->>S: boolean
    S->>HR: existsByApartmentNoAndDeletedAtIsNull(apartmentNo)
    HR-->>S: boolean
    opt Chủ hộ có CCCD/CMND
        S->>RR: existsByIdentityNumberAndDeletedAtIsNull(identityNumber)
        RR-->>S: boolean
    end
    break Có ít nhất một trường bị trùng
        S--xGEH: throw HouseholdDuplicateException(errors)
        GEH-->>FE: 409 DUPLICATE (errors theo từng trường)
        FE-->>U: Đánh dấu lỗi tại các trường bị trùng
    end
    rect rgb(248,250,252)
        Note over S,MR: Cùng một @Transactional — lỗi ở bước nào cũng rollback cả ba bản ghi
        S->>HR: saveAndFlush(Household: status = ACTIVE)
        HR-->>S: Household đã có id
        S->>RR: saveAndFlush(Resident: head = true, RESIDING, moveInDate = hôm nay)
        RR-->>S: Resident đã có id
        S->>MR: save(ResidentMovement: MOVE_IN, isInitial = true, recordedBy = currentUser)
        MR-->>S: ResidentMovement
    end
    S-->>-C: HouseholdResponse(head, residentCount = 1, presentCount = 1)
    C-->>-FE: 201 ApiResponse (Tạo hộ gia đình thành công)
    FE-->>U: Chuyển tới /ho-gia-dinh/{id}
```

### 10.7 Đổi chủ hộ

*Mã nguồn:* HouseholdDetailPage.jsx · HouseholdController.changeHouseholdHead() · HouseholdService.changeHouseholdHead() — chỉ vai trò BQT

```mermaid
sequenceDiagram
    autonumber
    actor U as Ban quản trị
    box rgb(231,246,238) Frontend · React
        participant FE as HouseholdDetailPage
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as HouseholdController
        participant S as HouseholdService
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant HR as HouseholdRepository
        participant RR as ResidentRepository
    end

    U->>FE: Chọn thành viên làm chủ hộ mới, bấm Đổi chủ hộ
    FE->>+C: PUT /households/{id}/head (residentId)
    Note over FE,C: Bearer JWT, hasRole(BQT) — xem SD-02
    C->>+S: changeHouseholdHead(householdId, residentId)
    S->>HR: findByIdAndDeletedAtIsNull(householdId)
    HR-->>S: Optional Household
    break Không tìm thấy hộ
        S--xGEH: throw ResourceNotFoundException
        GEH-->>FE: 404 NOT_FOUND
    end
    break Hộ không ở trạng thái ACTIVE
        S--xGEH: throw HouseholdInactiveException
        GEH-->>FE: 422 HOUSEHOLD_INACTIVE
    end
    S->>RR: findByIdAndDeletedAtIsNull(residentId)
    RR-->>S: Optional Resident (chủ hộ mới)
    break Không tồn tại, khác hộ, không RESIDING, hoặc đã là chủ hộ
        S--xGEH: throw InvalidNewHeadException(reason)
        GEH-->>FE: 422 INVALID_NEW_HEAD (NOT_FOUND, NOT_IN_HOUSEHOLD, NOT_RESIDING, ALREADY_HEAD)
    end
    S->>RR: findByHouseholdIdAndHeadTrueAndDeletedAtIsNull(householdId)
    RR-->>S: Optional Resident (chủ hộ hiện tại)
    opt Hộ đang có chủ hộ
        S->>RR: saveAndFlush(currentHead.head = false)
    end
    S->>RR: saveAndFlush(newHead.head = true)
    S-->>-C: HouseholdResponse (head mới, residentCount, presentCount)
    C-->>-FE: 200 ApiResponse (Thay đổi chủ hộ thành công)
    FE->>C: GET /households/{id} và GET /households/{id}/residents
    C-->>FE: Chi tiết hộ và danh sách nhân khẩu
    FE-->>U: Đã đổi chủ hộ thành công
```

### 10.8 Thêm nhân khẩu vào hộ

*Mã nguồn:* ResidentForm.jsx · ResidentController.createResident() · ResidentService.createResident() — chỉ vai trò BQT

Cũng chính phương thức createResident này được ResidentRequestService.approve gọi lại khi BQT duyệt đề nghị tạm trú (xem SD-13).

```mermaid
sequenceDiagram
    autonumber
    actor U as Ban quản trị
    box rgb(231,246,238) Frontend · React
        participant FE as ResidentForm<br/>(HouseholdDetailPage)
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as ResidentController
        participant S as ResidentService
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant HR as HouseholdRepository
        participant RR as ResidentRepository
        participant MR as ResidentMovement<br/>Repository
    end

    U->>FE: Nhập thông tin nhân khẩu, hình thức vào hộ, bấm Lưu
    FE->>FE: validateResident(form, creating = true)
    FE->>+C: POST /households/{id}/residents
    Note over FE,C: Bearer JWT, hasRole(BQT) — xem SD-02. @Valid + @AssertTrue của CreateResidentRequest, sai thì 400
    C->>+S: createResident(householdId, request, currentUser)
    S->>HR: findByIdAndDeletedAtIsNull(householdId)
    HR-->>S: Optional Household
    break Không tìm thấy hộ
        S--xGEH: throw ResourceNotFoundException
        GEH-->>FE: 404 NOT_FOUND
    end
    break Hộ không ở trạng thái ACTIVE
        S--xGEH: throw HouseholdInactiveException
        GEH-->>FE: 422 HOUSEHOLD_INACTIVE
    end
    opt Có CCCD/CMND
        S->>RR: existsByIdentityNumberAndDeletedAtIsNull(identityNumber)
        RR-->>S: boolean
    end
    break CCCD/CMND đã tồn tại
        S--xGEH: throw DuplicateException(identityNumber)
        GEH-->>FE: 409 DUPLICATE
    end
    S->>S: Mặc định entryType = MOVE_IN, entryDate = hôm nay
    alt entryType = TEMPORARY_RESIDENCE
        S->>S: status = TEMPORARY_RESIDENCE, statusUntil = endDate, moveInDate = startDate
    else MOVE_IN, BIRTH, EXISTING
        S->>S: status = RESIDING, moveInDate = entryDate
    end
    S->>RR: save(Resident: head = false)
    RR-->>S: Resident đã có id
    opt entryType khác EXISTING
        S->>MR: save(ResidentMovement: loại theo entryType, isInitial = true, recordedBy)
        MR-->>S: ResidentMovement
    end
    S-->>-C: ResidentResponse
    C-->>-FE: 201 ApiResponse (Thêm nhân khẩu thành công)
    FE-->>U: Đóng form, tải lại danh sách nhân khẩu
```

### 10.9 Xóa nhân khẩu (xóa mềm)

*Mã nguồn:* ResidentDetailPage.jsx · ResidentController.deleteResident() · ResidentService.deleteResident() — chỉ vai trò BQT

```mermaid
sequenceDiagram
    autonumber
    actor U as Ban quản trị
    box rgb(231,246,238) Frontend · React
        participant FE as ResidentDetailPage
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as ResidentController
        participant S as ResidentService
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant RR as ResidentRepository
        participant UR as AppUserRepository
        participant MR as ResidentMovement<br/>Repository
    end

    U->>FE: Bấm Xóa nhân khẩu và xác nhận
    FE->>+C: DELETE /residents/{id}
    Note over FE,C: Bearer JWT, hasRole(BQT) — xem SD-02
    C->>+S: deleteResident(id)
    S->>RR: findByIdAndDeletedAtIsNull(id)
    RR-->>S: Optional Resident
    break Không tìm thấy nhân khẩu
        S--xGEH: throw ResourceNotFoundException
        GEH-->>FE: 404 NOT_FOUND
    end
    break Nhân khẩu đang là chủ hộ
        S--xGEH: throw ResidentDeleteException(INVALID_NEW_HEAD)
        GEH-->>FE: 422 INVALID_NEW_HEAD (hãy đổi chủ hộ trước khi xóa)
    end
    S->>UR: existsByResidentId(id)
    UR-->>S: boolean
    break Nhân khẩu đang gắn với tài khoản đăng nhập
        S--xGEH: throw ResidentDeleteException(HAS_ACCOUNT)
        GEH-->>FE: 409 HAS_ACCOUNT
    end
    S->>MR: countByResident_IdAndIsInitialFalse(id)
    MR-->>S: movementCount
    break movementCount lớn hơn 0
        S--xGEH: throw ResidentDeleteException(HAS_HISTORY)
        GEH-->>FE: 409 HAS_HISTORY (suggestedAction = RECORD_MOVEMENT)
        FE-->>U: Gợi ý ghi nhận biến động thay vì xóa
    end
    S->>RR: save(resident.deletedAt = now)
    S-->>-C: void
    C-->>-FE: 204 No Content
    FE-->>U: Chuyển về /ho-gia-dinh/{householdId}
```

### 10.10 Ghi nhận biến động nhân khẩu

*Mã nguồn:* MovementPanel.jsx (MovementForm) · ResidentController.recordMovement() · ResidentService.recordMovement() — chỉ vai trò BQT

Dòng resident bị khóa bằng SELECT ... FOR UPDATE suốt transaction nên hai biến động đồng thời trên cùng một người sẽ được xử lý tuần tự. Khi chủ hộ rời hộ mà chưa chọn người thay, API trả HEAD_REQUIRED kèm danh sách ứng viên để form hiển thị và gửi lại.

```mermaid
sequenceDiagram
    autonumber
    actor U as Ban quản trị
    box rgb(231,246,238) Frontend · React
        participant FE as MovementForm<br/>(MovementPanel)
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as ResidentController
        participant S as ResidentService
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant RR as ResidentRepository
        participant MR as ResidentMovement<br/>Repository
        participant HR as HouseholdRepository
        participant UR as AppUserRepository
    end

    U->>FE: Chọn loại biến động, nhập ngày và thông tin liên quan
    FE->>FE: validateMovement(form, resident, history)
    FE->>+C: POST /residents/{id}/movements
    Note over FE,C: Bearer JWT, hasRole(BQT) — xem SD-02
    C->>+S: recordMovement(id, request, currentUser)
    S->>RR: findByIdForUpdate(id) — SELECT ... FOR UPDATE
    RR-->>S: Optional Resident (đã khóa dòng)
    break Không tìm thấy nhân khẩu
        S--xGEH: throw ResourceNotFoundException
        GEH-->>FE: 404 NOT_FOUND
    end
    break Thiếu loại, loại là BIRTH, note quá 500 ký tự, hoặc thiếu trường bắt buộc theo loại
        S--xGEH: throw AppValidationException(errors)
        GEH-->>FE: 400 VALIDATION_ERROR (errors theo từng trường)
    end
    break Đang MOVED_OUT hoặc DECEASED mà loại khác MOVE_IN
        S--xGEH: throw ResidentNotResidingException
        GEH-->>FE: 422 RESIDENT_NOT_RESIDING
    end
    break Trạng thái hiện tại không thuộc allowedFrom của loại biến động
        S--xGEH: throw InvalidTransitionException(allowedFrom)
        GEH-->>FE: 422 INVALID_TRANSITION
        FE->>C: GET /residents/{id} để lấy trạng thái mới nhất
    end
    break Ngày ở tương lai, trước ngày sinh, hoặc trước moveInDate
        S--xGEH: throw AppValidationException(errors)
        GEH-->>FE: 400 VALIDATION_ERROR
    end
    S->>MR: findFirstByResident_IdOrderByEffectiveDateDesc...(id)
    MR-->>S: Optional ResidentMovement (biến động gần nhất)
    break Ngày hiệu lực trước ngày của biến động gần nhất
        S--xGEH: throw AppValidationException
        GEH-->>FE: 400 VALIDATION_ERROR
    end
    opt Loại là TRANSFER hoặc MOVE_IN
        break TRANSFER tới chính hộ hiện tại
            S--xGEH: throw AppValidationException(newHouseholdId)
            GEH-->>FE: 400 VALIDATION_ERROR
        end
        S->>HR: findByIdAndDeletedAtIsNull(newHouseholdId)
        HR-->>S: Optional Household (hộ đích)
        break Không tìm thấy hộ đích hoặc hộ đích không ACTIVE
            S--xGEH: throw ResourceNotFoundException / HouseholdInactiveException
            GEH-->>FE: 404 NOT_FOUND / 422 HOUSEHOLD_INACTIVE
        end
    end
    opt Nhân khẩu là chủ hộ và loại là MOVE_OUT, DEATH hoặc TRANSFER
        S->>RR: findCandidatesForNewHead(householdId, residentId)
        RR-->>S: Danh sách thành viên khác đang RESIDING
        alt Có ứng viên
            break Request chưa có newHeadResidentId
                S--xGEH: throw HeadRequiredException(candidates)
                GEH-->>FE: 422 HEAD_REQUIRED (details.candidates)
                FE-->>U: Hiển thị danh sách ứng viên để chọn chủ hộ mới và gửi lại
            end
            S->>RR: findByIdAndDeletedAtIsNull(newHeadResidentId)
            RR-->>S: Optional Resident (chủ hộ mới)
            break Trùng người rời hộ, không tồn tại, khác hộ, đã là chủ hộ, hoặc không RESIDING
                S--xGEH: throw InvalidNewHeadException(reason)
                GEH-->>FE: 422 INVALID_NEW_HEAD
            end
            S->>RR: saveAndFlush(resident.head = false)
            S->>RR: saveAndFlush(newHead.head = true)
        else Không có ứng viên
            break Request vẫn gửi newHeadResidentId
                S--xGEH: throw InvalidNewHeadException(NOT_FOUND)
                GEH-->>FE: 422 INVALID_NEW_HEAD
            end
            S->>RR: saveAndFlush(resident.head = false)
        end
    end
    S->>S: Cập nhật resident theo loại: status, household, statusUntil, moveInDate, moveOutDate
    S->>RR: saveAndFlush(resident)
    RR-->>S: Resident sau cập nhật
    opt Loại là MOVE_OUT hoặc DEATH
        S->>UR: findByResidentId(residentId)
        UR-->>S: Optional AppUser
        opt Có tài khoản liên kết
            S->>UR: save(user.active = false)
        end
    end
    S->>MR: save(ResidentMovement: loại, effectiveDate, old/newHousehold, isInitial = false, recordedBy)
    MR-->>S: ResidentMovement
    S-->>-C: RecordMovementResponse(movement, resident)
    C-->>-FE: 201 ApiResponse (Ghi nhận biến động thành công)
    FE-->>U: Cập nhật trạng thái nhân khẩu và lịch sử biến động
```

### 10.11 Tìm kiếm nhanh hộ gia đình và nhân khẩu

*Mã nguồn:* SearchPage.jsx · SearchController.search() · SearchService.search() — vai trò BQT, ACCOUNTANT

```mermaid
sequenceDiagram
    autonumber
    actor U as BQT / Kế toán
    box rgb(231,246,238) Frontend · React
        participant FE as SearchPage
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as SearchController
        participant S as SearchService
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant HR as HouseholdRepository
        participant RR as ResidentRepository
    end

    U->>FE: Gõ từ khóa, chọn loại tìm kiếm
    FE->>FE: Bỏ qua nếu từ khóa ngắn hơn 2 hoặc dài hơn 100 ký tự
    FE->>FE: Debounce 300 ms, abort request trước đó
    FE->>+C: GET /search?keyword&type&limit
    Note over FE,C: Bearer JWT, hasAnyRole(BQT, ACCOUNTANT) — xem SD-02
    C->>+S: search(keyword, type, limit)
    break keyword không dài 2–100 ký tự, type không hợp lệ, hoặc limit ngoài 1–50
        S--xGEH: throw AppValidationException(errors)
        GEH-->>FE: 400 VALIDATION_ERROR
    end
    S->>S: escapeSqlLike(keyword)
    opt type = ALL hoặc HOUSEHOLD
        S->>HR: quickSearchHouseholds(keyword, limit)
        HR-->>S: Danh sách hộ (mã hộ, căn hộ, tầng, trạng thái, tên chủ hộ)
        S->>HR: countQuickSearchHouseholds(keyword)
        HR-->>S: totalHouseholds
    end
    opt type = ALL hoặc RESIDENT
        S->>RR: quickSearchResidents(keyword, limit)
        RR-->>S: Danh sách nhân khẩu kèm thông tin hộ
        S->>RR: countQuickSearchResidents(keyword)
        RR-->>S: totalResidents
        S->>S: Tính statusExpired theo statusUntil và ngày hôm nay
    end
    S->>S: Tổng hợp empty, totals, hasMore
    S-->>-C: SearchResponse(empty, households, residents, totals, hasMore)
    C-->>-FE: 200 ApiResponse(data = SearchResponse)
    FE-->>U: Hiển thị hai nhóm kết quả hoặc thông báo không tìm thấy
```

### 10.12 Cư dân gửi đề nghị (cập nhật hồ sơ, tạm vắng, tạm trú)

*Mã nguồn:* MyHouseholdPage.jsx · ResidentRequestForm.jsx · MeController.getMyHousehold() · ResidentRequestController.submitRequest() · ResidentRequestService.getMyHousehold() / submit() — chỉ vai trò RESIDENT

```mermaid
sequenceDiagram
    autonumber
    actor U as Cư dân
    box rgb(231,246,238) Frontend · React
        participant MP as MyHouseholdPage
        participant FE as ResidentRequestForm
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant MC as MeController
        participant C as ResidentRequest<br/>Controller
        participant S as ResidentRequest<br/>Service
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant RR as ResidentRepository
        participant MR as ResidentMovement<br/>Repository
        participant QR as ResidentRequest<br/>Repository
    end

    U->>MP: Mở trang Hộ của tôi
    MP->>MC: GET /me/household
    Note over MP,MC: Bearer JWT, hasRole(RESIDENT) — xem SD-02
    MC->>S: getMyHousehold(currentUser)
    S->>RR: findByIdAndDeletedAtIsNull(currentUser.residentId)
    RR-->>S: Resident và Household của người dùng
    S->>RR: findByHousehold_IdAndDeletedAtIsNullAndStatusIn(3 trạng thái còn ở hộ)
    RR-->>S: Danh sách thành viên
    S->>QR: findByHousehold_IdAndStatus(householdId, PENDING)
    QR-->>S: Các đề nghị đang chờ
    S-->>MC: MyHouseholdResponse
    MC-->>MP: 200 household, myResidentId, residents, pendingRequests
    U->>MP: Chọn loại đề nghị (và nhân khẩu nếu cần)
    MP->>FE: Mở form theo loại
    U->>FE: Điền thông tin và bấm Gửi
    FE->>FE: validateRequest(body, resident)
    FE->>+C: POST /me/resident-requests
    C->>+S: submit(currentUser, request)
    S->>RR: findByIdAndDeletedAtIsNull(currentUser.residentId)
    RR-->>S: Optional Resident (người gửi)
    break Tài khoản chưa gắn nhân khẩu
        S--xGEH: throw ResidentLinkRequiredException
        GEH-->>FE: 422 RESIDENT_LINK_REQUIRED
    end
    break Body thiếu type
        S--xGEH: throw AppValidationException
        GEH-->>FE: 400 VALIDATION_ERROR
    end
    break Hộ của người gửi không ACTIVE
        S--xGEH: throw HouseholdInactiveException
        GEH-->>FE: 422 HOUSEHOLD_INACTIVE
    end
    alt type = PROFILE_UPDATE
        S->>RR: findByIdAndDeletedAtIsNull(residentId)
        RR-->>S: Optional Resident (đối tượng)
        break Nhân khẩu không thuộc hộ của người gửi
            S--xGEH: throw ResidentNotInHouseholdException
            GEH-->>FE: 403 RESIDENT_NOT_IN_HOUSEHOLD
        end
        S->>S: validateProfileFields (Validator + ngày sinh), sai thì 400
        break Nhân khẩu MOVED_OUT / DECEASED, hoặc không có thay đổi
            S--xGEH: throw ResidentNotResidingException / NoChangesException
            GEH-->>FE: 422 RESIDENT_NOT_RESIDING / NO_CHANGES
        end
        opt Có đổi CCCD/CMND
            S->>RR: existsByIdentityNumberAndDeletedAtIsNullAndIdNot(CCCD mới, id)
            RR-->>S: boolean
            break CCCD mới trùng người khác
                S--xGEH: throw DuplicateException
                GEH-->>FE: 409 DUPLICATE
            end
        end
        S->>QR: findFirstByResident_IdAndTypeAndStatus(id, PROFILE_UPDATE, PENDING)
        QR-->>S: Optional ResidentRequest
        S->>S: proposed = hồ sơ đề nghị, beforeValues = hồ sơ hiện tại
    else type = TEMPORARY_ABSENCE
        S->>RR: findByIdAndDeletedAtIsNull(residentId)
        RR-->>S: Optional Resident (đối tượng)
        break Nhân khẩu không thuộc hộ của người gửi
            S--xGEH: throw ResidentNotInHouseholdException
            GEH-->>FE: 403 RESIDENT_NOT_IN_HOUSEHOLD
        end
        S->>MR: findFirstByResident_IdOrderByEffectiveDateDesc...(id)
        MR-->>S: Biến động gần nhất (để kiểm tra startDate)
        S->>S: validateAbsenceDates, sai thì 400
        break Nhân khẩu không ở trạng thái RESIDING
            S--xGEH: throw ResidentNotResidingException / InvalidTransitionException
            GEH-->>FE: 422 RESIDENT_NOT_RESIDING / INVALID_TRANSITION
        end
        S->>QR: findFirstByResident_IdAndTypeAndStatus(id, TEMPORARY_ABSENCE, PENDING)
        QR-->>S: Optional ResidentRequest
        S->>S: proposed = startDate, endDate
    else type = TEMPORARY_RESIDENCE
        S->>S: validateResidenceFields (ngày + thông tin khách), sai thì 400
        S->>RR: existsByIdentityNumberAndDeletedAtIsNull(CCCD của khách)
        RR-->>S: boolean
        break CCCD của khách đã có trong hệ thống
            S--xGEH: throw DuplicateException
            GEH-->>FE: 409 DUPLICATE
        end
        S->>QR: findPendingTemporaryResidenceIdByIdentityNumber(CCCD)
        QR-->>S: Optional id
        S->>S: proposed = startDate, endDate, person
    end
    break Đã có đề nghị cùng loại đang PENDING
        S--xGEH: throw PendingRequestExistsException(requestId)
        GEH-->>FE: 409 PENDING_REQUEST_EXISTS
        FE-->>U: Đưa liên kết tới đề nghị đang chờ
    end
    S->>QR: saveAndFlush(ResidentRequest: PENDING, submittedBy, submittedAt, proposed, beforeValues)
    QR-->>S: ResidentRequest đã có id
    Note over S,QR: Nếu DB báo DataIntegrityViolation (gửi đồng thời) thì cũng trả 409 PENDING_REQUEST_EXISTS
    S-->>-C: ResidentRequestResponse
    C-->>-FE: 201 ApiResponse (Đã gửi đề nghị, đang chờ Ban quản trị duyệt)
    FE-->>U: Chuyển tới /de-nghi/{id}
```

### 10.13 Ban quản trị duyệt đề nghị

*Mã nguồn:* ResidentRequestDetailPage.jsx · ResidentRequestController.approveRequest() · ResidentRequestService.approve() · ResidentService.recordMovement() / createResident() — chỉ vai trò BQT

ResidentRequestService gọi lại ResidentService trong cùng transaction, nên việc duyệt tái sử dụng toàn bộ kiểm tra của luồng ghi nhận biến động (SD-10) và thêm nhân khẩu (SD-08).

```mermaid
sequenceDiagram
    autonumber
    actor U as Ban quản trị
    box rgb(231,246,238) Frontend · React
        participant FE as ResidentRequest<br/>DetailPage
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as ResidentRequest<br/>Controller
        participant S as ResidentRequest<br/>Service
        participant RS as ResidentService
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant QR as ResidentRequest<br/>Repository
        participant RR as ResidentRepository
    end

    U->>FE: Mở chi tiết đề nghị
    FE->>C: GET /resident-requests/{id}
    C-->>FE: 200 ResidentRequestResponse (proposed, before, changes)
    U->>FE: Nhập ghi chú (tùy chọn) và xác nhận Duyệt
    FE->>+C: POST /resident-requests/{id}/approve (note)
    Note over FE,C: Bearer JWT, hasRole(BQT) — xem SD-02
    C->>+S: approve(id, approveRequest, bqtUser)
    S->>QR: findByIdForUpdate(id) — SELECT ... FOR UPDATE
    QR-->>S: Optional ResidentRequest (đã khóa dòng)
    break Không tìm thấy đề nghị
        S--xGEH: throw ResourceNotFoundException
        GEH-->>FE: 404 NOT_FOUND
    end
    break Trạng thái khác PENDING
        S--xGEH: throw RequestNotPendingException(status)
        GEH-->>FE: 409 REQUEST_NOT_PENDING
        FE->>C: GET /resident-requests/{id} để tải trạng thái mới nhất
    end
    break Hộ của đề nghị không ACTIVE
        S--xGEH: throw HouseholdInactiveException
        GEH-->>FE: 422 HOUSEHOLD_INACTIVE
    end
    alt type = PROFILE_UPDATE
        S->>RR: findByIdForUpdate(residentId) — SELECT ... FOR UPDATE
        RR-->>S: Optional Resident (đã khóa dòng)
        S->>S: calculateChanges(before, proposed), findOutdatedFields(resident, before, changes)
        break Nhân khẩu không còn, hoặc trường cần đổi đã khác beforeValues
            S--xGEH: throw RequestOutdatedException(outdatedFields)
            GEH-->>FE: 409 REQUEST_OUTDATED
            FE-->>U: Báo dữ liệu đã thay đổi, khóa nút Duyệt
        end
        opt Có đổi CCCD/CMND
            S->>RR: existsByIdentityNumberAndDeletedAtIsNullAndIdNot(CCCD mới, id)
            RR-->>S: boolean
            break CCCD mới trùng người khác
                S--xGEH: throw DuplicateException
                GEH-->>FE: 409 DUPLICATE
            end
        end
        S->>S: applyProfileChanges(resident, proposed, changes)
        S->>RR: save(resident)
    else type = TEMPORARY_ABSENCE
        S->>S: Dựng CreateMovementRequest(TEMPORARY_ABSENCE, startDate, endDate, note)
        S->>+RS: recordMovement(residentId, movementRequest, bqtUser)
        Note over RS,RR: Chạy toàn bộ luồng SD-10: khóa resident, kiểm tra chuyển trạng thái và ngày, lưu biến động
        break recordMovement ném lỗi 400 / 422
            RS--xGEH: exception của luồng biến động
            GEH-->>FE: Lỗi tương ứng, đề nghị vẫn PENDING (rollback)
        end
        RS-->>-S: RecordMovementResponse (movement.id)
    else type = TEMPORARY_RESIDENCE
        S->>RR: existsByIdentityNumberAndDeletedAtIsNull(CCCD của khách)
        RR-->>S: boolean
        break CCCD của khách đã có trong hệ thống
            S--xGEH: throw DuplicateException
            GEH-->>FE: 409 DUPLICATE
        end
        S->>+RS: createResident(householdId, CreateResidentRequest(TEMPORARY_RESIDENCE), bqtUser)
        Note over RS,RR: Chạy luồng SD-08: tạo Resident TEMPORARY_RESIDENCE và biến động ban đầu
        RS-->>-S: ResidentResponse (id nhân khẩu mới)
    end
    S->>QR: save(status = APPROVED, decidedBy, decidedAt, decisionNote, resultResidentId, resultMovementId)
    QR-->>S: ResidentRequest
    S-->>-C: ResidentRequestResponse (decision, result)
    C-->>-FE: 200 ApiResponse (Đã duyệt đề nghị)
    FE-->>U: Hiển thị trạng thái Đã duyệt
```

### 10.14 Từ chối đề nghị (BQT) và hủy đề nghị (cư dân)

*Mã nguồn:* ResidentRequestDetailPage.jsx · ResidentRequestController.rejectRequest() / cancelMyRequest() · ResidentRequestService.reject() / cancel()

```mermaid
sequenceDiagram
    autonumber
    actor U as BQT / Cư dân
    box rgb(231,246,238) Frontend · React
        participant FE as ResidentRequest<br/>DetailPage
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as ResidentRequest<br/>Controller
        participant S as ResidentRequest<br/>Service
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant QR as ResidentRequest<br/>Repository
    end

    alt BQT từ chối
        U->>FE: Nhập lý do từ chối và xác nhận
        FE->>+C: POST /resident-requests/{id}/reject (reason)
        Note over FE,C: hasRole(BQT) — xem SD-02. @Valid RejectRequest: reason bắt buộc, tối đa 500 ký tự
        C->>+S: reject(id, rejectRequest, bqtUser)
        S->>QR: findByIdForUpdate(id) — SELECT ... FOR UPDATE
        QR-->>S: Optional ResidentRequest
        break Không tìm thấy đề nghị
            S--xGEH: throw ResourceNotFoundException
            GEH-->>FE: 404 NOT_FOUND
        end
        break Trạng thái khác PENDING
            S--xGEH: throw RequestNotPendingException(status)
            GEH-->>FE: 409 REQUEST_NOT_PENDING
        end
        S->>QR: save(status = REJECTED, decidedBy = bqtUser, decidedAt, decisionReason)
        S-->>-C: ResidentRequestResponse
        C-->>-FE: 200 ApiResponse (Đã từ chối đề nghị)
    else Cư dân hủy
        U->>FE: Bấm Hủy đề nghị và xác nhận
        FE->>+C: POST /me/resident-requests/{id}/cancel
        Note over FE,C: hasRole(RESIDENT) — xem SD-02
        C->>+S: cancel(id, currentUser)
        S->>QR: findByIdForUpdate(id) — SELECT ... FOR UPDATE
        QR-->>S: Optional ResidentRequest
        break Không tìm thấy, hoặc không phải do người này gửi
            S--xGEH: throw ResourceNotFoundException
            GEH-->>FE: 404 NOT_FOUND
        end
        break Trạng thái khác PENDING
            S--xGEH: throw RequestNotPendingException(status)
            GEH-->>FE: 409 REQUEST_NOT_PENDING
        end
        S->>QR: save(status = CANCELLED, decidedBy = currentUser, decidedAt)
        S-->>-C: ResidentRequestResponse
        C-->>-FE: 200 ApiResponse (Đã hủy đề nghị)
    end
    FE-->>U: Hiển thị trạng thái mới của đề nghị
    opt Nhận 409 REQUEST_NOT_PENDING
        FE->>C: GET lại chi tiết đề nghị
        C-->>FE: Trạng thái do bên kia vừa quyết định
    end
```

### 10.15 Tạo và cập nhật khoản thu

*Mã nguồn:* FeeForm.jsx · FeeCategoryController.create() / update() · FeeCategoryService.create() / update() — vai trò ACCOUNTANT, BQT

```mermaid
sequenceDiagram
    autonumber
    actor U as Kế toán / BQT
    box rgb(231,246,238) Frontend · React
        participant FE as FeeForm<br/>(FeesPage)
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as FeeCategory<br/>Controller
        participant S as FeeCategory<br/>Service
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant FR as FeeCategory<br/>Repository
        participant VR as ReceivableRepository
    end

    U->>FE: Nhập thông tin khoản thu và bấm Lưu
    FE->>FE: validateFee(form)
    Note over FE,C: Bearer JWT, hasAnyRole(ACCOUNTANT, BQT) — xem SD-02
    alt Tạo mới
        FE->>+C: POST /fee-categories
        C->>+S: create(request)
        S->>S: Chuẩn hóa: code trim + IN HOA, name trim
        S->>FR: existsByCode(code)
        FR-->>S: boolean
        break Mã khoản thu đã tồn tại
            S--xGEH: throw FeeCategoryDuplicateException
            GEH-->>FE: 409 DUPLICATE
        end
        S->>S: Kiểm tra theo feeKind (MANDATORY: PER_AREA/FIXED, đơn giá, chu kỳ, hạn nộp 1–28. VOLUNTARY: MANUAL)
        break Có lỗi trong map errors
            S--xGEH: throw FeeCategoryValidationException(errors)
            GEH-->>FE: 400 VALIDATION_ERROR (errors theo từng trường)
        end
        S->>FR: save(FeeCategory: active = true)
        FR-->>S: FeeCategory đã có id
        S-->>-C: FeeCategoryResponse(inUse = false)
        C-->>-FE: 201 ApiResponse (Tạo danh mục khoản thu thành công)
    else Cập nhật
        FE->>+C: PUT /fee-categories/{id}
        C->>+S: update(id, request)
        S->>FR: findById(id)
        FR-->>S: Optional FeeCategory
        break Không tìm thấy khoản thu
            S--xGEH: throw ResourceNotFoundException
            GEH-->>FE: 404 NOT_FOUND
        end
        S->>S: Chuẩn hóa và kiểm tra như khi tạo
        break Có lỗi trong map errors
            S--xGEH: throw FeeCategoryValidationException(errors)
            GEH-->>FE: 400 VALIDATION_ERROR
        end
        S->>FR: existsByCodeIgnoreCaseAndIdNot(code, id)
        FR-->>S: boolean
        break Mã trùng với khoản thu khác
            S--xGEH: throw FeeCategoryDuplicateException
            GEH-->>FE: 409 DUPLICATE
        end
        S->>VR: existsByFeeCategory_Id(id)
        VR-->>S: inUse
        break inUse và có đổi code, feeKind, calculationMethod hoặc billingCycle
            S--xGEH: throw HasTransactionsException(errors)
            GEH-->>FE: 409 HAS_TRANSACTIONS (suggestedAction = CREATE_NEW)
        end
        S->>FR: save(category với các trường mới)
        FR-->>S: FeeCategory
        S-->>-C: FeeCategoryResponse(inUse)
        C-->>-FE: 200 ApiResponse (Cập nhật khoản thu thành công)
    end
    FE-->>U: Đóng form, tải lại danh mục khoản thu
```

### 10.16 Tính thử phí cho một hộ

*Mã nguồn:* FeePreview.jsx · FeeCalculationController.preview() · FeeCalculationService.preview() — vai trò ACCOUNTANT, BQT

```mermaid
sequenceDiagram
    autonumber
    actor U as Kế toán / BQT
    box rgb(231,246,238) Frontend · React
        participant FE as FeePreview<br/>(FeesPage)
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as FeeCalculation<br/>Controller
        participant S as FeeCalculation<br/>Service
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant HR as HouseholdRepository
        participant FR as FeeCategory<br/>Repository
    end

    U->>FE: Mở Tính thử trên một khoản thu, chọn hộ
    FE->>+C: POST /fee-calculation/preview (householdId, feeCategoryId)
    Note over FE,C: Bearer JWT, hasAnyRole(ACCOUNTANT, BQT) — xem SD-02. @Valid: hai id bắt buộc và lớn hơn 0
    C->>+S: preview(request)
    S->>HR: findByIdAndDeletedAtIsNull(householdId)
    HR-->>S: Optional Household
    break Không tìm thấy hộ
        S--xGEH: throw ResourceNotFoundException
        GEH-->>FE: 404 NOT_FOUND
    end
    break Hộ không ACTIVE
        S--xGEH: throw HouseholdInactiveException
        GEH-->>FE: 422 HOUSEHOLD_INACTIVE
    end
    S->>FR: findById(feeCategoryId)
    FR-->>S: Optional FeeCategory
    break Không tìm thấy khoản thu
        S--xGEH: throw ResourceNotFoundException
        GEH-->>FE: 404 NOT_FOUND
    end
    break Khoản thu đã ngừng sử dụng
        S--xGEH: throw FeeInactiveException
        GEH-->>FE: 422 FEE_INACTIVE
    end
    break Khoản VOLUNTARY hoặc cách tính MANUAL
        S--xGEH: throw FeeNotBillableException
        GEH-->>FE: 422 FEE_NOT_BILLABLE
    end
    alt calculationMethod = PER_AREA
        break Hộ chưa có diện tích
            S--xGEH: throw MissingAreaException(householdId)
            GEH-->>FE: 422 MISSING_AREA
        end
        S->>S: amount = round(area × unitPrice, 0, HALF_UP)
    else calculationMethod = FIXED
        S->>S: amount = round(unitPrice, 0, HALF_UP)
    end
    S-->>-C: FeeCalculationPreviewResponse(area, unitPrice, amount, formula)
    C-->>-FE: 200 ApiResponse(data = preview)
    Note over S,FR: Transaction readOnly, không ghi dữ liệu
    FE-->>U: Hiển thị công thức và số tiền
```

### 10.17 Quản lý kỳ thu: tạo, sửa, xóa

*Mã nguồn:* PeriodForm.jsx · PeriodDetailPage.jsx · BillingPeriodController · BillingPeriodService.create() / update() / delete() — vai trò ACCOUNTANT, BQT

```mermaid
sequenceDiagram
    autonumber
    actor U as Kế toán / BQT
    box rgb(231,246,238) Frontend · React
        participant FE as PeriodForm /<br/>PeriodDetailPage
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as BillingPeriod<br/>Controller
        participant S as BillingPeriod<br/>Service
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant PR as BillingPeriod<br/>Repository
    end

    Note over FE,C: Bearer JWT, hasAnyRole(ACCOUNTANT, BQT) — xem SD-02
    alt Tạo kỳ thu
        U->>FE: Nhập mã, tên, ngày bắt đầu, ngày kết thúc
        FE->>FE: validatePeriod(form)
        FE->>+C: POST /billing-periods
        C->>+S: create(request)
        break endDate trước startDate
            S--xGEH: throw InvalidBillingPeriodDateRangeException
            GEH-->>FE: 400 VALIDATION_ERROR
        end
        S->>PR: existsByCode(code)
        PR-->>S: boolean
        break Mã kỳ thu đã tồn tại
            S--xGEH: throw DuplicateException(code)
            GEH-->>FE: 409 DUPLICATE
        end
        S->>PR: findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(endDate, startDate)
        PR-->>S: Optional BillingPeriod (kỳ bị chồng lấn)
        break Khoảng ngày chồng lấn kỳ khác
            S--xGEH: throw BillingPeriodOverlapException(id, code)
            GEH-->>FE: 422 PERIOD_OVERLAP
        end
        S->>PR: save(BillingPeriod: status = OPEN, generatedAt = null)
        PR-->>S: BillingPeriod đã có id
        S-->>-C: BillingPeriodResponse
        C-->>-FE: 201 ApiResponse (Tạo kỳ thu thành công)
        FE-->>U: Chuyển tới /ky-thu/{id}
    else Sửa kỳ thu
        U->>FE: Sửa mã, tên hoặc khoảng ngày
        FE->>+C: PUT /billing-periods/{id}
        C->>+S: update(id, request)
        S->>PR: findByIdForUpdate(id) — SELECT ... FOR UPDATE
        PR-->>S: Optional BillingPeriod
        break Không tìm thấy kỳ thu
            S--xGEH: throw ResourceNotFoundException
            GEH-->>FE: 404 NOT_FOUND
        end
        break Kỳ đã lập danh sách (generatedAt khác null)
            S--xGEH: throw BillingPeriodAlreadyGeneratedException
            GEH-->>FE: 409 ALREADY_GENERATED
        end
        break endDate trước startDate
            S--xGEH: throw InvalidBillingPeriodDateRangeException
            GEH-->>FE: 400 VALIDATION_ERROR
        end
        S->>PR: existsByCodeAndIdNot(code, id)
        PR-->>S: boolean
        break Mã trùng với kỳ thu khác
            S--xGEH: throw DuplicateException(code)
            GEH-->>FE: 409 DUPLICATE
        end
        S->>PR: findOverlappingPeriod(id, startDate, endDate)
        PR-->>S: Optional BillingPeriod
        break Khoảng ngày chồng lấn kỳ khác
            S--xGEH: throw BillingPeriodOverlapException(id, code)
            GEH-->>FE: 422 PERIOD_OVERLAP
        end
        S->>S: Gán code, name, startDate, endDate (entity managed, tự UPDATE khi commit)
        S-->>-C: BillingPeriodResponse
        C-->>-FE: 200 ApiResponse (Cập nhật kỳ thu thành công)
    else Xóa kỳ thu
        U->>FE: Bấm Xóa và xác nhận
        FE->>+C: DELETE /billing-periods/{id}
        C->>+S: delete(id)
        S->>PR: findByIdForUpdate(id) — SELECT ... FOR UPDATE
        PR-->>S: Optional BillingPeriod
        break Không tìm thấy kỳ thu
            S--xGEH: throw ResourceNotFoundException
            GEH-->>FE: 404 NOT_FOUND
        end
        break Kỳ đã lập danh sách
            S--xGEH: throw BillingPeriodAlreadyGeneratedException
            GEH-->>FE: 409 ALREADY_GENERATED
        end
        S->>PR: delete(period)
        S-->>-C: void
        C-->>-FE: 204 No Content
        FE-->>U: Chuyển về /ky-thu
    end
```

### 10.18 Lập danh sách khoản phải thu cho kỳ thu

*Mã nguồn:* PeriodDetailPage.jsx perform() · BillingGenerationController.generate() · BillingGenerationService.generate() · ReceivableRepository — vai trò ACCOUNTANT, BQT

Hai lần gọi (xem trước và lập thật) đi qua cùng một phương thức generate. Khóa trên kỳ thu và trên các khoản thu MANDATORY bảo đảm hai người bấm cùng lúc không sinh trùng khoản phải thu và khoản ONE_TIME chỉ được tính một lần.

```mermaid
sequenceDiagram
    autonumber
    actor U as Kế toán / BQT
    box rgb(231,246,238) Frontend · React
        participant FE as PeriodDetailPage
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as BillingGeneration<br/>Controller
        participant S as BillingGeneration<br/>Service
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant PR as BillingPeriod<br/>Repository
        participant VR as ReceivableRepository
    end

    Note over FE,C: Bearer JWT, hasAnyRole(ACCOUNTANT, BQT) — xem SD-02
    U->>FE: Bấm Xem trước
    FE->>+C: POST /billing-periods/{id}/generate?dryRun=true
    C->>+S: generate(id, dryRun = true, currentUser)
    Note over S,VR: Chạy các bước kiểm tra và tính toán như lần lập thật bên dưới, không INSERT
    S-->>-C: BillingGenerationResponse(dryRun = true)
    C-->>-FE: 200 generatedCount, householdCount, totalAmount, byFee, skipped, inactiveHouseholdCount
    FE-->>U: Hiển thị bản xem trước và nút Xác nhận lập
    U->>FE: Xác nhận lập danh sách
    FE->>+C: POST /billing-periods/{id}/generate
    C->>+S: generate(id, dryRun = false, currentUser)
    S->>PR: findByIdForUpdate(id) — SELECT ... FOR UPDATE
    PR-->>S: Optional BillingPeriod (đã khóa dòng)
    break Không tìm thấy kỳ thu
        S--xGEH: throw ResourceNotFoundException
        GEH-->>FE: 404 NOT_FOUND
    end
    break Kỳ đã CLOSED
        S--xGEH: throw BillingPeriodClosedException
        GEH-->>FE: 422 PERIOD_CLOSED
    end
    break Kỳ đã lập danh sách
        S--xGEH: throw BillingPeriodAlreadyGeneratedException
        GEH-->>FE: 409 ALREADY_GENERATED
        FE->>C: GET /billing-periods/{id} để đồng bộ trạng thái
    end
    S->>VR: lockActiveMandatoryFeeCategories() — SELECT ... FOR UPDATE
    VR-->>S: Danh sách id khoản thu đã khóa
    S->>VR: countEligibleFees()
    VR-->>S: Số khoản thu đủ điều kiện
    break Không có khoản thu đủ điều kiện
        S--xGEH: throw NoActiveFeeException
        GEH-->>FE: 422 NO_ACTIVE_FEE
    end
    S->>VR: getGenerationAggregates()
    VR-->>S: Theo từng khoản thu: receivableCount, totalAmount
    S->>VR: getSkippedReceivables()
    VR-->>S: Các cặp hộ – khoản thu PER_AREA thiếu diện tích
    break Tổng receivableCount = 0
        S--xGEH: throw NothingToGenerateException(skipped)
        GEH-->>FE: 422 NOTHING_TO_GENERATE (details.skipped)
    end
    S->>VR: countHouseholdsToGenerate(), countInactiveHouseholds()
    VR-->>S: householdCount, inactiveHouseholdCount
    S->>VR: generateReceivables(periodId, userId) — INSERT ... SELECT hộ ACTIVE × khoản thu đủ điều kiện
    VR-->>S: insertedCount
    break insertedCount khác generatedCount
        S--xC: throw IllegalStateException (không có handler riêng)
        C-->>FE: 500 Internal Server Error, rollback toàn bộ
    end
    S->>PR: save(period.generatedAt = now, generatedBy = currentUser)
    S-->>-C: BillingGenerationResponse(dryRun = false)
    C-->>-FE: 200 ApiResponse (Lập danh sách khoản thu thành công)
    FE->>C: GET /billing-periods/{id}
    C-->>FE: BillingPeriodDetailResponse (kèm summary)
    FE-->>U: Đã lập danh sách khoản phải thu
    opt Lỗi TIMEOUT, NETWORK_ERROR hoặc 5xx ở lần lập thật
        FE->>C: GET /billing-periods/{id} (reconcile)
        C-->>FE: generated = true / false
        FE-->>U: Báo đúng tình trạng kỳ trước khi cho lập lại
    end
```

### 10.19 Đóng sổ và mở lại kỳ thu

*Mã nguồn:* PeriodDetailPage.jsx · BillingPeriodController.updateStatus() · BillingPeriodService.updateStatus() — chỉ vai trò BQT

```mermaid
sequenceDiagram
    autonumber
    actor U as Ban quản trị
    box rgb(231,246,238) Frontend · React
        participant FE as PeriodDetailPage
    end
    box rgb(255,246,221) Backend · Spring Boot
        participant C as BillingPeriod<br/>Controller
        participant S as BillingPeriod<br/>Service
        participant GEH as Global<br/>ExceptionHandler
    end
    box rgb(241,245,249) Spring Data JPA / PostgreSQL
        participant PR as BillingPeriod<br/>Repository
    end

    U->>FE: Bấm Đóng sổ hoặc Mở lại và xác nhận
    FE->>+C: PATCH /billing-periods/{id}/status (status)
    Note over FE,C: Bearer JWT, hasRole(BQT) — xem SD-02
    C->>+S: updateStatus(id, request, currentUser)
    S->>PR: findByIdForUpdate(id) — SELECT ... FOR UPDATE
    PR-->>S: Optional BillingPeriod (đã khóa dòng)
    break Không tìm thấy kỳ thu
        S--xGEH: throw ResourceNotFoundException
        GEH-->>FE: 404 NOT_FOUND
    end
    alt Trạng thái yêu cầu trùng trạng thái hiện tại
        S->>S: Không thay đổi gì
    else Yêu cầu CLOSED
        break Kỳ chưa lập danh sách (generatedAt = null)
            S--xGEH: throw PeriodNotGeneratedException
            GEH-->>FE: 422 PERIOD_NOT_GENERATED
        end
        S->>S: status = CLOSED, closedAt = now, closedBy = currentUser
    else Yêu cầu OPEN
        S->>S: status = OPEN, closedAt = null, closedBy = null
    end
    Note over S,PR: Entity đang managed nên UPDATE billing_period tự chạy khi commit
    S-->>-C: BillingPeriodResponse
    C-->>-FE: 200 ApiResponse (Cập nhật trạng thái kỳ thu thành công)
    FE->>C: GET /billing-periods/{id}
    C-->>FE: BillingPeriodDetailResponse
    FE-->>U: Hiển thị trạng thái mới của kỳ thu
```
