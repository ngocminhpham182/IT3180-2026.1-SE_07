package com.example.bluemoon.service;

import com.example.bluemoon.dto.*;
import com.example.bluemoon.exception.DuplicateException;
import com.example.bluemoon.exception.HouseholdInactiveException;
import com.example.bluemoon.exception.InvalidNewHeadException;
import com.example.bluemoon.exception.ResidentDeleteException;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.model.Household;
import com.example.bluemoon.model.Resident;
import com.example.bluemoon.model.ResidentMovement;
import com.example.bluemoon.model.enums.ResidencyStatus;
import com.example.bluemoon.model.enums.ResidentEntryType;
import com.example.bluemoon.model.enums.MovementType;
import com.example.bluemoon.repository.AppUserRepository;
import com.example.bluemoon.repository.HouseholdRepository;
import com.example.bluemoon.repository.ResidentMovementRepository;
import com.example.bluemoon.repository.ResidentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND"
                ));

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

        // 7. Ghi nhận biến động, trừ EXISTING
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

            // TODO: Gọi logic tạo ResidentMovement hiện có của project.
            // movementType: loại biến động
            // movementDate: ngày biến động
            // savedResident: nhân khẩu vừa được tạo
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
        return new ResidentResponse(
                resident.getId(),
                resident.getHousehold().getId(),
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
    public List<ResidentInHouseholdResponse> getResidentsByHousehold(
            Long householdId,
            String status
    ) {
        // 1. Kiểm tra hộ tồn tại và chưa bị xóa
        householdRepository.findByIdAndDeletedAtIsNull(householdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND"
                ));

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy nhân khẩu"
                ));

        return toResidentResponse(resident);
    }

    @Transactional
    public ResidentResponse updateResident(
            Long id,
            UpdateResidentRequest request
    ) {
        // 1. Tìm nhân khẩu, không lấy bản ghi đã xóa mềm
        Resident resident = residentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy nhân khẩu"
                ));

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy nhân khẩu"
                ));

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
}