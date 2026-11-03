package com.example.bluemoon.service;

import com.example.bluemoon.dto.ChangePasswordRequest;
import com.example.bluemoon.dto.UpdateProfileRequest;
import com.example.bluemoon.dto.UserResponse;
import com.example.bluemoon.exception.DuplicateException;
import com.example.bluemoon.exception.PasswordValidationException;
import com.example.bluemoon.exception.ResourceNotFoundException;
import com.example.bluemoon.model.AppRole;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.model.Resident;
import com.example.bluemoon.repository.AppUserRepository;
import com.example.bluemoon.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeService {

    private final AppUserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {

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

        // Chuẩn hóa email: null hoặc "" -> null
        String email = normalize(request.email());

        // Email phải duy nhất, ngoại trừ email của chính user
        if (email != null
                && userRepository.existsByEmailIgnoreCaseAndIdNot(
                email,
                user.getId()
        )) {

            throw new DuplicateException(
                    "email",
                    "Email đã tồn tại"
            );
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(normalize(request.phone()));

        user = userRepository.save(user);

        return toUserResponse(user);
    }

    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
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

    @Transactional
    public void changePassword(ChangePasswordRequest request) {

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

        // 1. Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(
                request.oldPassword(),
                user.getPasswordHash()
        )) {
            throw new PasswordValidationException(
                    "oldPassword",
                    "Mật khẩu cũ không đúng"
            );
        }

        // 2. New password phải khác old password
        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new PasswordValidationException(
                    "newPassword",
                    "Mật khẩu mới phải khác mật khẩu cũ"
            );
        }

        // 3. Không được trùng username
        if (request.newPassword()
                .equalsIgnoreCase(user.getUsername())) {

            throw new PasswordValidationException(
                    "newPassword",
                    "Mật khẩu mới không được trùng với username"
            );
        }

        // 4. Confirm password
        if (!request.newPassword()
                .equals(request.confirmPassword())) {

            throw new PasswordValidationException(
                    "confirmPassword",
                    "Mật khẩu xác nhận không khớp"
            );
        }

        // 5. Cập nhật password
        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );

        user.setMustChangePassword(false);

        // Vô hiệu hóa toàn bộ token cũ
        user.setPasswordChangedAt(Instant.now());

        userRepository.save(user);
    }
}