package com.minierp.backend.domain.project.repository;

import com.minierp.backend.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProjectId(Long projectId);

    List<ProjectMember> findByUserId(Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    void deleteByProjectIdAndUserId(Long projectId, Long userId);

    // 멤버 수 일괄 집계 (프로젝트 목록 1+3N 최적화)
    @Query("select pm.project.id, count(pm) from ProjectMember pm where pm.project.id in :projectIds group by pm.project.id")
    List<Object[]> countMembersByProjectIds(@Param("projectIds") List<Long> projectIds);
}
