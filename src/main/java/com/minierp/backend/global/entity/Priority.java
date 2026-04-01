package com.minierp.backend.global.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Priority {

    HIGH("높음"),
    MEDIUM("중간"),
    LOW("낮음");

    private final String description;
}
