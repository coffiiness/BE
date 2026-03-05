package com.coffiness.calfit.core.api.dto.v1.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CareerApplicationSubmitRequest(
    @NotBlank(message = "이름은 필수입니다.") @Size(max = 50, message = "이름은 50자 이하여야 합니다.") String name,
    @NotBlank(message = "성별은 필수입니다.") @Size(max = 10, message = "성별은 10자 이하여야 합니다.") String gender,
    @NotNull(message = "생년월일은 필수입니다.") LocalDate birthDate,
    @NotBlank(message = "전화번호는 필수입니다.") @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phone,
    @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
        String email,
    @NotBlank(message = "간단 자기소개는 필수입니다.") @Size(max = 500, message = "간단 자기소개는 500자 이하여야 합니다.")
        String shortBio,
    @Size(max = 255, message = "포트폴리오 URL은 255자 이하여야 합니다.") String portfolioUrl) {}
