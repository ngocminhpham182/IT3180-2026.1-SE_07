package com.example.bluemoon.service;

import com.example.bluemoon.dto.*;
import com.example.bluemoon.exception.HouseholdDuplicateException;
import com.example.bluemoon.exception.HouseholdInactiveException;
import com.example.bluemoon.exception.InvalidNewHeadException;
import com.example.bluemoon.model.*;
import com.example.bluemoon.model.enums.HouseholdStatus;
import com.example.bluemoon.model.enums.MovementType;
import com.example.bluemoon.model.enums.ResidencyStatus;
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

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final ResidentRepository residentRepository;
    private final ResidentMovementRepository movementRepository;

    @Transactional
    public HouseholdResponse createHousehold(
            CreateHouseholdRequest request,
            AppUser currentUser
    ) {
        // 1. Chuẩn hóa dữ liệu
        String householdCode = request.householdCode()
                .trim()
                .toUpperCase();

        String apartmentNo = request.apartmentNo()
                .trim()
                .toUpperCase();

        String identityNumber = normalizeOptional(
                request.head().identityNumber()
        );

        String phone = normalizeOptional(
                request.head().phone()
        );

        // 2. Kiểm tra ngày sinh
        LocalDate dateOfBirth = request.head().dateOfBirth();

        if (dateOfBirth.isBefore(LocalDate.of(1900, 1, 1))) {
            throw new IllegalArgumentException(
                    "Ngày sinh không được trước 1900-01-01"
            );
        }

        // 3. Kiểm tra các trường bị trùng
        Map<String, String> duplicateErrors = new LinkedHashMap<>();

        if (householdRepository
                .existsByHouseholdCodeAndDeletedAtIsNull(householdCode)) {

            duplicateErrors.put(
                    "householdCode",
                    "Mã hộ đã tồn tại"
            );
        }

        if (householdRepository
                .existsByApartmentNoAndDeletedAtIsNull(apartmentNo)) {

            duplicateErrors.put(
                    "apartmentNo",
                    "Số căn hộ đã tồn tại"
            );
        }

        if (identityNumber != null &&
                residentRepository
                        .existsByIdentityNumberAndDeletedAtIsNull(identityNumber)) {

            duplicateErrors.put(
                    "head.identityNumber",
                    "Số CCCD/CMND đã tồn tại"
            );
        }

        if (!duplicateErrors.isEmpty()) {
            throw new HouseholdDuplicateException(duplicateErrors);
        }

        // 4. Tạo hộ gia đình
        Household household = Household.builder()
                .householdCode(householdCode)
                .apartmentNo(apartmentNo)
                .floor(request.floor() == null
                        ? null
                        : request.floor().shortValue())
                .area(request.area())
                .status("ACTIVE")
                .build();

        householdRepository.saveAndFlush(household);

        // 5. Tạo chủ hộ
        LocalDate today = LocalDate.now();

        Resident head = Resident.builder()
                .household(household)
                .fullName(request.head().fullName().trim())
                .dateOfBirth(dateOfBirth)
                .gender(request.head().gender())
                .identityNumber(identityNumber)
                .phone(phone)
                .head(true)
                .status(ResidencyStatus.RESIDING)
                .moveInDate(today)
                .build();

        residentRepository.saveAndFlush(head);

        // 6. Ghi nhận biến động MOVE_IN
        ResidentMovement movement = ResidentMovement.builder()
                .resident(head)
                .movementType(MovementType.MOVE_IN)
                .oldHousehold(null)
                .newHousehold(household)
                .effectiveDate(today)
                .recordedBy(currentUser)
                .note("Ghi nhận chủ hộ khi tạo hộ gia đình")
                .isInitial(true)
                .build();

        movementRepository.save(movement);

        // 7. Tạo response
        HouseholdResponse.HeadResponse headResponse =
                new HouseholdResponse.HeadResponse(
                        head.getId(),
                        head.getFullName(),
                        head.getDateOfBirth(),
                        head.getGender(),
                        head.getIdentityNumber(),
                        head.getPhone(),
                        head.isHead(),
                        head.getStatus()
                );

        return new HouseholdResponse(
                household.getId(),
                household.getHouseholdCode(),
                household.getApartmentNo(),
                household.getFloor(),
                household.getArea(),
                household.getStatus(),
                household.getCreatedAt(),
                household.getUpdatedAt(),
                headResponse,
                1,
                1
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Transactional(readOnly = true)
    public PageResponse getHouseholds(
            String keyword,
            HouseholdStatus status,
            int page,
            int size
    ) {
        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.ASC, "householdCode")
        );

        Page<HouseholdListItemResponse> result =
                householdRepository.searchHouseholds(
                        normalizedKeyword,
                        status.name(),
                        pageable
                );

        return new PageResponse(
                result.getContent(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public HouseholdDetailResponse getHouseholdById(Long id) {
        Household household = householdRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "HOUSEHOLD_NOT_FOUND"
                ));

        List<ResidentInHouseholdResponse> residents = household.getResidents()
                .stream()
                .filter(resident -> resident.getDeletedAt() == null)
                .sorted(Comparator.comparing(Resident::getId))
                .map(resident -> new ResidentInHouseholdResponse(
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
                ))
                .toList();

        return new HouseholdDetailResponse(
                household.getId(),
                household.getHouseholdCode(),
                household.getApartmentNo(),
                household.getFloor(),
                household.getArea(),
                household.getStatus(),
                household.getCreatedAt(),
                household.getUpdatedAt(),
                residents
        );
    }

    @Transactional
    public HouseholdResponse updateHousehold(
            Long id,
            UpdateHouseholdRequest request
    ) {
        // 1. Tìm hộ gia đình, không cho phép sửa hộ đã bị xóa mềm
        Household household = householdRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "HOUSEHOLD_NOT_FOUND"
                ));

        // 2. Chuẩn hóa dữ liệu
        String householdCode = request.householdCode()
                .trim()
                .toUpperCase(Locale.ROOT);

        String apartmentNo = request.apartmentNo()
                .trim()
                .toUpperCase(Locale.ROOT);

        // 3. Kiểm tra trùng
        Map<String, String> errors = new LinkedHashMap<>();

        if (householdRepository
                .existsByHouseholdCodeAndDeletedAtIsNullAndIdNot(
                        householdCode, id
                )) {
            errors.put("householdCode", "Mã hộ đã tồn tại");
        }

        if (householdRepository
                .existsByApartmentNoAndDeletedAtIsNullAndIdNot(
                        apartmentNo, id
                )) {
            errors.put("apartmentNo", "Số căn hộ đã tồn tại");
        }

        if (!errors.isEmpty()) {
            throw new HouseholdDuplicateException(errors);
        }

        // 4. Chỉ cập nhật 4 trường được phép
        household.setHouseholdCode(householdCode);
        household.setApartmentNo(apartmentNo);
        household.setFloor(request.floor());
        household.setArea(request.area());

        householdRepository.save(household);

        return toHouseholdResponse(household);
    }

    private HouseholdResponse toHouseholdResponse(Household household) {

        List<Resident> residents = household.getResidents()
                .stream()
                .filter(resident -> resident.getDeletedAt() == null)
                .toList();

        Resident head = residents.stream()
                .filter(Resident::isHead)
                .findFirst()
                .orElse(null);

        HouseholdResponse.HeadResponse headResponse = null;

        if (head != null) {
            headResponse = new HouseholdResponse.HeadResponse(
                    head.getId(),
                    head.getFullName(),
                    head.getDateOfBirth(),
                    head.getGender(),
                    head.getIdentityNumber(),
                    head.getPhone(),
                    head.isHead(),
                    head.getStatus()
            );
        }

        int residentCount = residents.size();

        int presentCount = (int) residents.stream()
                .filter(resident ->
                        resident.getStatus() == ResidencyStatus.RESIDING
                )
                .count();

        return new HouseholdResponse(
                household.getId(),
                household.getHouseholdCode(),
                household.getApartmentNo(),
                household.getFloor(),
                household.getArea(),
                household.getStatus(),
                household.getCreatedAt(),
                household.getUpdatedAt(),
                headResponse,
                residentCount,
                presentCount
        );
    }

    @Transactional
    public HouseholdResponse changeHouseholdHead(
            Long householdId,
            Long residentId
    ) {
        // 1. Tìm hộ gia đình
        Household household = householdRepository
                .findByIdAndDeletedAtIsNull(householdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "HOUSEHOLD_NOT_FOUND"
                ));

        // 2. Kiểm tra trạng thái hộ
        if (!"ACTIVE".equals(household.getStatus())) {
            throw new HouseholdInactiveException();
        }

        // 3. Tìm nhân khẩu mới
        Resident newHead = residentRepository
                .findByIdAndDeletedAtIsNull(residentId)
                .orElseThrow(() -> new InvalidNewHeadException(
                        "Nhân khẩu không tồn tại hoặc đã bị xóa",
                        "NOT_FOUND"
                ));

        // 4. Kiểm tra nhân khẩu có thuộc hộ này không
        if (!newHead.getHousehold().getId().equals(householdId)) {
            throw new InvalidNewHeadException(
                    "Người này không thuộc hộ gia đình",
                    "NOT_IN_HOUSEHOLD"
            );
        }

        // 5. Kiểm tra trạng thái cư trú
        if (newHead.getStatus() != ResidencyStatus.RESIDING) {
            throw new InvalidNewHeadException(
                    "Nhân khẩu không ở trạng thái đang cư trú",
                    "NOT_RESIDING"
            );
        }

        // 6. Kiểm tra đã là chủ hộ chưa
        if (newHead.isHead()) {
            throw new InvalidNewHeadException(
                    "Người này đã là chủ hộ",
                    "ALREADY_HEAD"
            );
        }

        // 7. Tìm chủ hộ hiện tại
        Resident currentHead = residentRepository
                .findByHouseholdIdAndHeadTrueAndDeletedAtIsNull(householdId)
                .orElseThrow(() -> new IllegalStateException(
                        "Hộ gia đình không có chủ hộ hiện tại"
                ));

        // 8. Chuyển chủ hộ
        currentHead.setHead(false);
        residentRepository.saveAndFlush(currentHead);

        // Chỉ cập nhật chủ hộ mới sau khi chủ hộ cũ đã được bỏ trạng thái
        newHead.setHead(true);
        residentRepository.saveAndFlush(newHead);

        // 9. Trả về HouseholdResponse
        return toHouseholdResponse(household);
    }

    @Transactional
    public HouseholdResponse updateHouseholdStatus(
            Long id,
            UpdateHouseholdStatusRequest request
    ) {
        // 1. Tìm hộ gia đình
        Household household = householdRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "HOUSEHOLD_NOT_FOUND"
                ));

        // 2. Cập nhật trạng thái
        household.setStatus(request.status().name());

        householdRepository.save(household);

        // 3. Trả về DTO
        return toHouseholdResponse(household);
    }
}