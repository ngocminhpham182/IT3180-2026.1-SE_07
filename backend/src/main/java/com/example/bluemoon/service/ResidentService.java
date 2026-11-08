package com.example.bluemoon.service;

import com.example.bluemoon.dto.*;
import com.example.bluemoon.exception.*;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.model.Household;
import com.example.bluemoon.model.Resident;
import com.example.bluemoon.model.ResidentMovement;
import com.example.bluemoon.model.enums.MovementType;
import com.example.bluemoon.model.enums.ResidencyStatus;
import com.example.bluemoon.model.enums.ResidentEntryType;
import com.example.bluemoon.repository.AppUserRepository;
import com.example.bluemoon.repository.HouseholdRepository;
import com.example.bluemoon.repository.ResidentMovementRepository;
import com.example.bluemoon.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final HouseholdRepository householdRepository;
    private final ResidentRepository residentRepository;
    private final ResidentMovementRepository residentMovementRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public ResidentResponse createResident(
            Long householdId,
            CreateResidentRequest request,
            AppUser currentUser
    ) {
        // 1. Kiểm tra hộ gia đình
        Household household = householdRepository
                .findByIdAndDeletedAtIsNull(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hộ gia đình"));

        // 2. Kiểm tra trạng thái hộ
        if (!"ACTIVE".equals(household.getStatus())) {
            throw new HouseholdInactiveException();
        }

        // 3. Chuẩn hóa dữ liệu
        String identityNumber = normalizeOptional(request.identityNumber());
        String phone = normalizeOptional(request.phone());

        // 4. Kiểm tra CCCD/CMND trùng
        if (identityNumber != null
                && residentRepository.existsByIdentityNumberAndDeletedAtIsNull(identityNumber)) {
            throw new DuplicateException(
                    "identityNumber",
                    "Số CCCD/CMND đã tồn tại"
            );
        }

        // 5. Xác định entryType và entryDate mặc định
        ResidentEntryType entryType = request.entryType() != null
                ? request.entryType()
                : ResidentEntryType.MOVE_IN;

        LocalDate entryDate = request.entryDate() != null
                ? request.entryDate()
                : LocalDate.now();

        // 6. Tạo nhân khẩu
        Resident resident = new Resident();

        resident.setHousehold(household);
        resident.setFullName(request.fullName().trim());
        resident.setDateOfBirth(request.dateOfBirth());
        resident.setGender(request.gender());
        resident.setIdentityNumber(identityNumber);
        resident.setPhone(phone);
        resident.setRelationshipToHead(request.relationshipToHead().trim());

        // Không cho tạo chủ hộ qua endpoint này
        resident.setHead(false);

        if (entryType == ResidentEntryType.TEMPORARY_RESIDENCE) {
            resident.setStatus(ResidencyStatus.TEMPORARY_RESIDENCE);
            resident.setStatusUntil(request.endDate());
            resident.setMoveInDate(request.startDate());
        } else {
            resident.setStatus(ResidencyStatus.RESIDING);
            resident.setMoveInDate(entryDate);
        }

        Resident savedResident = residentRepository.save(resident);

        // 7. Ghi nhận biến động ban đầu (trừ EXISTING)
        if (entryType != ResidentEntryType.EXISTING) {

            MovementType movementType = switch (entryType) {
                case MOVE_IN -> MovementType.MOVE_IN;
                case BIRTH -> MovementType.BIRTH;
                case TEMPORARY_RESIDENCE -> MovementType.TEMPORARY_RESIDENCE;
                case EXISTING -> throw new IllegalStateException();
            };

            LocalDate movementDate = entryType == ResidentEntryType.TEMPORARY_RESIDENCE
                    ? request.startDate()
                    : entryDate;

            ResidentMovement movement = ResidentMovement.builder()
                    .resident(savedResident)
                    .movementType(movementType)
                    .oldHousehold(null)
                    .newHousehold(household)
                    .effectiveDate(movementDate)
                    .startDate(entryType == ResidentEntryType.TEMPORARY_RESIDENCE
                            ? request.startDate() : null)
                    .endDate(entryType == ResidentEntryType.TEMPORARY_RESIDENCE
                            ? request.endDate() : null)
                    .note(null)
                    .recordedBy(currentUser)
                    .isInitial(true)
                    .build();

            residentMovementRepository.save(movement);
        }

        return toResidentResponse(savedResident);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResidentResponse toResidentResponse(Resident resident) {
        boolean statusExpired = false;
        if ((resident.getStatus() == ResidencyStatus.TEMPORARILY_ABSENT
                || resident.getStatus() == ResidencyStatus.TEMPORARY_RESIDENCE)
                && resident.getStatusUntil() != null) {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            statusExpired = resident.getStatusUntil().isBefore(today);
        }

        Household household = resident.getHousehold();
        String householdCode = household != null ? household.getHouseholdCode() : null;
        String apartmentNo = household != null ? household.getApartmentNo() : null;
        Long householdId = household != null ? household.getId() : null;

        return new ResidentResponse(
                resident.getId(),
                householdId,
                householdCode,
                apartmentNo,
                resident.getFullName(),
                resident.getDateOfBirth(),
                resident.getGender(),
                resident.getIdentityNumber(),
                resident.getPhone(),
                resident.getRelationshipToHead(),
                resident.isHead(),
                resident.getStatus(),
                resident.getStatusUntil(),
                statusExpired,
                resident.getMoveInDate(),
                resident.getMoveOutDate(),
                resident.getCreatedAt(),
                resident.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ResidentInHouseholdResponse> getResidentsByHousehold(
            Long householdId,
            String status
    ) {
        // 1. Kiểm tra hộ tồn tại và chưa bị xóa
        householdRepository.findByIdAndDeletedAtIsNull(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hộ gia đình"));

        // 2. Thứ tự: chủ hộ trước, ngày sinh tăng dần
        Sort sort = Sort.by(
                Sort.Order.desc("head"),
                Sort.Order.asc("dateOfBirth")
        );

        // 3. Xử lý status
        List<Resident> residents;

        if (status == null || status.isBlank()) {
            residents = residentRepository
                    .findByHousehold_IdAndDeletedAtIsNullAndStatusIn(
                            householdId,
                            List.of(
                                    ResidencyStatus.RESIDING,
                                    ResidencyStatus.TEMPORARILY_ABSENT,
                                    ResidencyStatus.TEMPORARY_RESIDENCE
                            ),
                            sort
                    );
        } else if (status.trim().equalsIgnoreCase("ALL")) {
            residents = residentRepository
                    .findByHousehold_IdAndDeletedAtIsNull(householdId, sort);
        } else {
            List<ResidencyStatus> statuses;

            try {
                statuses = Arrays.stream(status.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .map(ResidencyStatus::valueOf)
                        .distinct()
                        .toList();
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_RESIDENCY_STATUS"
                );
            }

            if (statuses.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_RESIDENCY_STATUS"
                );
            }

            residents = residentRepository
                    .findByHousehold_IdAndDeletedAtIsNullAndStatusIn(
                            householdId,
                            statuses,
                            sort
                    );
        }

        // 4. Map Entity sang DTO
        return residents.stream()
                .map(this::toResidentInHouseholdResponse)
                .toList();
    }

    private ResidentInHouseholdResponse toResidentInHouseholdResponse(
            Resident resident
    ) {
        return new ResidentInHouseholdResponse(
                resident.getId(),
                resident.getFullName(),
                resident.getDateOfBirth(),
                resident.getGender(),
                resident.getIdentityNumber(),
                resident.getPhone(),
                resident.getRelationshipToHead(),
                resident.isHead(),
                resident.getStatus(),
                resident.getStatusUntil(),
                resident.getMoveInDate(),
                resident.getMoveOutDate(),
                resident.getCreatedAt(),
                resident.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ResidentResponse> searchResidents(
            String keyword,
            Long householdId,
            String status,
            Boolean statusExpired,
            int page,
            int size
    ) {
        if (page < 1 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PAGE_PARAMETERS"
            );
        }
        // Chuẩn hóa keyword
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        } else if (keyword != null) {
            keyword = keyword.trim();
        }

        // Mặc định: người đang thuộc hộ
        List<String> statuses;

        if (status == null || status.isBlank()) {
            statuses = List.of(
                    ResidencyStatus.RESIDING.name(),
                    ResidencyStatus.TEMPORARILY_ABSENT.name(),
                    ResidencyStatus.TEMPORARY_RESIDENCE.name()
            );
        } else if (status.trim().equalsIgnoreCase("ALL")) {
            statuses = Arrays.stream(ResidencyStatus.values())
                    .map(Enum::name)
                    .toList();
        } else {
            try {
                statuses = Arrays.stream(status.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .map(value -> ResidencyStatus.valueOf(value).name())
                        .distinct()
                        .toList();
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_RESIDENCY_STATUS"
                );
            }
        }

        // Page API bắt đầu từ 1, Spring Data bắt đầu từ 0
        Pageable pageable = PageRequest.of(
                page - 1,
                size
        );

        Page<Resident> result = residentRepository.searchResidents(
                keyword,
                householdId,
                statuses,
                Boolean.TRUE.equals(statusExpired),
                pageable
        );

        List<ResidentResponse> content = result.getContent()
                .stream()
                .map(this::toResidentResponse)
                .toList();

        return new PageResponse<>(
                content,
                result.getNumber() + 1,
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ResidentResponse getResidentById(Long id) {
        Resident resident = residentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân khẩu"));

        return toResidentResponse(resident);
    }

    @Transactional
    public ResidentResponse updateResident(
            Long id,
            UpdateResidentRequest request
    ) {
        // 1. Tìm nhân khẩu, không lấy bản ghi đã xóa mềm
        Resident resident = residentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân khẩu"));

        // 2. Chuẩn hóa dữ liệu
        String fullName = request.fullName().trim();
        String identityNumber = normalizeOptional(request.identityNumber());
        String phone = normalizeOptional(request.phone());
        String relationshipToHead = request.relationshipToHead().trim();

        // 3. Kiểm tra trùng CCCD/CMND
        if (identityNumber != null &&
                residentRepository.existsByIdentityNumberAndDeletedAtIsNullAndIdNot(
                        identityNumber, id
                )) {
            throw new DuplicateException(
                    "identityNumber",
                    "Số CCCD/CMND đã được sử dụng"
            );
        }

        // 4. Chỉ cập nhật các trường được phép
        resident.setFullName(fullName);
        resident.setDateOfBirth(request.dateOfBirth());
        resident.setGender(request.gender());
        resident.setIdentityNumber(identityNumber);
        resident.setPhone(phone);
        resident.setRelationshipToHead(relationshipToHead);

        // 5. Lưu và trả DTO
        Resident savedResident = residentRepository.save(resident);

        return toResidentResponse(savedResident);
    }

    @Transactional
    public void deleteResident(Long id) {
        Resident resident = residentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân khẩu"));

        // 1. Không cho xóa chủ hộ hiện tại
        if (resident.isHead()) {
            throw new ResidentDeleteException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_NEW_HEAD",
                    "Nhân khẩu đang là chủ hộ. Hãy đổi chủ hộ trước khi xóa.",
                    Map.of("reason", "ALREADY_HEAD")
            );
        }

        // 2. Không cho xóa nhân khẩu đang liên kết tài khoản
        if (appUserRepository.existsByResidentId(id)) {
            throw new ResidentDeleteException(
                    HttpStatus.CONFLICT,
                    "HAS_ACCOUNT",
                    "Nhân khẩu đang được gắn với tài khoản đăng nhập nên không thể xóa.",
                    Map.of("residentId", id)
            );
        }

        // 3. Không cho xóa nếu có biến động ngoài bản ghi nhập ban đầu
        long movementCount =
                residentMovementRepository.countByResident_IdAndIsInitialFalse(id);

        if (movementCount > 0) {
            throw new ResidentDeleteException(
                    HttpStatus.CONFLICT,
                    "HAS_HISTORY",
                    "Nhân khẩu đã có lịch sử biến động nên không thể xóa. Hãy ghi nhận chuyển đi hoặc biến động tương ứng.",
                    Map.of(
                            "movementCount", movementCount,
                            "suggestedAction", "RECORD_MOVEMENT"
                    )
            );
        }

        // 4. Xóa mềm
        resident.setDeletedAt(Instant.now());
        residentRepository.save(resident);
    }

    // =========================================================================
    // US-03: Ghi nhận biến động nhân khẩu
    // =========================================================================

    @Transactional
    public RecordMovementResponse recordMovement(
            Long id,
            CreateMovementRequest request,
            AppUser currentUser
    ) {
        // 1. Khóa dòng resident (SELECT FOR UPDATE)
        Resident resident = residentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân khẩu"));

        // 2. Kiểm tra định dạng cơ bản của request
        MovementType type = request.movementType();
        if (type == null) {
            throw new AppValidationException(Map.of("movementType", "Loại biến động không được để trống"));
        }
        if (type == MovementType.BIRTH) {
            throw new AppValidationException(Map.of("movementType", "Khai sinh được ghi khi thêm nhân khẩu"));
        }
        if (request.note() != null && request.note().length() > 500) {
            throw new AppValidationException(Map.of("note", "Ghi chú không được quá 500 ký tự"));
        }

        // 3. Kiểm tra các field bắt buộc theo từng loại biến động (Bảng 4.3)
        Map<String, String> errors = new LinkedHashMap<>();

        if (type == MovementType.MOVE_OUT || type == MovementType.DEATH
                || type == MovementType.TRANSFER || type == MovementType.MOVE_IN
                || type == MovementType.RETURN) {
            if (request.effectiveDate() == null) {
                errors.put("effectiveDate", "Ngày hiệu lực không được để trống");
            }
        }

        if (type == MovementType.TRANSFER || type == MovementType.MOVE_IN) {
            if (request.newHouseholdId() == null) {
                errors.put("newHouseholdId", "Hộ đích không được để trống");
            }
            if (request.relationshipToHead() == null || request.relationshipToHead().trim().isEmpty()) {
                errors.put("relationshipToHead", "Quan hệ với chủ hộ không được để trống");
            } else if (request.relationshipToHead().trim().length() > 50) {
                errors.put("relationshipToHead", "Quan hệ với chủ hộ không được quá 50 ký tự");
            }
        }

        if (type == MovementType.TEMPORARY_ABSENCE || type == MovementType.TEMPORARY_RESIDENCE) {
            if (request.startDate() == null) {
                errors.put("startDate", "Ngày bắt đầu không được để trống");
            }
            if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
                errors.put("endDate", "Ngày kết thúc không được sớm hơn ngày bắt đầu");
            }
        }

        if (!errors.isEmpty()) {
            throw new AppValidationException(errors);
        }

        // 4. Kiểm tra trạng thái hiện tại (AC3)
        ResidencyStatus currentStatus = resident.getStatus();
        if ((currentStatus == ResidencyStatus.MOVED_OUT || currentStatus == ResidencyStatus.DECEASED)
                && type != MovementType.MOVE_IN) {
            throw new ResidentNotResidingException(currentStatus.name());
        }

        // 5. Kiểm tra cặp (loại biến động, trạng thái hiện tại) có hợp lệ không (Bảng 4.4)
        List<ResidencyStatus> allowedFrom = switch (type) {
            case MOVE_OUT, DEATH -> List.of(
                    ResidencyStatus.RESIDING,
                    ResidencyStatus.TEMPORARILY_ABSENT,
                    ResidencyStatus.TEMPORARY_RESIDENCE
            );
            case TRANSFER -> List.of(
                    ResidencyStatus.RESIDING,
                    ResidencyStatus.TEMPORARY_RESIDENCE
            );
            case MOVE_IN -> List.of(ResidencyStatus.MOVED_OUT);
            case TEMPORARY_ABSENCE, TEMPORARY_RESIDENCE -> List.of(ResidencyStatus.RESIDING);
            case RETURN -> List.of(ResidencyStatus.TEMPORARILY_ABSENT);
            default -> List.of();
        };

        if (!allowedFrom.contains(currentStatus)) {
            throw new InvalidTransitionException(
                    currentStatus.name(),
                    allowedFrom.stream().map(Enum::name).toList()
            );
        }

        // 6. Kiểm tra ngày (Mục 4.6)
        LocalDate effectiveDate = (type == MovementType.TEMPORARY_ABSENCE || type == MovementType.TEMPORARY_RESIDENCE)
                ? request.startDate()
                : request.effectiveDate();
        String dateFieldName = (type == MovementType.TEMPORARY_ABSENCE || type == MovementType.TEMPORARY_RESIDENCE)
                ? "startDate"
                : "effectiveDate";
        String dateFieldLabel = (type == MovementType.TEMPORARY_ABSENCE || type == MovementType.TEMPORARY_RESIDENCE)
                ? "Ngày bắt đầu"
                : "Ngày hiệu lực";

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (effectiveDate.isAfter(today)) {
            errors.put(dateFieldName, dateFieldLabel + " không được ở tương lai");
        }
        if (resident.getDateOfBirth() != null && effectiveDate.isBefore(resident.getDateOfBirth())) {
            errors.put(dateFieldName, dateFieldLabel + " không được trước ngày sinh");
        }
        if ((type == MovementType.MOVE_OUT || type == MovementType.DEATH) && resident.getMoveInDate() != null) {
            if (effectiveDate.isBefore(resident.getMoveInDate())) {
                errors.put("effectiveDate", "Ngày hiệu lực không được trước ngày chuyển đến (" + resident.getMoveInDate() + ")");
            }
        }
        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            errors.put("endDate", "Ngày kết thúc không được sớm hơn ngày bắt đầu");
        }

        if (!errors.isEmpty()) {
            throw new AppValidationException(errors);
        }

        // Kiểm tra không trước effectiveDate của biến động gần nhất
        Optional<ResidentMovement> latestMovementOpt = residentMovementRepository
                .findFirstByResident_IdOrderByEffectiveDateDescRecordedAtDescIdDesc(resident.getId());
        if (latestMovementOpt.isPresent()) {
            LocalDate latestDate = latestMovementOpt.get().getEffectiveDate();
            if (effectiveDate.isBefore(latestDate)) {
                String msg = "Ngày hiệu lực không được trước ngày của biến động gần nhất (" + latestDate + ")";
                throw new AppValidationException(msg, Map.of(dateFieldName, msg));
            }
        }

        // 7. Kiểm tra hộ đích (Mục 4.3 & 4.4)
        Household newHousehold = null;
        if (type == MovementType.TRANSFER || type == MovementType.MOVE_IN) {
            if (type == MovementType.TRANSFER && request.newHouseholdId().equals(resident.getHousehold().getId())) {
                throw new AppValidationException(Map.of("newHouseholdId", "Hộ đích phải khác hộ hiện tại"));
            }
            newHousehold = householdRepository
                    .findByIdAndDeletedAtIsNull(request.newHouseholdId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Không tìm thấy hộ gia đình")
                    );
            if (!"ACTIVE".equals(newHousehold.getStatus())) {
                throw new HouseholdInactiveException();
            }
        }

        // 8. Xử lý chủ hộ rời hộ (Mục 4.5)
        if (resident.isHead() && (type == MovementType.MOVE_OUT || type == MovementType.DEATH || type == MovementType.TRANSFER)) {
            List<Resident> candidates = residentRepository.findCandidatesForNewHead(
                    resident.getHousehold().getId(),
                    resident.getId()
            );

            if (!candidates.isEmpty()) {
                if (request.newHeadResidentId() == null) {
                    List<HeadRequiredException.CandidateResponse> candidateResponses = candidates.stream()
                            .map(c -> new HeadRequiredException.CandidateResponse(
                                    c.getId(),
                                    c.getFullName(),
                                    c.getRelationshipToHead()
                            ))
                            .toList();
                    throw new HeadRequiredException(candidateResponses);
                }

                Long newHeadId = request.newHeadResidentId();
                if (newHeadId.equals(resident.getId())) {
                    throw new InvalidNewHeadException("Không thể chọn chính người này làm chủ hộ mới", "SAME_PERSON");
                }

                Resident newHead = residentRepository.findByIdAndDeletedAtIsNull(newHeadId)
                        .orElseThrow(() -> new InvalidNewHeadException("Chủ hộ mới không tồn tại", "NOT_FOUND"));

                if (!newHead.getHousehold().getId().equals(resident.getHousehold().getId())) {
                    throw new InvalidNewHeadException("Người này không thuộc hộ gia đình", "NOT_IN_HOUSEHOLD");
                }
                if (newHead.isHead()) {
                    throw new InvalidNewHeadException("Người này đã là chủ hộ", "ALREADY_HEAD");
                }
                if (newHead.getStatus() != ResidencyStatus.RESIDING) {
                    throw new InvalidNewHeadException("Nhân khẩu không ở trạng thái đang cư trú", "NOT_RESIDING");
                }

                // Ghi DB: bỏ is_head của chủ hộ cũ và saveAndFlush trước
                resident.setHead(false);
                residentRepository.saveAndFlush(resident);

                // Sau đó đặt chủ hộ mới
                newHead.setHead(true);
                residentRepository.saveAndFlush(newHead);
            } else {
                // Không có ứng viên nào
                if (request.newHeadResidentId() != null) {
                    throw new InvalidNewHeadException("Hộ gia đình không có nhân khẩu khác để chọn làm chủ hộ", "NOT_FOUND");
                }
                resident.setHead(false);
                residentRepository.saveAndFlush(resident);
            }
        }

        // 9. Cập nhật resident theo Bảng 4.4
        Household oldHousehold = resident.getHousehold();

        switch (type) {
            case MOVE_OUT -> {
                resident.setStatus(ResidencyStatus.MOVED_OUT);
                resident.setHead(false);
                resident.setStatusUntil(null);
                resident.setMoveOutDate(effectiveDate);
            }
            case DEATH -> {
                resident.setStatus(ResidencyStatus.DECEASED);
                resident.setHead(false);
                resident.setStatusUntil(null);
                resident.setMoveOutDate(effectiveDate);
            }
            case TRANSFER -> {
                resident.setStatus(ResidencyStatus.RESIDING);
                resident.setHousehold(newHousehold);
                resident.setHead(false);
                resident.setStatusUntil(null);
                resident.setMoveInDate(effectiveDate);
                resident.setMoveOutDate(null);
                resident.setRelationshipToHead(request.relationshipToHead().trim());
            }
            case MOVE_IN -> {
                resident.setStatus(ResidencyStatus.RESIDING);
                resident.setHousehold(newHousehold);
                resident.setHead(false);
                resident.setStatusUntil(null);
                resident.setMoveInDate(effectiveDate);
                resident.setMoveOutDate(null);
                resident.setRelationshipToHead(request.relationshipToHead().trim());
            }
            case TEMPORARY_ABSENCE -> {
                resident.setStatus(ResidencyStatus.TEMPORARILY_ABSENT);
                resident.setStatusUntil(request.endDate());
            }
            case TEMPORARY_RESIDENCE -> {
                resident.setStatus(ResidencyStatus.TEMPORARY_RESIDENCE);
                resident.setStatusUntil(request.endDate());
            }
            case RETURN -> {
                resident.setStatus(ResidencyStatus.RESIDING);
                resident.setStatusUntil(null);
            }
        }

        Resident savedResident = residentRepository.saveAndFlush(resident);

        // 10. Khóa tài khoản liên kết nếu MOVE_OUT hoặc DEATH (Mục 4.7)
        if (type == MovementType.MOVE_OUT || type == MovementType.DEATH) {
            appUserRepository.findByResidentId(resident.getId()).ifPresent(user -> {
                user.setActive(false);
                appUserRepository.save(user);
            });
        }

        // 11. Lưu bản ghi resident_movement mới (append-only)
        Household movementOldHousehold = switch (type) {
            case TRANSFER, MOVE_OUT, DEATH -> oldHousehold;
            default -> null;
        };
        Household movementNewHousehold = switch (type) {
            case TRANSFER, MOVE_IN -> newHousehold;
            default -> null;
        };

        ResidentMovement movement = ResidentMovement.builder()
                .resident(savedResident)
                .movementType(type)
                .oldHousehold(movementOldHousehold)
                .newHousehold(movementNewHousehold)
                .effectiveDate(effectiveDate)
                .startDate((type == MovementType.TEMPORARY_ABSENCE || type == MovementType.TEMPORARY_RESIDENCE)
                        ? request.startDate() : null)
                .endDate((type == MovementType.TEMPORARY_ABSENCE || type == MovementType.TEMPORARY_RESIDENCE)
                        ? request.endDate() : null)
                .note(request.note() != null && !request.note().isBlank() ? request.note().trim() : null)
                .recordedBy(currentUser)
                .isInitial(false)
                .build();

        ResidentMovement savedMovement = residentMovementRepository.save(movement);

        return new RecordMovementResponse(
                toMovementResponse(savedMovement),
                toResidentResponse(savedResident)
        );
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> getMovements(Long residentId) {
        Resident resident = residentRepository.findByIdAndDeletedAtIsNull(residentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân khẩu"));

        List<ResidentMovement> movements = residentMovementRepository
                .findByResident_IdOrderByEffectiveDateDescRecordedAtDescIdDesc(resident.getId());

        return movements.stream()
                .map(this::toMovementResponse)
                .toList();
    }

    private MovementResponse toMovementResponse(ResidentMovement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getResident().getId(),
                movement.getMovementType(),
                movement.getEffectiveDate(),
                movement.getStartDate(),
                movement.getEndDate(),

                movement.getOldHousehold() == null ? null :
                        new MovementResponse.HouseholdRef(
                                movement.getOldHousehold().getId(),
                                movement.getOldHousehold().getHouseholdCode(),
                                movement.getOldHousehold().getApartmentNo()
                        ),

                movement.getNewHousehold() == null ? null :
                        new MovementResponse.HouseholdRef(
                                movement.getNewHousehold().getId(),
                                movement.getNewHousehold().getHouseholdCode(),
                                movement.getNewHousehold().getApartmentNo()
                        ),

                movement.getNote(),
                movement.isInitial(),

                new MovementResponse.UserRef(
                        movement.getRecordedBy().getId(),
                        movement.getRecordedBy().getFullName()
                ),

                movement.getRecordedAt()
        );
    }
}
