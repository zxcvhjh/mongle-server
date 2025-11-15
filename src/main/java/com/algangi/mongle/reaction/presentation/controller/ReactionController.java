package com.algangi.mongle.reaction.presentation.controller;

import com.algangi.mongle.auth.infrastructure.security.authentication.CustomUserDetails;
import com.algangi.mongle.reaction.application.service.ReactionApplicationService;
import com.algangi.mongle.reaction.presentation.dto.ReactionRequest;
import com.algangi.mongle.reaction.presentation.dto.ReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 리액션 API 컨트롤러
 * ApiResponseAdvice에 의해 자동으로 ApiResponse로 래핑됨
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionApplicationService reactionApplicationService;

    @PostMapping("/{targetType}/{targetId}/reaction")
    public ReactionResponse updateReaction(
            @PathVariable String targetType,
            @PathVariable String targetId,
            @RequestBody ReactionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return reactionApplicationService.updateReaction(
                targetType,
                targetId,
                user.userId(),
                request.reactionType()
        );
    }

}