package com.example.bluemoon.dto;

import java.math.BigDecimal;

public interface FeeGenerationAggregate {
    Long getFeeCategoryId();
    String getFeeCode();
    String getFeeName();
    Long getReceivableCount();
    BigDecimal getTotalAmount();
}
