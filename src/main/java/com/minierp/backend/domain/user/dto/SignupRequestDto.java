package com.minierp.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignupRequestDto {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
    private String password;

    @NotBlank(message = "부서는 필수입니다.")
    @Size(min = 2, max = 2, message = "부서 코드는 2자리여야 합니다.")
    @jakarta.validation.constraints.Pattern(regexp = "^(01|02|03)$", message = "부서 코드는 01, 02, 03 중 하나여야 합니다.")
    private String departmentCode;

    @NotBlank(message = "직책은 필수입니다.")
    private String position;

    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다.")
    private String id;
}
