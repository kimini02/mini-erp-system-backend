package com.minierp.backend.domain.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserListResponseDto {

    private final List<UserResponseDto> content;
    private final PageMeta page;

    public static UserListResponseDto of(Page<UserResponseDto> pageResult) {
        return new UserListResponseDto(
                pageResult.getContent(),
                new PageMeta(
                        pageResult.getNumber(),
                        pageResult.getSize(),
                        pageResult.getTotalElements(),
                        pageResult.getTotalPages(),
                        pageResult.isFirst(),
                        pageResult.isLast()
                )
        );
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PageMeta {
        private final int number;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;
    }
}

