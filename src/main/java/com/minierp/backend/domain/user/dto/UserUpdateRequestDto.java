package com.minierp.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserUpdateRequestDto {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "부서는 필수입니다.")
    @Size(min = 2, max = 2, message = "부서 코드는 2자리여야 합니다.")
    @jakarta.validation.constraints.Pattern(regexp = "^(01|02|03)$", message = "부서 코드는 01, 02, 03 중 하나여야 합니다.")
    private String departmentCode;

    @NotBlank(message = "직책은 필수입니다.")
    @Size(max = 30, message = "직책은 30자 이하여야 합니다.")
    private String position;
}
