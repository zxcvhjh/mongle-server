package com.algangi.mongle.global.util;

import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.global.exception.ErrorCode;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.model.MemberRole;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 권한 검증을 위한 유틸리티 클래스.
 * 모든 서비스에서 일관된 권한 체크 로직을 사용하기 위한 중앙화된 유틸리티입니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthorizationUtil {

    /**
     * 리소스 소유자이거나 관리자인지 검증합니다.
     * 조건을 만족하지 않으면 예외를 발생시킵니다.
     *
     * @param resourceOwnerId 리소스 소유자 ID (null 가능 - 익명 리소스의 경우)
     * @param currentUserId 현재 요청한 사용자 ID
     * @param currentUserRole 현재 요청한 사용자의 역할
     * @param errorCode 권한이 없을 때 발생시킬 에러 코드
     * @throws ApplicationException 소유자도 아니고 관리자도 아닌 경우
     */
    public static void validateOwnershipOrAdmin(
        String resourceOwnerId,
        String currentUserId,
        MemberRole currentUserRole,
        ErrorCode errorCode
    ) {
        if (!isOwnerOrAdmin(resourceOwnerId, currentUserId, currentUserRole)) {
            throw new ApplicationException(errorCode);
        }
    }

    /**
     * 리소스 소유자이거나 관리자인지 검증합니다.
     * Member 객체를 직접 받아서 검증합니다.
     *
     * @param resourceOwnerId 리소스 소유자 ID (null 가능 - 익명 리소스의 경우)
     * @param currentMember 현재 요청한 사용자
     * @param errorCode 권한이 없을 때 발생시킬 에러 코드
     * @throws ApplicationException 소유자도 아니고 관리자도 아닌 경우
     */
    public static void validateOwnershipOrAdmin(
        String resourceOwnerId,
        Member currentMember,
        ErrorCode errorCode
    ) {
        validateOwnershipOrAdmin(
            resourceOwnerId,
            currentMember.getMemberId(),
            currentMember.getMemberRole(),
            errorCode
        );
    }

    /**
     * 관리자 권한이 있는지 검증합니다.
     *
     * @param member 검증할 회원
     * @param errorCode 관리자가 아닐 때 발생시킬 에러 코드
     * @throws ApplicationException 관리자가 아닌 경우
     */
    public static void validateAdmin(Member member, ErrorCode errorCode) {
        if (!member.isAdmin()) {
            throw new ApplicationException(errorCode);
        }
    }

    /**
     * 관리자 권한이 있는지 검증합니다.
     *
     * @param memberRole 검증할 회원 역할
     * @param errorCode 관리자가 아닐 때 발생시킬 에러 코드
     * @throws ApplicationException 관리자가 아닌 경우
     */
    public static void validateAdmin(MemberRole memberRole, ErrorCode errorCode) {
        if (memberRole != MemberRole.ADMIN) {
            throw new ApplicationException(errorCode);
        }
    }

    /**
     * 리소스 소유자인지 검증합니다 (관리자 예외 없음).
     *
     * @param resourceOwnerId 리소스 소유자 ID
     * @param currentUserId 현재 요청한 사용자 ID
     * @param errorCode 소유자가 아닐 때 발생시킬 에러 코드
     * @throws ApplicationException 소유자가 아닌 경우
     */
    public static void validateOwnership(
        String resourceOwnerId,
        String currentUserId,
        ErrorCode errorCode
    ) {
        if (!isOwner(resourceOwnerId, currentUserId)) {
            throw new ApplicationException(errorCode);
        }
    }

    /**
     * 리소스 소유자이거나 관리자인지 확인합니다 (boolean 반환).
     *
     * @param resourceOwnerId 리소스 소유자 ID (null 가능 - 익명 리소스의 경우)
     * @param currentUserId 현재 요청한 사용자 ID
     * @param currentUserRole 현재 요청한 사용자의 역할
     * @return 소유자이거나 관리자인 경우 true
     */
    public static boolean isOwnerOrAdmin(
        String resourceOwnerId,
        String currentUserId,
        MemberRole currentUserRole
    ) {
        return isOwner(resourceOwnerId, currentUserId) || currentUserRole == MemberRole.ADMIN;
    }

    /**
     * 리소스 소유자인지 확인합니다 (boolean 반환).
     * resourceOwnerId가 null인 경우 (익명 리소스) false를 반환합니다.
     *
     * @param resourceOwnerId 리소스 소유자 ID
     * @param currentUserId 현재 요청한 사용자 ID
     * @return 소유자인 경우 true
     */
    public static boolean isOwner(String resourceOwnerId, String currentUserId) {
        return resourceOwnerId != null && Objects.equals(resourceOwnerId, currentUserId);
    }
}
