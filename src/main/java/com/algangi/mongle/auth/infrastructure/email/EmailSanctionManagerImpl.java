package com.algangi.mongle.auth.infrastructure.email;

import com.algangi.mongle.auth.application.service.email.EmailSanctionManager;
import com.algangi.mongle.auth.domain.model.EmailSanction;
import com.algangi.mongle.auth.domain.repository.EmailSanctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSanctionManagerImpl implements EmailSanctionManager {

    private final EmailSanctionRepository emailSanctionRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isBanned(String email) {
        return emailSanctionRepository.findById(email)
            .map(EmailSanction::isBanned)
            .orElse(false);
    }

    @Override
    @Transactional
    public void recordHardBounceAndSanction(String email) {
        EmailSanction sanction = getOrCreateSanction(email);

        if (sanction.isBanned()) {
            log.warn("이미 벤 처리된 이메일({})에 하드 바운스 기록 시도가 있었습니다.", email);
            return;
        }

        sanction.incrementHardBounceCount();

        if (sanction.getHardBounceCount() >= MAX_HARD_BOUNCE_COUNT) {
            sanction.markAsBanned();
            log.warn("이메일 발송 실패 횟수(5회) 초과로 이메일({})을 벤 처리합니다.", email);
        }

        emailSanctionRepository.save(sanction);
    }

    @Override
    @Transactional
    public EmailSanction getOrCreateSanction(String email) {
        return emailSanctionRepository.findById(email)
            .orElseGet(() -> emailSanctionRepository.save(EmailSanction.create(email)));
    }
}