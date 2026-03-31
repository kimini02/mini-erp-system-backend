package com.minierp.backend.domain.approval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RejectRequestDto {
    private String rejectReason;
}
