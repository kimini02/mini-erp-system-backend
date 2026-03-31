package com.minierp.backend.domain.project.dto;

import com.minierp.backend.domain.project.entity.ProjectMember;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectMemberResponseDto {

    private Long id;
    private Long projectId;
    private Long userId;

    public static ProjectMemberResponseDto from(ProjectMember projectMember) {
        return new ProjectMemberResponseDto(
                projectMember.getId(),
                projectMember.getProject().getId(),
                projectMember.getUser().getId()
        );
    }
}
