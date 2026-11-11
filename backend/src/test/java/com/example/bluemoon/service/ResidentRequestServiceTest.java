package com.example.bluemoon.service;

import com.example.bluemoon.dto.*;
import com.example.bluemoon.exception.*;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.model.Household;
import com.example.bluemoon.model.Resident;
import com.example.bluemoon.model.ResidentRequest;
import com.example.bluemoon.model.enums.*;
import com.example.bluemoon.repository.ResidentMovementRepository;
import com.example.bluemoon.repository.ResidentRepository;
import com.example.bluemoon.repository.ResidentRequestRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResidentRequestServiceTest {

    @Mock
    private ResidentRequestRepository residentRequestRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private ResidentMovementRepository residentMovementRepository;

    @Mock
    private ResidentService residentService;

    private Validator validator;
    private ResidentRequestService service;

    private Household household;
    private Household otherHousehold;
    private Resident resident;
    private Resident otherResident;
    private AppUser residentUser;
    private AppUser bqtUser;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new ResidentRequestService(
                residentRequestRepository,
                residentRepository,
                residentMovementRepository,
                residentService,
                validator
        );

        household = Household.builder()
                .id(1L)
                .householdCode("H001")
                .apartmentNo("A101")
                .floor((short) 1)
                .area(new BigDecimal("75.5"))
                .status("ACTIVE")
                .build();

        otherHousehold = Household.builder()
                .id(2L)
                .householdCode("H002")
                .apartmentNo("A102")
                .status("ACTIVE")
                .build();

        resident = Resident.builder()
                .id(5L)
                .household(household)
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .identityNumber("012345678901")
                .phone("0901234567")
                .relationshipToHead("Con")
                .head(false)
                .status(ResidencyStatus.RESIDING)
                .build();

        otherResident = Resident.builder()
                .id(9L)
                .household(otherHousehold)
                .fullName("Trần Thị B")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .status(ResidencyStatus.RESIDING)
                .build();

        residentUser = AppUser.builder()
                .id(7L)
                .username("cu_dan_5")
                .fullName("Nguyễn Văn A")
                .residentId(5L)
                .build();

        bqtUser = AppUser.builder()
                .id(1L)
                .username("bqt01")
                .fullName("Ban Quản Trị")
                .build();
    }

    @Test
    @DisplayName("[US-05] GET /me/household trả về hộ, thành viên và pendingRequests")
    void getMyHousehold_returnsHouseholdData() {
        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(resident));
        when(residentRepository.findByHousehold_IdAndDeletedAtIsNullAndStatusIn(eq(1L), anyList(), any(Sort.class)))
                .thenReturn(List.of(resident));
        ResidentRequest pending = ResidentRequest.builder()
                .id(31L)
                .type(ResidentRequestType.PROFILE_UPDATE)
                .status(ResidentRequestStatus.PENDING)
                .household(household)
                .resident(resident)
                .submittedBy(residentUser)
                .build();
        when(residentRequestRepository.findByHousehold_IdAndStatus(1L, ResidentRequestStatus.PENDING))
                .thenReturn(List.of(pending));

        MyHouseholdResponse res = service.getMyHousehold(residentUser);

        assertEquals(1L, res.household().id());
        assertEquals("H001", res.household().householdCode());
        assertEquals(5L, res.myResidentId());
        assertEquals(1, res.residents().size());
        assertEquals(1, res.pendingRequests().size());
        assertEquals(31L, res.pendingRequests().get(0).id());
    }

    @Test
    @DisplayName("[US-05] GET /me/household khi tài khoản chưa gắn nhân khẩu -> 404 ResourceNotFoundException")
    void getMyHousehold_noResidentLink_throwsNotFound() {
        AppUser unlinked = AppUser.builder().id(8L).username("no_link").residentId(null).build();
        assertThrows(ResourceNotFoundException.class, () -> service.getMyHousehold(unlinked));
    }

    @Test
    @DisplayName("[US-05 AC1] Gửi đề nghị sửa thông tin hợp lệ -> PENDING và chưa sửa dữ liệu gốc")
    void submit_profileUpdate_valid_createsPendingRequest() {
        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(resident));
        when(residentRequestRepository.findFirstByResident_IdAndTypeAndStatus(
                5L, ResidentRequestType.PROFILE_UPDATE, ResidentRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(residentRequestRepository.saveAndFlush(any(ResidentRequest.class)))
                .thenAnswer(inv -> {
                    ResidentRequest r = inv.getArgument(0);
                    r.setId(31L);
                    return r;
                });

        SubmitResidentRequest req = new SubmitResidentRequest(
                ResidentRequestType.PROFILE_UPDATE,
                5L,
                new ResidentInfo("Nguyễn Văn Anh", LocalDate.of(2000, 1, 1), Gender.MALE,
                        "012345678901", "0901234567", "Con"),
                null, null, null, "Sai họ tên trên hồ sơ"
        );

        ResidentRequestResponse res = service.submit(residentUser, req);

        assertEquals(31L, res.id());
        assertEquals(ResidentRequestStatus.PENDING, res.status());
        assertEquals(List.of("fullName"), res.changes());
        assertEquals("Nguyễn Văn A", resident.getFullName()); // Original unchanged
    }

    @Test
    @DisplayName("[US-05 AC2] Ngày sinh tương lai và họ tên trống -> AppValidationException")
    void submit_profileUpdate_invalidFields_throwsValidation() {
        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(resident));

        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        SubmitResidentRequest req = new SubmitResidentRequest(
                ResidentRequestType.PROFILE_UPDATE,
                5L,
                new ResidentInfo(" ", tomorrow, Gender.MALE, "012345678901", "0901234567", "Con"),
                null, null, null, null
        );

        AppValidationException ex = assertThrows(AppValidationException.class, () -> service.submit(residentUser, req));
        assertTrue(ex.getErrors().containsKey("profile.fullName"));
        assertTrue(ex.getErrors().containsKey("profile.dateOfBirth"));
        verify(residentRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("[US-05 AC3 / US-06 AC3] Gửi đề nghị cho nhân khẩu hộ khác -> ResidentNotInHouseholdException")
    void submit_otherHouseholdResident_throwsForbidden() {
        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(resident));
        when(residentRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(otherResident));

        SubmitResidentRequest req = new SubmitResidentRequest(
                ResidentRequestType.TEMPORARY_ABSENCE,
                9L,
                null, null,
                LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")),
                LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(10),
                null
        );

        assertThrows(ResidentNotInHouseholdException.class, () -> service.submit(residentUser, req));
    }

    @Test
    @DisplayName("[US-05] Gửi đề nghị sửa thông tin nhưng không đổi gì -> NoChangesException")
    void submit_profileUpdate_noChanges_throwsNoChanges() {
        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(resident));

        SubmitResidentRequest req = new SubmitResidentRequest(
                ResidentRequestType.PROFILE_UPDATE,
                5L,
                new ResidentInfo("Nguyễn Văn A", LocalDate.of(2000, 1, 1), Gender.MALE,
                        "012345678901", "0901234567", "Con"),
                null, null, null, null
        );

        assertThrows(NoChangesException.class, () -> service.submit(residentUser, req));
    }

    @Test
    @DisplayName("[US-06 AC1] Gửi tờ khai tạm vắng hợp lệ -> PENDING")
    void submit_temporaryAbsence_valid_createsPendingRequest() {
        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(resident));
        when(residentMovementRepository.findFirstByResident_IdOrderByEffectiveDateDescRecordedAtDescIdDesc(5L))
                .thenReturn(Optional.empty());
        when(residentRequestRepository.findFirstByResident_IdAndTypeAndStatus(
                5L, ResidentRequestType.TEMPORARY_ABSENCE, ResidentRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(residentRequestRepository.saveAndFlush(any(ResidentRequest.class)))
                .thenAnswer(inv -> {
                    ResidentRequest r = inv.getArgument(0);
                    r.setId(32L);
                    return r;
                });

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        SubmitResidentRequest req = new SubmitResidentRequest(
                ResidentRequestType.TEMPORARY_ABSENCE,
                5L, null, null,
                today, today.plusDays(14), "Đi công tác"
        );

        ResidentRequestResponse res = service.submit(residentUser, req);

        assertEquals(32L, res.id());
        assertEquals(ResidentRequestStatus.PENDING, res.status());
        assertNull(res.before());
        assertNull(res.changes());
    }

    @Test
    @DisplayName("[US-06 AC2] Tờ khai tạm vắng thiếu ngày bắt đầu / kết thúc -> AppValidationException")
    void submit_temporaryAbsence_missingDates_throwsValidation() {
        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(resident));

        SubmitResidentRequest req = new SubmitResidentRequest(
                ResidentRequestType.TEMPORARY_ABSENCE,
                5L, null, null, null, null, null
        );

        AppValidationException ex = assertThrows(AppValidationException.class, () -> service.submit(residentUser, req));
        assertTrue(ex.getErrors().containsKey("startDate"));
        assertTrue(ex.getErrors().containsKey("endDate"));
    }

    @Test
    @DisplayName("[US-06 AC1] Gửi tờ khai tạm trú khách hợp lệ -> PENDING")
    void submit_temporaryResidence_valid_createsPendingRequest() {
        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(resident));
        when(residentRepository.existsByIdentityNumberAndDeletedAtIsNull("012345678999")).thenReturn(false);
        when(residentRequestRepository.findPendingTemporaryResidenceIdByIdentityNumber("012345678999"))
                .thenReturn(Optional.empty());
        when(residentRequestRepository.saveAndFlush(any(ResidentRequest.class)))
                .thenAnswer(inv -> {
                    ResidentRequest r = inv.getArgument(0);
                    r.setId(33L);
                    return r;
                });

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        SubmitResidentRequest req = new SubmitResidentRequest(
                ResidentRequestType.TEMPORARY_RESIDENCE,
                null, null,
                new ResidentInfo("Lê Văn Khách", LocalDate.of(1995, 5, 5), Gender.MALE,
                        "012345678999", "0912345678", "Bạn"),
                today, today.plusMonths(3), "Ở nhờ"
        );

        ResidentRequestResponse res = service.submit(residentUser, req);

        assertEquals(33L, res.id());
        assertNull(res.resident());
        assertEquals(ResidentRequestStatus.PENDING, res.status());
    }

    @Test
    @DisplayName("Đã có đề nghị chờ duyệt cùng loại -> PendingRequestExistsException")
    void submit_whenPendingExists_throwsConflict() {
        when(residentRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(resident));
        ResidentRequest existing = ResidentRequest.builder().id(99L).build();
        when(residentRequestRepository.findFirstByResident_IdAndTypeAndStatus(
                5L, ResidentRequestType.PROFILE_UPDATE, ResidentRequestStatus.PENDING))
                .thenReturn(Optional.of(existing));

        SubmitResidentRequest req = new SubmitResidentRequest(
                ResidentRequestType.PROFILE_UPDATE,
                5L,
                new ResidentInfo("Nguyễn Văn Anh", LocalDate.of(2000, 1, 1), Gender.MALE,
                        "012345678901", "0901234567", "Con"),
                null, null, null, null
        );

        PendingRequestExistsException ex = assertThrows(
                PendingRequestExistsException.class,
                () -> service.submit(residentUser, req)
        );
        assertEquals(99L, ex.getRequestId());
    }

    @Test
    @DisplayName("[US-05 AC4] Duyệt đề nghị sửa thông tin: cập nhật đúng field trong changes")
    void approve_profileUpdate_updatesOnlyChangedFields() {
        Map<String, Object> before = Map.of(
                "fullName", "Nguyễn Văn A",
                "dateOfBirth", "2000-01-01",
                "gender", "MALE",
                "identityNumber", "012345678901",
                "phone", "0901234567",
                "relationshipToHead", "Con"
        );
        Map<String, Object> proposed = new LinkedHashMap<>(before);
        proposed.put("fullName", "Nguyễn Văn Anh");

        ResidentRequest pending = ResidentRequest.builder()
                .id(31L)
                .type(ResidentRequestType.PROFILE_UPDATE)
                .status(ResidentRequestStatus.PENDING)
                .household(household)
                .resident(resident)
                .submittedBy(residentUser)
                .submittedAt(Instant.now())
                .beforeValues(before)
                .proposed(proposed)
                .build();

        when(residentRequestRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(pending));
        when(residentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(resident));
        when(residentRepository.save(any(Resident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(residentRequestRepository.save(any(ResidentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ResidentRequestResponse res = service.approve(31L, new ApproveRequest("OK"), bqtUser);

        assertEquals(ResidentRequestStatus.APPROVED, res.status());
        assertEquals("Nguyễn Văn Anh", resident.getFullName());
        assertEquals(5L, res.result().residentId());
        assertNull(res.result().movementId());
    }

    @Test
    @DisplayName("Duyệt đề nghị sửa thông tin khi dữ liệu đã đổi trong lúc chờ -> RequestOutdatedException")
    void approve_profileUpdate_whenOutdated_throwsConflict() {
        resident.setFullName("Tên BQT Đã Sửa");

        Map<String, Object> before = Map.of(
                "fullName", "Nguyễn Văn A",
                "dateOfBirth", "2000-01-01",
                "gender", "MALE",
                "identityNumber", "012345678901",
                "phone", "0901234567",
                "relationshipToHead", "Con"
        );
        Map<String, Object> proposed = new LinkedHashMap<>(before);
        proposed.put("fullName", "Nguyễn Văn Anh");

        ResidentRequest pending = ResidentRequest.builder()
                .id(31L)
                .type(ResidentRequestType.PROFILE_UPDATE)
                .status(ResidentRequestStatus.PENDING)
                .household(household)
                .resident(resident)
                .submittedBy(residentUser)
                .beforeValues(before)
                .proposed(proposed)
                .build();

        when(residentRequestRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(pending));
        when(residentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(resident));

        RequestOutdatedException ex = assertThrows(
                RequestOutdatedException.class,
                () -> service.approve(31L, null, bqtUser)
        );
        assertEquals(List.of("fullName"), ex.getFields());
    }

    @Test
    @DisplayName("[US-06 AC4] Duyệt tờ khai tạm vắng gọi recordMovement với note Theo khai báo #id")
    void approve_temporaryAbsence_callsRecordMovement() {
        LocalDate start = LocalDate.of(2026, 11, 2);
        LocalDate end = LocalDate.of(2026, 12, 20);
        ResidentRequest pending = ResidentRequest.builder()
                .id(32L)
                .type(ResidentRequestType.TEMPORARY_ABSENCE)
                .status(ResidentRequestStatus.PENDING)
                .household(household)
                .resident(resident)
                .submittedBy(residentUser)
                .note("Đi công tác")
                .proposed(Map.of("startDate", start.toString(), "endDate", end.toString()))
                .build();

        when(residentRequestRepository.findByIdForUpdate(32L)).thenReturn(Optional.of(pending));
        MovementResponse movementRes = new MovementResponse(
                12L, 5L, MovementType.TEMPORARY_ABSENCE,
                start, start, end, null, null,
                "Theo khai báo #32: Đi công tác", false,
                new MovementResponse.UserRef(1L, "Ban Quản Trị"), Instant.now()
        );
        when(residentService.recordMovement(eq(5L), any(CreateMovementRequest.class), eq(bqtUser)))
                .thenReturn(new RecordMovementResponse(movementRes, null));
        when(residentRequestRepository.save(any(ResidentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ResidentRequestResponse res = service.approve(32L, null, bqtUser);

        assertEquals(ResidentRequestStatus.APPROVED, res.status());
        assertEquals(5L, res.result().residentId());
        assertEquals(12L, res.result().movementId());
        verify(residentService).recordMovement(
                eq(5L),
                argThat(req -> req.movementType() == MovementType.TEMPORARY_ABSENCE
                        && req.note().equals("Theo khai báo #32: Đi công tác")),
                eq(bqtUser)
        );
    }

    @Test
    @DisplayName("[US-05 AC4] Từ chối đề nghị -> REJECTED với decision.reason")
    void reject_pendingRequest_setsRejectedStatus() {
        ResidentRequest pending = ResidentRequest.builder()
                .id(31L)
                .type(ResidentRequestType.PROFILE_UPDATE)
                .status(ResidentRequestStatus.PENDING)
                .household(household)
                .resident(resident)
                .submittedBy(residentUser)
                .proposed(Map.of())
                .beforeValues(Map.of())
                .build();

        when(residentRequestRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(pending));
        when(residentRequestRepository.save(any(ResidentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ResidentRequestResponse res = service.reject(31L, new RejectRequest("Cần mang CCCD bản gốc"), bqtUser);

        assertEquals(ResidentRequestStatus.REJECTED, res.status());
        assertEquals("Cần mang CCCD bản gốc", res.decision().reason());
        assertNull(res.result());
    }

    @Test
    @DisplayName("Hủy đề nghị đã xử lý -> RequestNotPendingException")
    void cancel_alreadyDecided_throwsConflict() {
        ResidentRequest approved = ResidentRequest.builder()
                .id(31L)
                .status(ResidentRequestStatus.APPROVED)
                .submittedBy(residentUser)
                .build();

        when(residentRequestRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(approved));

        RequestNotPendingException ex = assertThrows(
                RequestNotPendingException.class,
                () -> service.cancel(31L, residentUser)
        );
        assertEquals(ResidentRequestStatus.APPROVED, ex.getStatus());
    }

    @Test
    @DisplayName("[US-06 AC4] Duyệt tờ khai tạm trú tạo nhân khẩu mới TEMPORARY_RESIDENCE")
    void approve_temporaryResidence_callsCreateResident() {
        LocalDate start = LocalDate.of(2026, 11, 2);
        LocalDate end = LocalDate.of(2027, 2, 2);
        Map<String, Object> personMap = new LinkedHashMap<>();
        personMap.put("fullName", "Lê Văn Khách");
        personMap.put("dateOfBirth", "1995-05-05");
        personMap.put("gender", "MALE");
        personMap.put("identityNumber", "012345678999");
        personMap.put("phone", "0912345678");
        personMap.put("relationshipToHead", "Bạn");

        Map<String, Object> proposed = new LinkedHashMap<>();
        proposed.put("startDate", start.toString());
        proposed.put("endDate", end.toString());
        proposed.put("person", personMap);

        ResidentRequest pending = ResidentRequest.builder()
                .id(33L)
                .type(ResidentRequestType.TEMPORARY_RESIDENCE)
                .status(ResidentRequestStatus.PENDING)
                .household(household)
                .resident(null)
                .submittedBy(residentUser)
                .proposed(proposed)
                .build();

        ResidentResponse createdRes = new ResidentResponse(
                101L, 1L, "H001", "A101", "Lê Văn Khách",
                LocalDate.of(1995, 5, 5), Gender.MALE, "012345678999", "0912345678",
                "Bạn", false, ResidencyStatus.TEMPORARY_RESIDENCE, end, false, start, null,
                Instant.now(), Instant.now()
        );
        Resident createdEntity = Resident.builder().id(101L).fullName("Lê Văn Khách").build();

        when(residentRequestRepository.findByIdForUpdate(33L)).thenReturn(Optional.of(pending));
        when(residentRepository.existsByIdentityNumberAndDeletedAtIsNull("012345678999")).thenReturn(false);
        when(residentService.createResident(eq(1L), any(CreateResidentRequest.class), eq(bqtUser)))
                .thenReturn(createdRes);
        when(residentRepository.findById(101L)).thenReturn(Optional.of(createdEntity));
        when(residentRequestRepository.save(any(ResidentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ResidentRequestResponse res = service.approve(33L, null, bqtUser);

        assertEquals(ResidentRequestStatus.APPROVED, res.status());
        assertEquals(101L, res.result().residentId());
        assertNull(res.result().movementId());
        assertNotNull(res.resident());
        assertEquals("Lê Văn Khách", res.resident().fullName());
        verify(residentService).createResident(
                eq(1L),
                argThat(req -> req.entryType() == ResidentEntryType.TEMPORARY_RESIDENCE
                        && req.fullName().equals("Lê Văn Khách")),
                eq(bqtUser)
        );
    }

    @Test
    @DisplayName("[US-05 AC3] Xem chi tiết đề nghị của người khác -> ResourceNotFoundException (404)")
    void getMine_otherUserRequest_throwsNotFound() {
        AppUser anotherUser = AppUser.builder().id(99L).fullName("Other").build();
        ResidentRequest req = ResidentRequest.builder()
                .id(50L)
                .submittedBy(anotherUser)
                .build();
        when(residentRequestRepository.findById(50L)).thenReturn(Optional.of(req));

        assertThrows(ResourceNotFoundException.class, () -> service.getMine(50L, residentUser));
    }

    @Test
    @DisplayName("Filter status không hợp lệ -> AppValidationException")
    void listMine_invalidStatus_throwsValidation() {
        AppValidationException ex = assertThrows(
                AppValidationException.class,
                () -> service.listMine(residentUser, "INVALID_STATUS", null, 1, 20)
        );
        assertTrue(ex.getErrors().containsKey("status"));
    }
}
