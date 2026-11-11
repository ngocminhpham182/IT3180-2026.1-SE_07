package com.example.bluemoon.service;

import com.example.bluemoon.dto.CreateUserRequest;
import com.example.bluemoon.dto.RoleResponse;
import com.example.bluemoon.dto.UserResponse;
import com.example.bluemoon.exception.DuplicateException;
import com.example.bluemoon.exception.ResidentLinkRequiredException;
import com.example.bluemoon.exception.ResourceNotFoundException;
import com.example.bluemoon.model.AppRole;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.model.Resident;
import com.example.bluemoon.model.enums.ResidencyStatus;
import com.example.bluemoon.model.enums.RoleCode;
import com.example.bluemoon.repository.AppRoleRepository;
import com.example.bluemoon.repository.AppUserRepository;
import com.example.bluemoon.repository.ResidentRepository;
import com.example.bluemoon.dto.PageResponse;
import com.example.bluemoon.repository.specification.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bluemoon.dto.UpdateUserRolesRequest;
import com.example.bluemoon.exception.SelfLockoutException;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.bluemoon.dto.UpdateUserStatusRequest;
import com.example.bluemoon.dto.PasswordResetResponse;
import com.example.bluemoon.dto.ResetPasswordRequest;
import com.example.bluemoon.exception.SelfResetNotAllowedException;

import java.security.SecureRandom;
import java.time.Instant;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        // Username phải duy nhất
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateException(
                    "username",
                    "Username đã tồn tại"
            );
        }

        // 2. Email unique nếu có gửi
        if (request.email() != null
                && !request.email().isBlank()
                && userRepository.existsByEmailIgnoreCase(request.email())) {

            throw new DuplicateException(
                    "email",
                    "Email đã tồn tại"
            );
        }

        // 3. Password không được trùng username
        if (request.password().equalsIgnoreCase(request.username())) {
            throw new IllegalArgumentException(
                    "Password không được trùng với username"
            );
        }

        // 4. Có role RESIDENT thì bắt buộc residentId
        boolean isResident = request.roles()
                .contains(RoleCode.RESIDENT);

        if (isResident && request.residentId() == null) {
            throw new ResidentLinkRequiredException();
        }

        // 5. Nếu có residentId thì kiểm tra nhân khẩu
        if (request.residentId() != null) {

            Resident resident = residentRepository
                    .findById(request.residentId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Nhân khẩu không tồn tại"
                            )
                    );

            if (resident.getStatus()
                    != ResidencyStatus.RESIDING) {

                throw new IllegalArgumentException(
                        "Nhân khẩu phải đang cư trú"
                );
            }


            if (userRepository.existsByResidentId(request.residentId())) {
                throw new DuplicateException(
                        "residentId",
                        "Nhân khẩu này đã có tài khoản"
                );
            }
        }

        Set<AppRole> roles = new HashSet<>();

        for (var roleCode : request.roles()) {
            AppRole role = roleRepository
                    .findByCode(roleCode.name())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Role không tồn tại: " + roleCode
                            )
                    );

            roles.add(role);
        }

        AppUser user = AppUser.builder()
                .username(request.username())
                .passwordHash(
                        passwordEncoder.encode(request.password())
                )
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .residentId(request.residentId())
                .active(true)
                .mustChangePassword(true)
                .roles(roles)
                .build();

        user = userRepository.save(user);

        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {

        return roleRepository.findAll()
                .stream()
                .map(role -> new RoleResponse(
                        role.getCode(),
                        role.getName()
                ))
                .toList();
    }

    private UserResponse toUserResponse(AppUser user) {

        List<String> roles = user.getRoles()
                .stream()
                .map(AppRole::getCode)
                .sorted()
                .toList();
        Long householdId = null;

        if (user.getResidentId() != null) {
            householdId = residentRepository
                    .findById(user.getResidentId())
                    .map(resident -> resident.getHousehold().getId())
                    .orElse(null);
        }

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getResidentId(),
                householdId,
                user.isActive(),
                roles,
                user.isMustChangePassword()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(
            String keyword,
            String role,
            Boolean active,
            int page,
            int size
    ) {
        Specification<AppUser> specification =
                Specification.allOf(
                        UserSpecification.hasKeyword(keyword),
                        UserSpecification.hasRole(role),
                        UserSpecification.hasActive(active)
                );

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.ASC, "username")
        );

        Page<AppUser> userPage =
                userRepository.findAll(specification, pageable);

        List<UserResponse> users = userPage
                .getContent()
                .stream()
                .map(this::toUserResponse)
                .toList();

        return new PageResponse<>(
                users,
                page,
                size,
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        AppUser user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy tài khoản"
                        )
                );

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserRoles(
            Long id,
            UpdateUserRolesRequest request
    ) {

        AppUser user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy tài khoản"
                        )
                );

        // Kiểm tra role bị trùng
        long distinctRoleCount = request.roles()
                .stream()
                .distinct()
                .count();

        if (distinctRoleCount != request.roles().size()) {
            throw new IllegalArgumentException(
                    "Roles không được trùng nhau"
            );
        }

        boolean hasResidentRole = request.roles()
                .contains(RoleCode.RESIDENT);

        // Thêm RESIDENT nhưng chưa gắn nhân khẩu
        if (hasResidentRole && user.getResidentId() == null) {
            throw new ResidentLinkRequiredException();
        }

        // Username của người đang thực hiện request
        String currentUsername = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        boolean isCurrentUser = user.getUsername()
                .equalsIgnoreCase(currentUsername);

        boolean currentlyBqt = user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getCode().equals(RoleCode.BQT.name())
                );

        boolean willRemainBqt = request.roles()
                .contains(RoleCode.BQT);

        // BQT tự bỏ role BQT của chính mình
        if (isCurrentUser && currentlyBqt && !willRemainBqt) {
            throw new SelfLockoutException();
        }

        Set<AppRole> newRoles = new HashSet<>();

        for (RoleCode roleCode : request.roles()) {

            AppRole role = roleRepository
                    .findByCode(roleCode.name())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Role không tồn tại: " + roleCode
                            )
                    );

            newRoles.add(role);
        }

        user.setRoles(newRoles);

        user = userRepository.save(user);

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(
            Long id,
            UpdateUserStatusRequest request
    ) {

        AppUser user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy tài khoản"
                        )
                );

        String currentUsername = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        boolean isCurrentUser = user.getUsername()
                .equalsIgnoreCase(currentUsername);

        // BQT không được tự khóa chính mình
        if (isCurrentUser && !request.active()) {
            throw new SelfLockoutException();
        }

        user.setActive(request.active());

        user = userRepository.save(user);

        return toUserResponse(user);
    }

    @Transactional
    public PasswordResetResponse resetPassword(
            Long id,
            ResetPasswordRequest request
    ) {

        AppUser user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy tài khoản"
                        )
                );

        String currentUsername = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        // BQT không được reset password của chính mình
        if (user.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new SelfResetNotAllowedException();
        }

        String newPassword = request.newPassword();

        // Không gửi password -> tự sinh password tạm 10 ký tự
        if (newPassword == null ) {
            newPassword = generateTemporaryPassword();
        }

        // Password không được trùng username
        if (newPassword.equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Mật khẩu không được trùng với username"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        user.setMustChangePassword(true);

        // Dùng để vô hiệu hóa các token cũ
        user.setPasswordChangedAt(Instant.now());

        userRepository.save(user);

        return new PasswordResetResponse(
                user.getId(),
                user.getUsername(),
                newPassword,
                true
        );
    }

    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    + "abcdefghijklmnopqrstuvwxyz"
                    + "0123456789";

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private String generateTemporaryPassword() {

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            int index = SECURE_RANDOM.nextInt(
                    PASSWORD_CHARACTERS.length()
            );

            password.append(
                    PASSWORD_CHARACTERS.charAt(index)
            );
        }

        return password.toString();
    }

    @Transactional(readOnly = true)
    public UserResponse getMe() {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        AppUser user = userRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy tài khoản"
                        )
                );

        return toUserResponse(user);
    }
}
