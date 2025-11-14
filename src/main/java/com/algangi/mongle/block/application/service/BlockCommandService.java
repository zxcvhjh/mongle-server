package com.algangi.mongle.block.application.service;

import com.algangi.mongle.block.exception.BlockErrorCode;
import com.algangi.mongle.global.exception.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algangi.mongle.block.domain.model.Block;
import com.algangi.mongle.block.domain.repository.BlockRepository;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional
public class BlockCommandService {

    private final BlockRepository blockRepository;
    private final MemberFinder memberFinder;

    public void blockUser(String blockerId, String blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new ApplicationException(BlockErrorCode.CANNOT_BLOCK_SELF);
        }

        if (blockRepository.findByBlocker_MemberIdAndBlocked_MemberId(blockerId, blockedId)
            .isPresent()) {
            return;
        }

        Member blocker = memberFinder.getMemberOrThrow(blockerId);
        Member blocked = memberFinder.getMemberOrThrow(blockedId);

        if (isProtectedAccount(blocked)) {
            throw new ApplicationException(BlockErrorCode.CANNOT_BLOCK_ADMIN_OR_BOOTH);
        }

        Block newBlock = Block.of(blocker, blocked);
        blockRepository.save(newBlock);
    }

    public void unblockUser(String blockerId, String blockedId) {
        blockRepository.findByBlocker_MemberIdAndBlocked_MemberId(blockerId, blockedId)
            .ifPresent(blockRepository::delete);
    }

    private boolean isProtectedAccount(Member member) {
        return member.getMemberRole() == com.algangi.mongle.member.domain.model.MemberRole.ADMIN
            || member.getMemberRole() == com.algangi.mongle.member.domain.model.MemberRole.BOOTH;
    }
}
