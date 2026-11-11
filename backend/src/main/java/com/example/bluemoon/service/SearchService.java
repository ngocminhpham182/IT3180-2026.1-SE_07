package com.example.bluemoon.service;

import com.example.bluemoon.dto.HouseholdListItemResponse;
import com.example.bluemoon.dto.SearchResidentProjection;
import com.example.bluemoon.dto.SearchResponse;
import com.example.bluemoon.exception.AppValidationException;
import com.example.bluemoon.model.enums.ResidencyStatus;
import com.example.bluemoon.model.enums.SearchType;
import com.example.bluemoon.repository.HouseholdRepository;
import com.example.bluemoon.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final HouseholdRepository householdRepository;
    private final ResidentRepository residentRepository;

    @Transactional(readOnly = true)
    public SearchResponse search(String keyword, String type, Integer limit) {
        // Validate keyword
        if (keyword == null || keyword.trim().length() < 2) {
            throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("keyword", "Nhập ít nhất 2 ký tự"));
        }
        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.length() > 100) {
            throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("keyword", "Từ khóa tìm kiếm không được quá 100 ký tự"));
        }

        // Validate type
        SearchType searchType;
        try {
            searchType = SearchType.valueOf(type != null ? type.trim().toUpperCase() : "ALL");
        } catch (IllegalArgumentException ex) {
            throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("type", "Loại tìm kiếm không hợp lệ"));
        }

        // Validate limit
        if (limit == null || limit < 1 || limit > 50) {
            throw new AppValidationException("Dữ liệu không hợp lệ", Map.of("limit", "Số lượng kết quả mỗi nhóm phải từ 1 đến 50"));
        }

        String escapedKeyword = escapeSqlLike(trimmedKeyword);

        // Search Households
        List<SearchResponse.SearchHouseholdItem> households;
        long totalHouseholds = 0;
        if (searchType == SearchType.ALL || searchType == SearchType.HOUSEHOLD) {
            List<HouseholdListItemResponse> householdResults = householdRepository.quickSearchHouseholds(escapedKeyword, limit);
            totalHouseholds = householdRepository.countQuickSearchHouseholds(escapedKeyword);
            households = householdResults.stream()
                    .map(h -> new SearchResponse.SearchHouseholdItem(
                            h.getId(),
                            h.getHouseholdCode(),
                            h.getApartmentNo(),
                            h.getFloor(),
                            h.getStatus(),
                            h.getHeadName()
                    ))
                    .toList();
        } else {
            households = Collections.emptyList();
        }

        // Search Residents
        List<SearchResponse.SearchResidentItem> residents;
        long totalResidents = 0;
        if (searchType == SearchType.ALL || searchType == SearchType.RESIDENT) {
            List<SearchResidentProjection> residentResults = residentRepository.quickSearchResidents(escapedKeyword, limit);
            totalResidents = residentRepository.countQuickSearchResidents(escapedKeyword);
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

            residents = residentResults.stream()
                    .map(r -> {
                        ResidencyStatus residencyStatus = r.getResidencyStatus() != null
                                ? ResidencyStatus.valueOf(r.getResidencyStatus())
                                : null;
                        boolean statusExpired = false;
                        if ((residencyStatus == ResidencyStatus.TEMPORARILY_ABSENT
                                || residencyStatus == ResidencyStatus.TEMPORARY_RESIDENCE)
                                && r.getStatusUntil() != null) {
                            statusExpired = r.getStatusUntil().isBefore(today);
                        }

                        return new SearchResponse.SearchResidentItem(
                                r.getId(),
                                r.getFullName(),
                                r.getDateOfBirth(),
                                r.getHouseholdId(),
                                r.getHouseholdCode(),
                                r.getApartmentNo(),
                                Boolean.TRUE.equals(r.getIsHead()),
                                residencyStatus,
                                r.getStatusUntil(),
                                statusExpired
                        );
                    })
                    .toList();
        } else {
            residents = Collections.emptyList();
        }

        boolean empty = households.isEmpty() && residents.isEmpty();
        SearchResponse.SearchTotals totals = new SearchResponse.SearchTotals(totalHouseholds, totalResidents);
        SearchResponse.SearchHasMore hasMore = new SearchResponse.SearchHasMore(
                totalHouseholds > households.size(),
                totalResidents > residents.size()
        );

        return new SearchResponse(empty, households, residents, totals, hasMore);
    }

    private String escapeSqlLike(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
