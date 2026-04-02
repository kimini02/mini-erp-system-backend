package com.minierp.backend.domain.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberRequestDto {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    public static MemberRequestDto of(Long userId) {
        MemberRequestDto dto = new MemberRequestDto();
        dto.userId = userId;
        return dto;
    }
}
