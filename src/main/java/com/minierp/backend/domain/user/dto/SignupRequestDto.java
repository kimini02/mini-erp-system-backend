package com.minierp.backend.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequestDto {
    private String userName;
    private String userEmail;
    private String userPw;
    private String positionName;
}