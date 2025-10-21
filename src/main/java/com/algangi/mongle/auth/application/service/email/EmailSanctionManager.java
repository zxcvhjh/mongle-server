package com.algangi.mongle.auth.application.service.email;

import com.algangi.mongle.auth.domain.model.EmailSanction;

/**
 * 이메일 발송 제재 정책(하드 바운스, 벤)을 관리하는 서비스 인터페이스입니다.
 */
public interface EmailSanctionManager {

    int MAX_HARD_BOUNCE_COUNT = 5;

    /**
     * 특정 이메일 주소가 현재 발송이 차단된 상태인지 확인합니다.
     *
     * @param email 확인할 이메일 주소
     * @return 차단된 상태면 true
     */
    boolean isBanned(String email);

    /**
     * 특정 이메일의 하드 바운스 횟수를 증가시키고, 최대 횟수 초과 시 벤 처리합니다.
     *
     * @param email 하드 바운스가 발생한 이메일 주소
     */
    void recordHardBounceAndSanction(String email);

    /**
     * 이메일 주소에 대한 제재 정보를 조회하거나, 없으면 생성합니다.
     *
     * @param email 이메일 주소
     * @return EmailSanction 객체
     */
    EmailSanction getOrCreateSanction(String email);
}