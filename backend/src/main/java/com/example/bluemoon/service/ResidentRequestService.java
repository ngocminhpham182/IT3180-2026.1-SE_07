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
import com.example.bluemoon.repository.specification.ResidentRequestSpecification;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ResidentRequestService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate MIN_DOB = LocalDate.of(1900, 1, 1);
    private static final List<String> PROFILE_FIELDS = List.of(
            "fullName", "dateOfBirth", "gender", "identityNumber", "phone", "relationshipToHead"
    );

    private final ResidentRequestRepository residentRequestRepository;
    private final ResidentRepository residentRepository;
    private final ResidentMovementRepository residentMovementRepository;
    private final ResidentService residentService;
    private final Validator validator;

    @Transactional(readOnly = true)
    public MyHouseholdResponse getMyHousehold(AppUser currentUser) {
        if (currentUser.getResidentId() == null) {
            throw new ResourceNotFoundException("Tài khoản chưa gắn với hộ gia đình");
        }

        Resident myResident = residentRepository.findByIdAndDeletedAtIsNull(currentUser.getResidentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản chưa gắn với hộ gia đình"));

        Household household = myResident.getHousehold();
        if (household == null || household.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Tài khoản chưa gắn với hộ gia đình");
        }

        List<ResidencyStatus> activeStatuses = List.of(
                ResidencyStatus.RESIDING,
                ResidencyStatus.TEMPORARILY_ABSENT,
                ResidencyStatus.TEMPORARY_RESIDENCE
        );
        Sort sort = Sort.by(Sort.Order.desc("head"), Sort.Order.asc("dateOfBirth"));
        List<ResidentInHouseholdResponse> residents = residentRepository
                .findByHousehold_IdAndDeletedAtIsNullAndStatusIn(household.getId(), activeStatuses, sort)
                .stream()
                .map(this::toResidentInHouseholdResponse)
                .toList();

        List<MyHouseholdResponse.PendingRequestItem> pendingRequests = residentRequestRepository
                .findByHousehold_IdAndStatus(household.getId(), ResidentRequestStatus.PENDING)
                .stream()
                .map(r -> new MyHouseholdResponse.PendingRequestItem(
                        r.getId(),
                        r.getType(),
                        r.getResident() != null ? r.getResident().getId() : null
                ))
                .toList();

        MyHouseholdResponse.HouseholdInfo householdInfo = new MyHouseholdResponse.HouseholdInfo(
                household.getId(),
                household.getHouseholdCode(),
                household.getApartmentNo(),
                household.getFloor(),
                household.getArea(),
                household.getStatus()
        );

        return new MyHouseholdResponse(
                householdInfo,
                myResident.getId(),
                residents,
                pendingRequests
        );
    }

    @Transactional
    public ResidentRequestResponse submit(AppUser currentUser, SubmitResidentRequest req) {
        // 1. Caller must be linked to a resident
        if (currentUser.getResidentId() == null) {
            throw new ResidentLinkRequiredException();
        }
        Resident callerResident = residentRepository.findByIdAndDeletedAtIsNull(currentUser.getResidentId())
                .orElseThrow(ResidentLinkRequiredException::new);

        // 2. Check body and type
        if (req == null || req.type() == null) {
            throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("type", "Loại đề nghị không được để trống"));
        }

        // 3. Household must be ACTIVE
        Household household = callerResident.getHousehold();
        if (household == null || household.getDeletedAt() != null || !"ACTIVE".equalsIgnoreCase(household.getStatus())) {
            throw new HouseholdInactiveException();
        }

        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        Resident targetResident = null;
        Map<String, Object> proposedMap;
        Map<String, Object> beforeMap = null;

        switch (req.type()) {
            case PROFILE_UPDATE -> {
                // 4. residentId must belong to caller's household
                targetResident = resolveHouseholdResident(req.residentId(), household.getId());

                // 5. Validate profile fields and note
                validateProfileFields(req, today);

                // 6. Check resident status
                if (targetResident.getStatus() == ResidencyStatus.MOVED_OUT
                        || targetResident.getStatus() == ResidencyStatus.DECEASED) {
                    throw new ResidentNotResidingException(targetResident.getStatus().name());
                }

                // 7. Check if anything changed
                beforeMap = buildProfileMap(targetResident);
                proposedMap = buildProfileMap(req.profile());
                if (calculateChanges(beforeMap, proposedMap).isEmpty()) {
                    throw new NoChangesException();
                }

                // 8. Check duplicate identityNumber
                String newIdentity = req.profile().identityNumber();
                if (newIdentity != null
                        && !Objects.equals(newIdentity, targetResident.getIdentityNumber())
                        && residentRepository.existsByIdentityNumberAndDeletedAtIsNullAndIdNot(newIdentity, targetResident.getId())) {
                    throw new DuplicateException("profile.identityNumber", "Số CCCD/CMND đã tồn tại");
                }

                // 9. Check pending request exists
                Optional<ResidentRequest> existingPending = residentRequestRepository
                        .findFirstByResident_IdAndTypeAndStatus(
                                targetResident.getId(),
                                ResidentRequestType.PROFILE_UPDATE,
                                ResidentRequestStatus.PENDING
                        );
                if (existingPending.isPresent()) {
                    throw new PendingRequestExistsException(existingPending.get().getId());
                }
            }
            case TEMPORARY_ABSENCE -> {
                // 4. residentId must belong to caller's household
                targetResident = resolveHouseholdResident(req.residentId(), household.getId());

                // 5. Validate dates and note
                validateAbsenceDates(req, targetResident, today);

                // 6. Check resident status
                if (targetResident.getStatus() == ResidencyStatus.MOVED_OUT
                        || targetResident.getStatus() == ResidencyStatus.DECEASED) {
                    throw new ResidentNotResidingException(targetResident.getStatus().name());
                }
                if (targetResident.getStatus() != ResidencyStatus.RESIDING) {
                    throw new InvalidTransitionException(
                            targetResident.getStatus().name(),
                            List.of(ResidencyStatus.RESIDING.name())
                    );
                }

                // 9. Check pending request exists
                Optional<ResidentRequest> existingPending = residentRequestRepository
                        .findFirstByResident_IdAndTypeAndStatus(
                                targetResident.getId(),
                                ResidentRequestType.TEMPORARY_ABSENCE,
                                ResidentRequestStatus.PENDING
                        );
                if (existingPending.isPresent()) {
                    throw new PendingRequestExistsException(existingPending.get().getId());
                }

                proposedMap = new LinkedHashMap<>();
                proposedMap.put("startDate", req.startDate().toString());
                proposedMap.put("endDate", req.endDate().toString());
            }
            case TEMPORARY_RESIDENCE -> {
                // 5. Validate dates, person and note
                validateResidenceFields(req, today);

                // 8. Check duplicate identityNumber
                String guestIdentity = req.person().identityNumber();
                if (guestIdentity != null && residentRepository.existsByIdentityNumberAndDeletedAtIsNull(guestIdentity)) {
                    throw new DuplicateException("person.identityNumber", "Số CCCD/CMND đã tồn tại");
                }

                // 9. Check pending request exists by guest identityNumber
                if (guestIdentity != null) {
                    Optional<Long> existingPendingId = residentRequestRepository
                            .findPendingTemporaryResidenceIdByIdentityNumber(guestIdentity);
                    if (existingPendingId.isPresent()) {
                        throw new PendingRequestExistsException(existingPendingId.get());
                    }
                }

                proposedMap = new LinkedHashMap<>();
                proposedMap.put("startDate", req.startDate().toString());
                proposedMap.put("endDate", req.endDate().toString());
                proposedMap.put("person", buildGuestPersonMap(req.person()));
            }
            default -> throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("type", "Loại đề nghị không hợp lệ"));
        }

        ResidentRequest entity = ResidentRequest.builder()
                .type(req.type())
                .status(ResidentRequestStatus.PENDING)
                .household(household)
                .resident(targetResident)
                .submittedBy(currentUser)
                .submittedAt(Instant.now())
                .note(req.note())
                .proposed(proposedMap)
                .beforeValues(beforeMap)
                .build();

        try {
            entity = residentRequestRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            Long existingId = findConflictingPendingId(req, targetResident);
            if (existingId != null) {
                throw new PendingRequestExistsException(existingId);
            }
            throw ex;
        }

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResidentRequestResponse> listMine(
            AppUser currentUser,
            String statusParam,
            String typeParam,
            int page,
            int size
    ) {
        validatePagination(page, size);
        List<ResidentRequestStatus> statuses = parseStatuses(statusParam, "ALL");
        ResidentRequestType type = parseType(typeParam);

        Specification<ResidentRequest> spec = Specification.allOf(
                ResidentRequestSpecification.submittedByUserId(currentUser.getId()),
                ResidentRequestSpecification.hasStatusIn(statuses),
                ResidentRequestSpecification.hasType(type)
        );

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id"))
        );

        Page<ResidentRequest> resultPage = residentRequestRepository.findAll(spec, pageable);
        List<ResidentRequestResponse> content = resultPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                page,
                size,
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ResidentRequestResponse getMine(Long id, AppUser currentUser) {
        ResidentRequest request = residentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề nghị"));

        if (!request.getSubmittedBy().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy đề nghị");
        }

        return toResponse(request);
    }

    @Transactional
    public ResidentRequestResponse cancel(Long id, AppUser currentUser) {
        ResidentRequest request = residentRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề nghị"));

        if (!request.getSubmittedBy().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy đề nghị");
        }

        if (request.getStatus() != ResidentRequestStatus.PENDING) {
            throw new RequestNotPendingException(request.getStatus());
        }

        request.setStatus(ResidentRequestStatus.CANCELLED);
        request.setDecidedBy(currentUser);
        request.setDecidedAt(Instant.now());

        request = residentRequestRepository.save(request);
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResidentRequestResponse> listAll(
            String statusParam,
            String typeParam,
            Long householdId,
            String keyword,
            int page,
            int size
    ) {
        validatePagination(page, size);
        List<ResidentRequestStatus> statuses = parseStatuses(statusParam, "PENDING");
        ResidentRequestType type = parseType(typeParam);

        Specification<ResidentRequest> spec = Specification.allOf(
                ResidentRequestSpecification.hasStatusIn(statuses),
                ResidentRequestSpecification.hasType(type),
                ResidentRequestSpecification.hasHouseholdId(householdId),
                ResidentRequestSpecification.hasKeyword(keyword)
        );

        boolean onlyPending = statuses.size() == 1 && statuses.contains(ResidentRequestStatus.PENDING);
        Sort sort = onlyPending
                ? Sort.by(Sort.Order.asc("submittedAt"), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id"));

        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<ResidentRequest> resultPage = residentRequestRepository.findAll(spec, pageable);
        List<ResidentRequestResponse> content = resultPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                page,
                size,
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ResidentRequestResponse getById(Long id) {
        ResidentRequest request = residentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề nghị"));
        return toResponse(request);
    }

    @Transactional
    public ResidentRequestResponse approve(Long id, ApproveRequest approveReq, AppUser bqtUser) {
        ResidentRequest request = residentRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề nghị"));

        if (request.getStatus() != ResidentRequestStatus.PENDING) {
            throw new RequestNotPendingException(request.getStatus());
        }

        Household household = request.getHousehold();
        if (household == null || household.getDeletedAt() != null || !"ACTIVE".equalsIgnoreCase(household.getStatus())) {
            throw new HouseholdInactiveException();
        }

        Long resultResidentId = null;
        Long resultMovementId = null;

        switch (request.getType()) {
            case PROFILE_UPDATE -> {
                Resident resident = request.getResident() != null
                        ? residentRepository.findByIdForUpdate(request.getResident().getId()).orElse(null)
                        : null;
                if (resident == null || resident.getDeletedAt() != null) {
                    throw new RequestOutdatedException(
                            Collections.emptyList(),
                            "Nhân khẩu không còn tồn tại trong hệ thống"
                    );
                }

                List<String> changes = calculateChanges(request.getBeforeValues(), request.getProposed());
                List<String> outdatedFields = findOutdatedFields(resident, request.getBeforeValues(), changes);
                if (!outdatedFields.isEmpty()) {
                    throw new RequestOutdatedException(
                            outdatedFields,
                            "Dữ liệu nhân khẩu đã thay đổi kể từ khi gửi đề nghị"
                    );
                }

                if (changes.contains("identityNumber")) {
                    String newIdentity = getString(request.getProposed(), "identityNumber");
                    if (newIdentity != null
                            && residentRepository.existsByIdentityNumberAndDeletedAtIsNullAndIdNot(newIdentity, resident.getId())) {
                        throw new DuplicateException("profile.identityNumber", "Số CCCD/CMND đã tồn tại");
                    }
                }

                applyProfileChanges(resident, request.getProposed(), changes);
                residentRepository.save(resident);
                resultResidentId = resident.getId();
            }
            case TEMPORARY_ABSENCE -> {
                if (request.getResident() == null) {
                    throw new ResourceNotFoundException("Không tìm thấy nhân khẩu");
                }
                LocalDate startDate = LocalDate.parse(Objects.requireNonNull(getString(request.getProposed(), "startDate")));
                LocalDate endDate = LocalDate.parse(Objects.requireNonNull(getString(request.getProposed(), "endDate")));
                String movementNote = buildMovementNote(request);

                CreateMovementRequest movementReq = new CreateMovementRequest(
                        MovementType.TEMPORARY_ABSENCE,
                        null,
                        startDate,
                        endDate,
                        null,
                        null,
                        null,
                        movementNote
                );

                RecordMovementResponse movementRes = residentService.recordMovement(
                        request.getResident().getId(),
                        movementReq,
                        bqtUser
                );
                resultResidentId = request.getResident().getId();
                resultMovementId = movementRes.movement().id();
            }
            case TEMPORARY_RESIDENCE -> {
                LocalDate startDate = LocalDate.parse(Objects.requireNonNull(getString(request.getProposed(), "startDate")));
                LocalDate endDate = LocalDate.parse(Objects.requireNonNull(getString(request.getProposed(), "endDate")));

                @SuppressWarnings("unchecked")
                Map<String, Object> personMap = (Map<String, Object>) request.getProposed().get("person");
                String identityNumber = getString(personMap, "identityNumber");
                if (identityNumber != null && residentRepository.existsByIdentityNumberAndDeletedAtIsNull(identityNumber)) {
                    throw new DuplicateException("person.identityNumber", "Số CCCD/CMND đã tồn tại");
                }

                String genderStr = getString(personMap, "gender");
                Gender gender = genderStr != null ? Gender.valueOf(genderStr) : null;
                LocalDate dob = LocalDate.parse(Objects.requireNonNull(getString(personMap, "dateOfBirth")));

                CreateResidentRequest createReq = new CreateResidentRequest(
                        getString(personMap, "fullName"),
                        dob,
                        gender,
                        identityNumber,
                        getString(personMap, "phone"),
                        getString(personMap, "relationshipToHead"),
                        ResidentEntryType.TEMPORARY_RESIDENCE,
                        startDate,
                        endDate,
                        null
                );

                try {
                    ResidentResponse created = residentService.createResident(household.getId(), createReq, bqtUser);
                    resultResidentId = created.id();
                } catch (DuplicateException ex) {
                    throw new DuplicateException("person.identityNumber", ex.getMessage());
                }
            }
        }

        request.setStatus(ResidentRequestStatus.APPROVED);
        request.setDecidedBy(bqtUser);
        request.setDecidedAt(Instant.now());
        request.setDecisionNote(approveReq != null ? approveReq.note() : null);
        request.setResultResidentId(resultResidentId);
        request.setResultMovementId(resultMovementId);

        request = residentRequestRepository.save(request);
        return toResponse(request);
    }

    @Transactional
    public ResidentRequestResponse reject(Long id, RejectRequest rejectReq, AppUser bqtUser) {
        ResidentRequest request = residentRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề nghị"));

        if (request.getStatus() != ResidentRequestStatus.PENDING) {
            throw new RequestNotPendingException(request.getStatus());
        }

        request.setStatus(ResidentRequestStatus.REJECTED);
        request.setDecidedBy(bqtUser);
        request.setDecidedAt(Instant.now());
        request.setDecisionReason(rejectReq.reason().trim());

        request = residentRequestRepository.save(request);
        return toResponse(request);
    }

    private Resident resolveHouseholdResident(Long residentId, Long householdId) {
        if (residentId == null) {
            throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("residentId", "Vui lòng chọn nhân khẩu"));
        }
        Optional<Resident> opt = residentRepository.findByIdAndDeletedAtIsNull(residentId);
        if (opt.isEmpty() || opt.get().getHousehold() == null || !opt.get().getHousehold().getId().equals(householdId)) {
            throw new ResidentNotInHouseholdException("Nhân khẩu không thuộc hộ gia đình của bạn");
        }
        return opt.get();
    }

    private void validateNote(SubmitResidentRequest req, Map<String, String> errors) {
        if (req.note() != null && req.note().length() > 500) {
            errors.put("note", "Ghi chú tối đa 500 ký tự");
        }
    }

    private void validateResidentInfo(String prefix, ResidentInfo info, LocalDate today, Map<String, String> errors) {
        Set<ConstraintViolation<ResidentInfo>> violations = validator.validate(info);
        for (ConstraintViolation<ResidentInfo> v : violations) {
            errors.putIfAbsent(prefix + "." + v.getPropertyPath().toString(), v.getMessage());
        }
        LocalDate dob = info.dateOfBirth();
        if (dob != null) {
            if (dob.isAfter(today)) {
                errors.put(prefix + ".dateOfBirth", "Ngày sinh không được ở tương lai");
            } else if (dob.isBefore(MIN_DOB)) {
                errors.put(prefix + ".dateOfBirth", "Ngày sinh phải từ 01/01/1900 đến hiện tại");
            }
        }
    }

    private void validateProfileFields(SubmitResidentRequest req, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateNote(req, errors);
        if (req.profile() == null) {
            errors.put("profile", "Thông tin nhân khẩu không được để trống");
        } else {
            validateResidentInfo("profile", req.profile(), today, errors);
        }
        if (!errors.isEmpty()) {
            throw new AppValidationException("Dữ liệu không hợp lệ", errors);
        }
    }

    private void validateAbsenceDates(SubmitResidentRequest req, Resident target, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateNote(req, errors);
        if (req.startDate() == null) {
            errors.put("startDate", "Ngày bắt đầu không được để trống");
        }
        if (req.endDate() == null) {
            errors.put("endDate", "Ngày kết thúc không được để trống");
        }
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            errors.put("endDate", "Ngày kết thúc không được sớm hơn ngày bắt đầu");
        }
        if (req.startDate() != null) {
            if (req.startDate().isAfter(today)) {
                errors.put("startDate", "Ngày bắt đầu không được ở tương lai");
            } else if (target.getDateOfBirth() != null && req.startDate().isBefore(target.getDateOfBirth())) {
                errors.put("startDate", "Ngày bắt đầu không được trước ngày sinh");
            } else {
                residentMovementRepository
                        .findFirstByResident_IdOrderByEffectiveDateDescRecordedAtDescIdDesc(target.getId())
                        .ifPresent(latest -> {
                            if (latest.getEffectiveDate() != null && req.startDate().isBefore(latest.getEffectiveDate())) {
                                errors.put("startDate", "Ngày bắt đầu không được trước ngày biến động gần nhất");
                            }
                        });
            }
        }
        if (!errors.isEmpty()) {
            throw new AppValidationException("Dữ liệu không hợp lệ", errors);
        }
    }

    private void validateResidenceFields(SubmitResidentRequest req, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateNote(req, errors);
        if (req.startDate() == null) {
            errors.put("startDate", "Ngày bắt đầu không được để trống");
        }
        if (req.endDate() == null) {
            errors.put("endDate", "Ngày kết thúc không được để trống");
        }
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            errors.put("endDate", "Ngày kết thúc không được sớm hơn ngày bắt đầu");
        }
        if (req.startDate() != null && req.startDate().isAfter(today)) {
            errors.put("startDate", "Ngày bắt đầu không được ở tương lai");
        }
        if (req.person() == null) {
            errors.put("person", "Thông tin khách tạm trú không được để trống");
        } else {
            validateResidentInfo("person", req.person(), today, errors);
            LocalDate dob = req.person().dateOfBirth();
            if (req.startDate() != null && dob != null && req.startDate().isBefore(dob) && !errors.containsKey("startDate")) {
                errors.put("startDate", "Ngày bắt đầu không được trước ngày sinh");
            }
        }
        if (!errors.isEmpty()) {
            throw new AppValidationException("Dữ liệu không hợp lệ", errors);
        }
    }

    private Long findConflictingPendingId(SubmitResidentRequest req, Resident targetResident) {
        return switch (req.type()) {
            case PROFILE_UPDATE -> targetResident != null
                    ? residentRequestRepository.findFirstByResident_IdAndTypeAndStatus(
                            targetResident.getId(), ResidentRequestType.PROFILE_UPDATE, ResidentRequestStatus.PENDING)
                    .map(ResidentRequest::getId).orElse(null)
                    : null;
            case TEMPORARY_ABSENCE -> targetResident != null
                    ? residentRequestRepository.findFirstByResident_IdAndTypeAndStatus(
                            targetResident.getId(), ResidentRequestType.TEMPORARY_ABSENCE, ResidentRequestStatus.PENDING)
                    .map(ResidentRequest::getId).orElse(null)
                    : null;
            case TEMPORARY_RESIDENCE -> req.person() != null && req.person().identityNumber() != null
                    ? residentRequestRepository.findPendingTemporaryResidenceIdByIdentityNumber(req.person().identityNumber())
                    .orElse(null)
                    : null;
        };
    }

    private Map<String, Object> buildProfileMap(Resident r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fullName", r.getFullName());
        map.put("dateOfBirth", r.getDateOfBirth() != null ? r.getDateOfBirth().toString() : null);
        map.put("gender", r.getGender() != null ? r.getGender().name() : null);
        map.put("identityNumber", r.getIdentityNumber());
        map.put("phone", r.getPhone());
        map.put("relationshipToHead", r.getRelationshipToHead());
        return map;
    }

    private Map<String, Object> buildProfileMap(ResidentInfo info) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fullName", info.fullName());
        map.put("dateOfBirth", info.dateOfBirth() != null ? info.dateOfBirth().toString() : null);
        map.put("gender", info.gender() != null ? info.gender().name() : null);
        map.put("identityNumber", info.identityNumber());
        map.put("phone", info.phone());
        map.put("relationshipToHead", info.relationshipToHead());
        return map;
    }

    private Map<String, Object> buildGuestPersonMap(ResidentInfo info) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fullName", info.fullName());
        map.put("dateOfBirth", info.dateOfBirth() != null ? info.dateOfBirth().toString() : null);
        if (info.gender() != null) map.put("gender", info.gender().name());
        if (info.identityNumber() != null) map.put("identityNumber", info.identityNumber());
        if (info.phone() != null) map.put("phone", info.phone());
        map.put("relationshipToHead", info.relationshipToHead());
        return map;
    }

    private List<String> calculateChanges(Map<String, Object> before, Map<String, Object> proposed) {
        if (before == null || proposed == null) {
            return Collections.emptyList();
        }
        List<String> changes = new ArrayList<>();
        for (String field : PROFILE_FIELDS) {
            String bVal = getString(before, field);
            String pVal = getString(proposed, field);
            if (!Objects.equals(bVal, pVal)) {
                changes.add(field);
            }
        }
        return changes;
    }

    private List<String> findOutdatedFields(Resident current, Map<String, Object> before, List<String> changes) {
        Map<String, Object> currentMap = buildProfileMap(current);
        List<String> outdated = new ArrayList<>();
        for (String field : changes) {
            String currVal = getString(currentMap, field);
            String beforeVal = getString(before, field);
            if (!Objects.equals(currVal, beforeVal)) {
                outdated.add(field);
            }
        }
        return outdated;
    }

    private void applyProfileChanges(Resident resident, Map<String, Object> proposed, List<String> changes) {
        for (String field : changes) {
            String val = getString(proposed, field);
            switch (field) {
                case "fullName" -> resident.setFullName(val);
                case "dateOfBirth" -> resident.setDateOfBirth(val != null ? LocalDate.parse(val) : null);
                case "gender" -> resident.setGender(val != null ? Gender.valueOf(val) : null);
                case "identityNumber" -> resident.setIdentityNumber(val);
                case "phone" -> resident.setPhone(val);
                case "relationshipToHead" -> resident.setRelationshipToHead(val);
            }
        }
    }

    private String buildMovementNote(ResidentRequest request) {
        String base = "Theo khai báo #" + request.getId();
        if (request.getNote() != null && !request.getNote().isBlank()) {
            base = base + ": " + request.getNote().trim();
        }
        return base.length() > 500 ? base.substring(0, 500) : base;
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val == null) return null;
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? null : s;
    }

    private void validatePagination(int page, int size) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (page < 1) {
            errors.put("page", "Số trang phải lớn hơn hoặc bằng 1");
        }
        if (size < 1 || size > 100) {
            errors.put("size", "Kích thước trang phải từ 1 đến 100");
        }
        if (!errors.isEmpty()) {
            throw new AppValidationException("Dữ liệu không hợp lệ", errors);
        }
    }

    private List<ResidentRequestStatus> parseStatuses(String statusParam, String defaultValue) {
        String effective = (statusParam == null || statusParam.isBlank()) ? defaultValue : statusParam.trim();
        if ("ALL".equalsIgnoreCase(effective)) {
            return List.of(ResidentRequestStatus.values());
        }
        List<ResidentRequestStatus> result = new ArrayList<>();
        for (String part : effective.split(",")) {
            String token = part.trim().toUpperCase();
            if (token.isEmpty()) continue;
            try {
                result.add(ResidentRequestStatus.valueOf(token));
            } catch (IllegalArgumentException ex) {
                throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("status", "Trạng thái đề nghị không hợp lệ"));
            }
        }
        if (result.isEmpty()) {
            throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("status", "Trạng thái đề nghị không hợp lệ"));
        }
        return result;
    }

    private ResidentRequestType parseType(String typeParam) {
        if (typeParam == null || typeParam.isBlank()) {
            return null;
        }
        try {
            return ResidentRequestType.valueOf(typeParam.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("type", "Loại đề nghị không hợp lệ"));
        }
    }

    private ResidentInHouseholdResponse toResidentInHouseholdResponse(Resident r) {
        return new ResidentInHouseholdResponse(
                r.getId(),
                r.getFullName(),
                r.getDateOfBirth(),
                r.getGender(),
                r.getIdentityNumber(),
                r.getPhone(),
                r.getRelationshipToHead(),
                r.isHead(),
                r.getStatus(),
                r.getStatusUntil(),
                r.getMoveInDate(),
                r.getMoveOutDate(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    public ResidentRequestResponse toResponse(ResidentRequest request) {
        Household h = request.getHousehold();
        ResidentRequestResponse.HouseholdRef householdRef = new ResidentRequestResponse.HouseholdRef(
                h.getId(),
                h.getHouseholdCode(),
                h.getApartmentNo()
        );

        ResidentRequestResponse.ResidentRef residentRef = null;
        if (request.getResident() != null) {
            residentRef = new ResidentRequestResponse.ResidentRef(
                    request.getResident().getId(),
                    request.getResident().getFullName()
            );
        } else if (request.getResultResidentId() != null) {
            Resident created = residentRepository.findById(request.getResultResidentId()).orElse(null);
            if (created != null) {
                residentRef = new ResidentRequestResponse.ResidentRef(created.getId(), created.getFullName());
            }
        }

        AppUser submitter = request.getSubmittedBy();
        ResidentRequestResponse.UserRef submittedByRef = new ResidentRequestResponse.UserRef(
                submitter.getId(),
                submitter.getFullName()
        );

        Map<String, Object> before = null;
        List<String> changes = null;
        if (request.getType() == ResidentRequestType.PROFILE_UPDATE) {
            before = request.getBeforeValues();
            changes = calculateChanges(request.getBeforeValues(), request.getProposed());
        }

        ResidentRequestResponse.DecisionRef decisionRef = null;
        if (request.getStatus() != ResidentRequestStatus.PENDING) {
            ResidentRequestResponse.UserRef decidedByRef = null;
            if (request.getDecidedBy() != null) {
                decidedByRef = new ResidentRequestResponse.UserRef(
                        request.getDecidedBy().getId(),
                        request.getDecidedBy().getFullName()
                );
            }
            decisionRef = new ResidentRequestResponse.DecisionRef(
                    request.getStatus(),
                    decidedByRef,
                    request.getDecidedAt(),
                    request.getDecisionReason(),
                    request.getDecisionNote()
            );
        }

        ResidentRequestResponse.ResultRef resultRef = null;
        if (request.getStatus() == ResidentRequestStatus.APPROVED) {
            resultRef = new ResidentRequestResponse.ResultRef(
                    request.getResultResidentId(),
                    request.getResultMovementId()
            );
        }

        return new ResidentRequestResponse(
                request.getId(),
                request.getType(),
                request.getStatus(),
                householdRef,
                residentRef,
                submittedByRef,
                request.getSubmittedAt(),
                request.getNote(),
                request.getProposed(),
                before,
                changes,
                decisionRef,
                resultRef
        );
    }
}
