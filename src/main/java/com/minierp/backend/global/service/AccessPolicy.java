package com.minierp.backend.global.service;

import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class AccessPolicy {

    public void requireAdmin(UserRole role) {
        if (role == null || !role.isTopManager()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void requireAdminOrLeader(UserRole role) {
        if (role == null || role.isGeneralUser()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    public boolean canViewAllUsers(UserRole role) {
        return role != null && role.isTopManager();
    }

    public boolean canViewAllRequests(UserRole role) {
        return role != null && !role.isGeneralUser();
    }

    public boolean canAccessSelfOrAdmin(Long ownerUserId, Long accessorUserId, UserRole accessorRole) {
        if (ownerUserId == null || accessorUserId == null || accessorRole == null) {
            return false;
        }
        return accessorRole.isTopManager() || ownerUserId.equals(accessorUserId);
    }

    public void validateApprovalHierarchy(UserRole requesterRole, Long requesterId, UserRole approverRole, Long approverId) {
        if (requesterRole == null || approverRole == null || requesterId == null || approverId == null) {
            throw new BusinessException(ErrorCode.APPROVAL_ADMIN_ONLY, "권한 정책상 처리할 수 없는 결재 대상입니다.");
        }

        // USER 신청 건: TEAM_LEADER만 처리 가능
        if (requesterRole.isGeneralUser()) {
            if (approverRole.isTeamLeader()) {
                return;
            }
            throw new BusinessException(ErrorCode.APPROVAL_ADMIN_ONLY,
                    "일반 사용자 신청 건은 팀장만 결재할 수 있습니다.");
        }

        // TEAM_LEADER 신청 건: ADMIN만 처리 가능
        if (requesterRole.isTeamLeader()) {
            if (approverRole.isTopManager()) {
                return;
            }
            throw new BusinessException(ErrorCode.APPROVAL_ADMIN_ONLY,
                    "팀장 신청 건은 관리소장만 결재할 수 있습니다.");
        }

        // ADMIN 신청 건: 상위 권한이 없어 처리 불가
        if (requesterRole.isTopManager()) {
            throw new BusinessException(ErrorCode.APPROVAL_ADMIN_ONLY,
                    "관리소장 신청 건은 처리할 수 있는 상위 권한이 없습니다.");
        }

        throw new BusinessException(ErrorCode.APPROVAL_ADMIN_ONLY,
                "권한 정책상 처리할 수 없는 결재 대상입니다.");
    }
}

