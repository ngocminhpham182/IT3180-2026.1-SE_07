package com.example.bluemoon.service;

import com.example.bluemoon.dto.ReceivablePageResponse;
import com.example.bluemoon.exception.ResourceNotFoundException;
import com.example.bluemoon.model.BillingPeriod;
import com.example.bluemoon.model.FeeCategory;
import com.example.bluemoon.model.Household;
import com.example.bluemoon.model.Receivable;
import com.example.bluemoon.repository.BillingPeriodRepository;
import com.example.bluemoon.repository.ReceivableRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingPeriodServiceTest {

    @Mock
    private BillingPeriodRepository billingPeriodRepository;

    @Mock
    private ReceivableRepository receivableRepository;

    @InjectMocks
    private BillingPeriodService billingPeriodService;

    @Test
    @DisplayName("Lấy danh sách khoản thu thành công cho kỳ thu")
    void getReceivables_success() {
        Long periodId = 1L;
        when(billingPeriodRepository.existsById(periodId)).thenReturn(true);

        Household household = Household.builder()
                .id(10L)
                .householdCode("HGD-001")
                .apartmentNo("P101")
                .build();

        FeeCategory feeCategory = FeeCategory.builder()
                .id(20L)
                .code("FEE_QL")
                .name("Phí quản lý")
                .build();

        BillingPeriod billingPeriod = new BillingPeriod();
        billingPeriod.setId(periodId);

        Receivable receivable = Receivable.builder()
                .id(100L)
                .billingPeriod(billingPeriod)
                .household(household)
                .feeCategory(feeCategory)
                .quantity(new BigDecimal("75.50"))
                .unitPriceApplied(new BigDecimal("10000.00"))
                .amountDue(new BigDecimal("755000.00"))
                .dueDate(LocalDate.of(2026, 11, 25))
                .build();

        when(receivableRepository.searchReceivables(
                eq(periodId), isNull(), isNull(), eq(""), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(receivable), PageRequest.of(0, 20), 1));

        when(receivableRepository.sumAmountDue(
                eq(periodId), isNull(), isNull(), eq("")
        )).thenReturn(new BigDecimal("755000.00"));

        ReceivablePageResponse response = billingPeriodService.getReceivables(
                periodId, null, null, null, 1, 20
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(100L, response.content().get(0).id());
        assertEquals("HGD-001", response.content().get(0).householdCode());
        assertEquals("P101", response.content().get(0).apartmentNo());
        assertEquals("Phí quản lý", response.content().get(0).feeName());
        assertEquals(new BigDecimal("755000.00"), response.content().get(0).amountDue());
        assertEquals(BigDecimal.ZERO, response.content().get(0).paidAmount());
        assertEquals(new BigDecimal("755000.00"), response.content().get(0).outstanding());
        assertEquals(new BigDecimal("755000.00"), response.summary().totalDue());
        assertEquals(1, response.page());
        assertEquals(20, response.size());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    @Test
    @DisplayName("Ném lỗi ResourceNotFoundException khi kỳ thu không tồn tại")
    void getReceivables_periodNotFound() {
        when(billingPeriodRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                billingPeriodService.getReceivables(999L, null, null, null, 1, 20)
        );
    }

    @Test
    @DisplayName("Ném lỗi IllegalArgumentException khi page < 1 hoặc size không hợp lệ")
    void getReceivables_invalidPagination() {
        when(billingPeriodRepository.existsById(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                billingPeriodService.getReceivables(1L, null, null, null, 0, 20)
        );

        assertThrows(IllegalArgumentException.class, () ->
                billingPeriodService.getReceivables(1L, null, null, null, 1, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                billingPeriodService.getReceivables(1L, null, null, null, 1, 101)
        );
    }

    @Test
    @DisplayName("Tìm kiếm với bộ lọc từ khóa, hộ và khoản thu")
    void getReceivables_withFilters() {
        Long periodId = 1L;
        when(billingPeriodRepository.existsById(periodId)).thenReturn(true);

        when(receivableRepository.searchReceivables(
                eq(periodId), eq(10L), eq(20L), eq("P101"), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        when(receivableRepository.sumAmountDue(
                eq(periodId), eq(10L), eq(20L), eq("P101")
        )).thenReturn(BigDecimal.ZERO);

        ReceivablePageResponse response = billingPeriodService.getReceivables(
                periodId, "  P101  ", 10L, 20L, 1, 20
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(BigDecimal.ZERO, response.summary().totalDue());
    }
}
