package com.minierp.backend.domain.project.dto;

import com.minierp.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LeaderSummaryDto {

    private Long userId;
    private String userName;
    private long assignedProjectCount;

    public static LeaderSummaryDto of(User user, long assignedProjectCount) {
        return new LeaderSummaryDto(
                user.getId(),
                user.getUserName(),
                assignedProjectCount
        );
    }
}
