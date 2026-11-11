package com.example.bluemoon.service;

import com.example.bluemoon.dto.HouseholdListItemResponse;
import com.example.bluemoon.dto.SearchResidentProjection;
import com.example.bluemoon.dto.SearchResponse;
import com.example.bluemoon.exception.AppValidationException;
import com.example.bluemoon.model.enums.ResidencyStatus;
import com.example.bluemoon.repository.HouseholdRepository;
import com.example.bluemoon.repository.ResidentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private ResidentRepository residentRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    @DisplayName("[US-04 AC1] Tìm kiếm thành công cả hộ và nhân khẩu")
    void search_all_success() {
        HouseholdListItemResponse mockHousehold = mock(HouseholdListItemResponse.class);
        when(mockHousehold.getId()).thenReturn(1L);
        when(mockHousehold.getHouseholdCode()).thenReturn("H001");
        when(mockHousehold.getApartmentNo()).thenReturn("A101");
        when(mockHousehold.getFloor()).thenReturn((short) 1);
        when(mockHousehold.getStatus()).thenReturn("ACTIVE");
        when(mockHousehold.getHeadName()).thenReturn("Nguyễn Văn An");

        SearchResidentProjection mockResident = mock(SearchResidentProjection.class);
        when(mockResident.getId()).thenReturn(5L);
        when(mockResident.getFullName()).thenReturn("Nguyễn Văn A");
        when(mockResident.getDateOfBirth()).thenReturn(LocalDate.of(2000, 1, 1));
        when(mockResident.getHouseholdId()).thenReturn(1L);
        when(mockResident.getHouseholdCode()).thenReturn("H001");
        when(mockResident.getApartmentNo()).thenReturn("A101");
        when(mockResident.getIsHead()).thenReturn(false);
        when(mockResident.getResidencyStatus()).thenReturn("MOVED_OUT");
        when(mockResident.getStatusUntil()).thenReturn(null);

        when(householdRepository.quickSearchHouseholds("nguyen", 20)).thenReturn(List.of(mockHousehold));
        when(householdRepository.countQuickSearchHouseholds("nguyen")).thenReturn(1L);
        when(residentRepository.quickSearchResidents("nguyen", 20)).thenReturn(List.of(mockResident));
        when(residentRepository.countQuickSearchResidents("nguyen")).thenReturn(1L);

        SearchResponse response = searchService.search("nguyen", "ALL", 20);

        assertNotNull(response);
        assertFalse(response.empty());
        assertEquals(1, response.households().size());
        assertEquals(1, response.residents().size());
        assertEquals("H001", response.households().get(0).householdCode());
        assertEquals("Nguyễn Văn A", response.residents().get(0).fullName());
        assertEquals(ResidencyStatus.MOVED_OUT, response.residents().get(0).residencyStatus());
        assertEquals(1L, response.totals().households());
        assertEquals(1L, response.totals().residents());
        assertFalse(response.hasMore().households());
        assertFalse(response.hasMore().residents());
    }

    @Test
    @DisplayName("[US-04 AC2] Không có kết quả -> empty = true và hai mảng rỗng")
    void search_noResults_returnsEmptyTrue() {
        when(householdRepository.quickSearchHouseholds("xyz_not_found", 20)).thenReturn(Collections.emptyList());
        when(householdRepository.countQuickSearchHouseholds("xyz_not_found")).thenReturn(0L);
        when(residentRepository.quickSearchResidents("xyz_not_found", 20)).thenReturn(Collections.emptyList());
        when(residentRepository.countQuickSearchResidents("xyz_not_found")).thenReturn(0L);

        SearchResponse response = searchService.search("xyz_not_found", "ALL", 20);

        assertNotNull(response);
        assertTrue(response.empty());
        assertTrue(response.households().isEmpty());
        assertTrue(response.residents().isEmpty());
        assertEquals(0L, response.totals().households());
        assertEquals(0L, response.totals().residents());
    }

    @Test
    @DisplayName("Từ khóa ngắn dưới 2 ký tự -> ném AppValidationException errors.keyword")
    void search_keywordTooShort_throwsValidationException() {
        AppValidationException ex = assertThrows(AppValidationException.class, () ->
                searchService.search("a", "ALL", 20)
        );

        assertTrue(ex.getErrors().containsKey("keyword"));
        assertEquals("Nhập ít nhất 2 ký tự", ex.getErrors().get("keyword"));
    }

    @Test
    @DisplayName("Limit ngoài khoảng 1..50 -> ném AppValidationException errors.limit")
    void search_invalidLimit_throwsValidationException() {
        AppValidationException ex = assertThrows(AppValidationException.class, () ->
                searchService.search("test", "ALL", 100)
        );

        assertTrue(ex.getErrors().containsKey("limit"));
    }

    @Test
    @DisplayName("Loại tìm kiếm không hợp lệ -> ném AppValidationException errors.type")
    void search_invalidType_throwsValidationException() {
        AppValidationException ex = assertThrows(AppValidationException.class, () ->
                searchService.search("test", "UNKNOWN", 20)
        );

        assertTrue(ex.getErrors().containsKey("type"));
    }

    @Test
    @DisplayName("Tìm kiếm chỉ theo HOUSEHOLD -> mảng residents rỗng và count = 0")
    void search_onlyHousehold_returnsOnlyHouseholds() {
        when(householdRepository.quickSearchHouseholds("test", 20)).thenReturn(Collections.emptyList());
        when(householdRepository.countQuickSearchHouseholds("test")).thenReturn(0L);

        SearchResponse response = searchService.search("test", "HOUSEHOLD", 20);

        assertTrue(response.residents().isEmpty());
        assertEquals(0L, response.totals().residents());
        verify(residentRepository, never()).quickSearchResidents(anyString(), anyInt());
    }
}
