package com.minierp.backend.domain.project.dto;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.entity.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectPermissionDto {

    private Long projectId;
    private String title;
    private ProjectStatus status;
    private boolean assigned;

    public static ProjectPermissionDto of(Project project, boolean assigned) {
        return new ProjectPermissionDto(
                project.getId(),
                project.getTitle(),
                project.getStatus(),
                assigned
        );
    }
}
