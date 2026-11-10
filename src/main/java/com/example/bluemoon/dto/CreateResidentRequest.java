package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.Gender;
import com.example.bluemoon.model.enums.ResidentEntryType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateResidentRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(min = 2, max = 150, message = "Họ tên phải từ 2 đến 150 ký tự")
        String fullName,

        @NotNull(message = "Ngày sinh không được để trống")
        @PastOrPresent(message = "Ngày sinh không được ở tương lai")
        LocalDate dateOfBirth,

        Gender gender,

        @Pattern(
                regexp = "^(\\d{9}|\\d{12})$",
                message = "Số CCCD/CMND phải gồm 9 hoặc 12 chữ số"
        )
        String identityNumber,

        @Pattern(
                regexp = "^(0\\d{9}|\\+84\\d{9})$",
                message = "Số điện thoại không hợp lệ"
        )
        String phone,

        @NotBlank(message = "Quan hệ với chủ hộ không được để trống")
        @Size(min = 1, max = 50,
                message = "Quan hệ với chủ hộ phải từ 1 đến 50 ký tự")
        String relationshipToHead,

        ResidentEntryType entryType,

        LocalDate startDate,

        LocalDate endDate,

        @PastOrPresent(message = "Ngày vào hộ không được ở tương lai")
        LocalDate entryDate

) {

    @AssertTrue(message = "Ngày sinh phải từ 1900-01-01 đến hôm nay")
    public boolean isDateOfBirthValid() {
        return dateOfBirth == null
                || !dateOfBirth.isBefore(LocalDate.of(1900, 1, 1));
    }

    @AssertTrue(message = "startDate là bắt buộc khi entryType = TEMPORARY_RESIDENCE")
    public boolean isStartDateValid() {
        return entryType != ResidentEntryType.TEMPORARY_RESIDENCE
                || startDate != null;
    }

    @AssertTrue(message = "endDate không được sớm hơn startDate")
    public boolean isEndDateValid() {
        return startDate == null || endDate == null
                || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "startDate và endDate chỉ được dùng khi entryType = TEMPORARY_RESIDENCE")
    public boolean areTemporaryDatesValid() {
        return entryType == ResidentEntryType.TEMPORARY_RESIDENCE
                || (startDate == null && endDate == null);
    }
}