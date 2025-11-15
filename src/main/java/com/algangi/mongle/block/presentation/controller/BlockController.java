package com.algangi.mongle.block.presentation.controller;

import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import com.algangi.mongle.block.application.service.BlockCommandService;
import com.algangi.mongle.block.application.service.BlockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 차단 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockCommandService blockCommandService;
    private final BlockQueryService blockQueryService;


    @GetMapping("/me")
    public List<String> getMyBlockedUsers(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return blockQueryService.getBlockedUserIds(user.userId());
    }

    @PostMapping("/{blockedUserId}")
    public void blockUser(
            @PathVariable String blockedUserId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        blockCommandService.blockUser(user.userId(), blockedUserId);
    }

    @DeleteMapping("/{blockedUserId}")
    public void unblockUser(
            @PathVariable String blockedUserId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        blockCommandService.unblockUser(user.userId(), blockedUserId);
    }
}