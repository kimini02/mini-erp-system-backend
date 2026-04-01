package com.minierp.backend.domain.project.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectStatus {

    READY("준비"),
    PROGRESS("진행 중"),
    DONE("완료"),
    HOLD("보류");

    private final String description;
}
