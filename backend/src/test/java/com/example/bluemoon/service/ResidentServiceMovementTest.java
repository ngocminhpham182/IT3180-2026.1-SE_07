package com.example.bluemoon.service;

import com.example.bluemoon.dto.CreateMovementRequest;
import com.example.bluemoon.dto.MovementResponse;
import com.example.bluemoon.dto.RecordMovementResponse;
import com.example.bluemoon.exception.*;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.model.Household;
import com.example.bluemoon.model.Resident;
import com.example.bluemoon.model.ResidentMovement;
import com.example.bluemoon.model.enums.MovementType;
import com.example.bluemoon.model.enums.ResidencyStatus;
import com.example.bluemoon.repository.AppUserRepository;
import com.example.bluemoon.repository.HouseholdRepository;
import com.example.bluemoon.repository.ResidentMovementRepository;
import com.example.bluemoon.repository.ResidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResidentServiceMovementTest {

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private ResidentMovementRepository residentMovementRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ResidentService residentService;

    private Household sampleHousehold;
    private Resident sampleResident;
    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        sampleHousehold = Household.builder()
                .id(1L)
                .householdCode("H001")
                .apartmentNo("A101")
                .status("ACTIVE")
                .build();

        sampleResident = Resident.builder()
                .id(5L)
                .household(sampleHousehold)
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .status(ResidencyStatus.RESIDING)
                .head(false)
                .moveInDate(LocalDate.of(2024, 1, 1))
                .build();

        currentUser = AppUser.builder()
                .id(99L)
                .username("bqt_user")
                .fullName("Ban Quản Trị")
                .build();
    }

    @Test
    @DisplayName("[US-03 AC1] Ghi nhận TEMPORARY_ABSENCE thành công cho nhân khẩu RESIDING")
    void recordMovement_temporaryAbsence_success() {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now().plusMonths(2);

        CreateMovementRequest request = new CreateMovementRequest(
                MovementType.TEMPORARY_ABSENCE,
                null,
                startDate,
                endDate,
                null,
                null,
                null,
                "Đi công tác"
        );

        when(residentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sampleResident));
        when(residentMovementRepository.findFirstByResident_IdOrderByEffectiveDateDescRecordedAtDescIdDesc(5L))
                .thenReturn(Optional.empty());
        when(residentRepository.saveAndFlush(any(Resident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(residentMovementRepository.save(any(ResidentMovement.class))).thenAnswer(invocation -> {
            ResidentMovement m = invocation.getArgument(0);
            m.setId(12L);
            return m;
        });

        RecordMovementResponse response = residentService.recordMovement(5L, request, currentUser);

        assertNotNull(response);
        assertEquals(ResidencyStatus.TEMPORARILY_ABSENT, response.resident().residencyStatus());
        assertEquals(endDate, response.resident().statusUntil());
        assertEquals(MovementType.TEMPORARY_ABSENCE, response.movement().movementType());
        assertEquals(startDate, response.movement().effectiveDate());
        assertEquals(startDate, response.movement().startDate());
        assertEquals(endDate, response.movement().endDate());
    }

    @Test
    @DisplayName("[US-03 AC2] endDate < startDate -> ném AppValidationException có errors.endDate")
    void recordMovement_endDateBeforeStartDate_throwsException() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = startDate.minusDays(5);

        CreateMovementRequest request = new CreateMovementRequest(
                MovementType.TEMPORARY_ABSENCE,
                null,
                startDate,
                endDate,
                null,
                null,
                null,
                "Đi học"
        );

        when(residentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sampleResident));

        AppValidationException ex = assertThrows(AppValidationException.class, () ->
                residentService.recordMovement(5L, request, currentUser)
        );

        assertTrue(ex.getErrors().containsKey("endDate"));
        assertEquals("Ngày kết thúc không được sớm hơn ngày bắt đầu", ex.getErrors().get("endDate"));
    }

    @Test
    @DisplayName("[US-03 AC3] Nhân khẩu đã MOVED_OUT ghi TEMPORARY_ABSENCE -> ném ResidentNotResidingException 422")
    void recordMovement_alreadyMovedOut_throwsResidentNotResiding() {
        sampleResident.setStatus(ResidencyStatus.MOVED_OUT);

        CreateMovementRequest request = new CreateMovementRequest(
                MovementType.TEMPORARY_ABSENCE,
                null,
                LocalDate.now().minusDays(2),
                LocalDate.now().plusMonths(1),
                null,
                null,
                null,
                "Đi công tác"
        );

        when(residentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sampleResident));

        ResidentNotResidingException ex = assertThrows(ResidentNotResidingException.class, () ->
                residentService.recordMovement(5L, request, currentUser)
        );

        assertEquals("MOVED_OUT", ex.getResidencyStatus());
        assertEquals("Nhân khẩu không còn cư trú", ex.getMessage());
    }

    @Test
    @DisplayName("Chuyển trạng thái không hợp lệ (ví dụ RETURN khi đang RESIDING) -> ném InvalidTransitionException")
    void recordMovement_invalidTransition_throwsException() {
        sampleResident.setStatus(ResidencyStatus.RESIDING);

        CreateMovementRequest request = new CreateMovementRequest(
                MovementType.RETURN,
                LocalDate.now().minusDays(1),
                null,
                null,
                null,
                null,
                null,
                "Trở về"
        );

        when(residentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sampleResident));

        InvalidTransitionException ex = assertThrows(InvalidTransitionException.class, () ->
                residentService.recordMovement(5L, request, currentUser)
        );

        assertEquals("RESIDING", ex.getCurrentStatus());
        assertTrue(ex.getAllowedFrom().contains("TEMPORARILY_ABSENT"));
    }

    @Test
    @DisplayName("Chủ hộ rời hộ (MOVE_OUT) nhưng chưa chọn chủ hộ mới khi có ứng viên -> ném HeadRequiredException 422")
    void recordMovement_headLeaving_headRequired() {
        sampleResident.setHead(true);

        Resident candidate = Resident.builder()
                .id(6L)
                .household(sampleHousehold)
                .fullName("Trần Thị B")
                .relationshipToHead("Vợ")
                .status(ResidencyStatus.RESIDING)
                .head(false)
                .build();

        CreateMovementRequest request = new CreateMovementRequest(
                MovementType.MOVE_OUT,
                LocalDate.now(),
                null,
                null,
                null,
                null,
                null,
                "Chuyển đi"
        );

        when(residentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sampleResident));
        when(residentRepository.findCandidatesForNewHead(1L, 5L)).thenReturn(List.of(candidate));

        HeadRequiredException ex = assertThrows(HeadRequiredException.class, () ->
                residentService.recordMovement(5L, request, currentUser)
        );

        assertFalse(ex.getCandidates().isEmpty());
        assertEquals(6L, ex.getCandidates().get(0).id());
    }

    @Test
    @DisplayName("Chủ hộ rời hộ và chọn chủ hộ mới hợp lệ -> chuyển chủ hộ và lưu thành công")
    void recordMovement_headLeaving_withValidNewHead_success() {
        sampleResident.setHead(true);

        Resident candidate = Resident.builder()
                .id(6L)
                .household(sampleHousehold)
                .fullName("Trần Thị B")
                .relationshipToHead("Vợ")
                .status(ResidencyStatus.RESIDING)
                .head(false)
                .build();

        CreateMovementRequest request = new CreateMovementRequest(
                MovementType.MOVE_OUT,
                LocalDate.now(),
                null,
                null,
                null,
                null,
                6L,
                "Chuyển đi"
        );

        when(residentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sampleResident));
        when(residentRepository.findCandidatesForNewHead(1L, 5L)).thenReturn(List.of(candidate));
        when(residentRepository.findByIdAndDeletedAtIsNull(6L)).thenReturn(Optional.of(candidate));
        when(residentRepository.saveAndFlush(any(Resident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(residentMovementRepository.save(any(ResidentMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecordMovementResponse response = residentService.recordMovement(5L, request, currentUser);

        assertNotNull(response);
        assertFalse(sampleResident.isHead());
        assertTrue(candidate.isHead());
        assertEquals(ResidencyStatus.MOVED_OUT, response.resident().residencyStatus());
    }

    @Test
    @DisplayName("Khi MOVE_OUT hoặc DEATH, tài khoản liên kết bị đặt is_active = false")
    void recordMovement_moveOut_deactivatesLinkedAccount() {
        CreateMovementRequest request = new CreateMovementRequest(
                MovementType.MOVE_OUT,
                LocalDate.now(),
                null,
                null,
                null,
                null,
                null,
                "Chuyển đi"
        );

        AppUser linkedUser = AppUser.builder()
                .id(10L)
                .residentId(5L)
                .active(true)
                .build();

        when(residentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sampleResident));
        when(residentRepository.saveAndFlush(any(Resident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(residentMovementRepository.save(any(ResidentMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appUserRepository.findByResidentId(5L)).thenReturn(Optional.of(linkedUser));

        residentService.recordMovement(5L, request, currentUser);

        assertFalse(linkedUser.isActive());
        verify(appUserRepository).save(linkedUser);
    }

    @Test
    @DisplayName("[US-03 AC4] Lấy lịch sử biến động theo thứ tự giảm dần thời gian")
    void getMovements_returnsListSorted() {
        ResidentMovement m1 = ResidentMovement.builder()
                .id(1L)
                .resident(sampleResident)
                .movementType(MovementType.MOVE_IN)
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .recordedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .isInitial(true)
                .recordedBy(currentUser)
                .build();

        ResidentMovement m2 = ResidentMovement.builder()
                .id(2L)
                .resident(sampleResident)
                .movementType(MovementType.TEMPORARY_ABSENCE)
                .effectiveDate(LocalDate.of(2026, 10, 1))
                .recordedAt(Instant.parse("2026-10-01T08:00:00Z"))
                .isInitial(false)
                .recordedBy(currentUser)
                .build();

        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(sampleResident));
        when(residentMovementRepository.findByResident_IdOrderByEffectiveDateDescRecordedAtDescIdDesc(5L))
                .thenReturn(List.of(m2, m1));

        List<MovementResponse> movements = residentService.getMovements(5L);

        assertEquals(2, movements.size());
        assertEquals(2L, movements.get(0).id());
        assertEquals(1L, movements.get(1).id());
        assertEquals(MovementType.TEMPORARY_ABSENCE, movements.get(0).movementType());
        assertEquals(MovementType.MOVE_IN, movements.get(1).movementType());
    }
}
