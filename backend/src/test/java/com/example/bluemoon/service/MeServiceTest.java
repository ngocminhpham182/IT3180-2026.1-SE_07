package com.example.bluemoon.service;

import com.example.bluemoon.dto.ChangePasswordRequest;
import com.example.bluemoon.dto.UpdateProfileRequest;
import com.example.bluemoon.dto.UserResponse;
import com.example.bluemoon.exception.DuplicateException;
import com.example.bluemoon.exception.PasswordValidationException;
import com.example.bluemoon.model.AppRole;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.repository.AppRoleRepository;
import com.example.bluemoon.repository.AppUserRepository;
import com.example.bluemoon.repository.ResidentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AppRoleRepository roleRepository;

    @Mock
    private ResidentRepository residentRepository;

    private PasswordEncoder passwordEncoder;
    private MeService meService;
    private UserService userService;
    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        meService = new MeService(userRepository, residentRepository, passwordEncoder);
        userService = new UserService(userRepository, roleRepository, residentRepository, passwordEncoder);

        AppRole role = new AppRole();
        role.setId(1L);
        role.setCode("RESIDENT");
        role.setName("Cư dân");

        currentUser = AppUser.builder()
                .id(10L)
                .username("resident01")
                .passwordHash(passwordEncoder.encode("OldPass@123"))
                .fullName("Nguyễn Văn Cũ")
                .email("old@example.com")
                .phone("0901234567")
                .active(true)
                .mustChangePassword(true)
                .roles(Set.of(role))
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_RESIDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMe_resolvesAppUserPrincipalFromSecurityContext() {
        when(userRepository.findByUsernameIgnoreCase("resident01"))
                .thenReturn(Optional.of(currentUser));

        UserResponse res = userService.getMe();

        assertEquals(10L, res.id());
        assertEquals("resident01", res.username());
        assertEquals("old@example.com", res.email());
        assertEquals("0901234567", res.phone());
    }

    @Test
    void updateProfile_updatesFullNameEmailAndPhoneWithAppUserPrincipal() {
        when(userRepository.findByUsernameIgnoreCase("resident01"))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 10L))
                .thenReturn(false);
        when(userRepository.save(any(AppUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = meService.updateProfile(new UpdateProfileRequest(
                "  Nguyễn Văn Mới  ",
                "  new@example.com  ",
                "  0987654321  "
        ));

        assertEquals("Nguyễn Văn Mới", res.fullName());
        assertEquals("new@example.com", res.email());
        assertEquals("0987654321", res.phone());
        verify(userRepository).save(currentUser);
    }

    @Test
    void updateProfile_rejectsDuplicateEmail() {
        when(userRepository.findByUsernameIgnoreCase("resident01"))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("dup@example.com", 10L))
                .thenReturn(true);

        assertThrows(DuplicateException.class, () -> meService.updateProfile(
                new UpdateProfileRequest("Nguyễn Văn Mới", "dup@example.com", null)
        ));
    }

    @Test
    void changePassword_updatesHashClearsMustChangeAndSetsPasswordChangedAt() {
        when(userRepository.findByUsernameIgnoreCase("resident01"))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.save(any(AppUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        meService.changePassword(new ChangePasswordRequest(
                "OldPass@123",
                "NewPass@456",
                "NewPass@456"
        ));

        assertTrue(passwordEncoder.matches("NewPass@456", currentUser.getPasswordHash()));
        assertFalse(currentUser.isMustChangePassword());
        assertNotNull(currentUser.getPasswordChangedAt());
        verify(userRepository).save(currentUser);
    }

    @Test
    void changePassword_rejectsWrongOldPassword() {
        when(userRepository.findByUsernameIgnoreCase("resident01"))
                .thenReturn(Optional.of(currentUser));

        assertThrows(PasswordValidationException.class, () -> meService.changePassword(
                new ChangePasswordRequest("WrongPass", "NewPass@456", "NewPass@456")
        ));
        verify(userRepository, never()).save(any());
    }
}
