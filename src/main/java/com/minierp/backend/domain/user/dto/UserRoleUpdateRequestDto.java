package com.minierp.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UserRoleUpdateRequestDto {

    @NotBlank(message = "권한은 필수입니다.")
    private String role;
}

