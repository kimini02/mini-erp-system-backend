package com.minierp.backend.domain.task.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {

    TODO("할 일"),
    DOING("진행 중"),
    DONE("완료");

    private final String description;
}
