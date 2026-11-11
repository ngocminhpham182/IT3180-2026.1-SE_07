package com.example.bluemoon.service;

import com.example.bluemoon.dto.*;
import com.example.bluemoon.exception.*;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.model.BillingPeriod;
import com.example.bluemoon.model.enums.BillingPeriodStatus;
import com.example.bluemoon.repository.BillingPeriodRepository;
import com.example.bluemoon.repository.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingPeriodService {

    private final BillingPeriodRepository billingPeriodRepository;
    private final ReceivableRepository receivableRepository;

    @Transactional
    public BillingPeriodResponse create(BillingPeriodRequest request) {

        // 1. Kiểm tra khoảng ngày
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidBillingPeriodDateRangeException();
        }

        // 2. Kiểm tra code trùng
        String code = request.code().trim();

        if (billingPeriodRepository.existsByCode(code)) {
            throw new DuplicateException(
                    "code",
                    "Mã kỳ thu đã tồn tại"
            );
        }

        // 3. Kiểm tra khoảng ngày chồng lấn
        billingPeriodRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        request.endDate(),
                        request.startDate()
                )
                .ifPresent(existing -> {
                    throw new BillingPeriodOverlapException(
                            existing.getId(),
                            existing.getCode()
                    );
                });

        // 4. Tạo kỳ thu mới
        BillingPeriod period = new BillingPeriod();
        period.setCode(code);
        period.setName(request.name().trim());
        period.setStartDate(request.startDate());
        period.setEndDate(request.endDate());

        // Mặc định: OPEN, chưa lập danh sách
        period.setStatus(BillingPeriodStatus.OPEN);

        // Chưa lập danh sách nên các trường này phải null
        period.setGeneratedAt(null);
        period.setGeneratedBy(null);

        // Chưa đóng sổ
        period.setClosedAt(null);
        period.setClosedBy(null);

        BillingPeriod saved = billingPeriodRepository.save(period);

        return BillingPeriodResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<BillingPeriodResponse> getAll(
            Integer year,
            BillingPeriodStatus status,
            Boolean generated,
            int page,
            int size
    ) {
        // API page bắt đầu từ 1
        if (page < 1) {
            throw new IllegalArgumentException("page phải lớn hơn hoặc bằng 1");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size phải nằm trong khoảng 1 đến 100");
        }

        Specification<BillingPeriod> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<
                    jakarta.persistence.criteria.Predicate>();

            // Lọc theo năm của startDate
            if (year != null) {
                LocalDate yearStart = LocalDate.of(year, 1, 1);
                LocalDate nextYearStart = yearStart.plusYears(1);

                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("startDate"),
                                yearStart
                        )
                );

                predicates.add(
                        cb.lessThan(
                                root.get("startDate"),
                                nextYearStart
                        )
                );
            }

            // Lọc theo trạng thái OPEN / CLOSED
            if (status != null) {
                predicates.add(
                        cb.equal(root.get("status"), status)
                );
            }

            // Lọc theo tình trạng đã lập danh sách
            if (generated != null) {
                if (generated) {
                    predicates.add(
                            cb.isNotNull(root.get("generatedAt"))
                    );
                } else {
                    predicates.add(
                            cb.isNull(root.get("generatedAt"))
                    );
                }
            }

            return cb.and(predicates.toArray(
                    new jakarta.persistence.criteria.Predicate[0]
            ));
        };

        Page<BillingPeriod> result = billingPeriodRepository.findAll(
                spec,
                PageRequest.of(
                        page - 1,
                        size,
                        Sort.by(Sort.Direction.DESC, "startDate")
                )
        );

        List<BillingPeriodResponse> content = result.getContent()
                .stream()
                .map(BillingPeriodResponse::from)
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
    public BillingPeriodDetailResponse getById(Long id) {

        BillingPeriod period = billingPeriodRepository.findWithUsersById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy kỳ thu")
                );

        BillingPeriodSummaryResponse summary =
                receivableRepository.getSummaryByBillingPeriodId(id)
                        .orElse(BillingPeriodSummaryResponse.empty());

        return BillingPeriodDetailResponse.from(period, summary);
    }

    @Transactional
    public BillingPeriodResponse update(Long id, BillingPeriodRequest request) {
        BillingPeriod period = billingPeriodRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy kỳ thu"));

        // Không cho sửa kỳ đã lập danh sách
        if (period.getGeneratedAt() != null) {
            throw new BillingPeriodAlreadyGeneratedException(
                    period.getGeneratedAt()
            );
        }


        validateDateRange(request.startDate(), request.endDate());

        // Kiểm tra trùng mã, loại trừ chính kỳ đang sửa
        if (billingPeriodRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateException(
                    "code",
                    "Mã kỳ thu đã tồn tại"
            );
        }

        // Kiểm tra khoảng thời gian chồng lấn, loại trừ chính kỳ đang sửa
        billingPeriodRepository.findOverlappingPeriod(
                id,
                request.startDate(),
                request.endDate()
        ).ifPresent(overlapping -> {
            throw new BillingPeriodOverlapException(
                    overlapping.getId(),
                    overlapping.getCode()
            );
        });

        period.setCode(request.code());
        period.setName(request.name());
        period.setStartDate(request.startDate());
        period.setEndDate(request.endDate());

        // Không thay đổi status, generatedAt, generatedBy, closedAt, closedBy.
        // Entity đang managed nên không bắt buộc gọi save().
        return BillingPeriodResponse.from(period);
    }

    @Transactional
    public void delete(Long id) {
        BillingPeriod period = billingPeriodRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy kỳ thu"));

        if (period.getGeneratedAt() != null) {
            throw new BillingPeriodAlreadyGeneratedException(
                    period.getGeneratedAt()
            );
        }

        billingPeriodRepository.delete(period);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidBillingPeriodDateRangeException();
        }
    }

    @Transactional
    public BillingPeriodResponse updateStatus(
            Long id,
            BillingPeriodStatusRequest request,
            AppUser currentUser
    ) {
        BillingPeriod period = billingPeriodRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy kỳ thu"));

        BillingPeriodStatus newStatus = request.status();

        // Đã đúng trạng thái hiện tại: không thay đổi gì
        if (period.getStatus() == newStatus) {
            return BillingPeriodResponse.from(period);
        }

        if (newStatus == BillingPeriodStatus.CLOSED) {

            // Chỉ đóng kỳ đã lập danh sách
            if (period.getGeneratedAt() == null) {
                throw new PeriodNotGeneratedException();
            }

            period.setStatus(BillingPeriodStatus.CLOSED);
            period.setClosedAt(Instant.now());
            period.setClosedBy(currentUser);

        } else if (newStatus == BillingPeriodStatus.OPEN) {

            period.setStatus(BillingPeriodStatus.OPEN);
            period.setClosedAt(null);
            period.setClosedBy(null);
        }

        return BillingPeriodResponse.from(period);
    }
}