package com.algangi.mongle.map.application.service;

import com.algangi.mongle.block.application.service.BlockQueryService;
import com.algangi.mongle.dynamicCloud.domain.model.DynamicCloud;
import com.algangi.mongle.dynamicCloud.domain.repository.DynamicCloudRepository;
import com.algangi.mongle.file.application.service.ViewUrlIssueService;
import com.algangi.mongle.global.infrastructure.S2CellService;
import com.algangi.mongle.global.util.S2PolygonConverter;
import com.algangi.mongle.map.presentation.dto.MapObjectsRequest;
import com.algangi.mongle.map.presentation.dto.MapObjectsResponse;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.PostStatus;
import com.algangi.mongle.post.domain.repository.PostQueryRepository;
import com.algangi.mongle.postViewLog.application.service.PostViewLogService;
import com.algangi.mongle.staticCloud.domain.model.StaticCloud;
import com.algangi.mongle.staticCloud.repository.StaticCloudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapQueryService {

    private final S2CellService s2CellService;
    private final PostQueryRepository postQueryRepository;
    private final StaticCloudRepository staticCloudRepository;
    private final DynamicCloudRepository dynamicCloudRepository;
    private final MemberFinder memberFinder;
    private final S2PolygonConverter s2PolygonConverter;
    private final BlockQueryService blockQueryService;
    private final PostViewLogService postViewLogService;
    private final ViewUrlIssueService viewUrlIssueService;

    private static final String ADMIN_DISPLAY_NAME = "몽글 관리자";

    public MapObjectsResponse getMapObjects(MapObjectsRequest request, String memberId) {
        if (StringUtils.hasText(memberId)) {
            try {
                postViewLogService.refreshViewLogTtl(memberId);
            } catch (Exception e) {
                log.warn("Failed to refresh view log TTL in Redis for map query.", e);
            }
        }

        List<String> s2cellTokens = s2CellService.getCellsForRect(
            request.swLat(), request.swLng(), request.neLat(), request.neLng()
        );

        if (s2cellTokens.isEmpty()) {
            return MapObjectsResponse.empty();
        }

        List<String> blockedAuthorIds = blockQueryService.getBlockedUserIds(memberId);

        List<Post> grains = postQueryRepository.findGrainsInCells(s2cellTokens, blockedAuthorIds);

        List<String> postIdsToCheck = grains.stream().map(Post::getId).toList();
        Set<String> viewedPostIds = (!StringUtils.hasText(memberId) || postIdsToCheck.isEmpty())
            ? java.util.Collections.emptySet()
            : postViewLogService.findViewedPostIdsInList(memberId, postIdsToCheck);

        List<StaticCloud> staticClouds = staticCloudRepository.findCloudsInCells(s2cellTokens);
        List<DynamicCloud> dynamicClouds = dynamicCloudRepository.findActiveCloudsInCells(
            s2cellTokens);

        Map<String, Member> authors = getAuthors(grains);
        Map<Long, Long> staticCloudPostCounts = getStaticCloudPostCounts(staticClouds);
        Map<Long, Long> dynamicCloudPostCounts = getDynamicCloudPostCounts(dynamicClouds);

        Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        boolean isLoggedIn = StringUtils.hasText(memberId);

        List<MapObjectsResponse.Grain> grainDtos = grains.stream()
            .map(post -> {
                Member author = authors.get(post.getAuthorId());
                MapObjectsResponse.Grain.Author authorDto = createAuthorDto(post, author);

                boolean isViewed = viewedPostIds.contains(post.getId());
                boolean isPostCreatedRecently = !post.getCreatedDate().isBefore(thirtyMinutesAgo);
                boolean isRecent = isLoggedIn && !isViewed && isPostCreatedRecently;
                boolean isGrain =
                    post.getStaticCloudId() == null && post.getDynamicCloudId() == null;

                return new MapObjectsResponse.Grain(
                    post.getId(),
                    post.getLocation().getLatitude(),
                    post.getLocation().getLongitude(),
                    authorDto,
                    isViewed,
                    isRecent,
                    isGrain ? post.getInfoText() : null
                );
            })
            .toList();

        List<MapObjectsResponse.StaticCloudInfo> staticCloudDtos = staticClouds.stream()
            .map(cloud -> new MapObjectsResponse.StaticCloudInfo(
                cloud.getId().toString(),
                cloud.getName(),
                cloud.getLatitude(),
                cloud.getLongitude(),
                staticCloudPostCounts.getOrDefault(cloud.getId(), 0L),
                s2PolygonConverter.convert(cloud.getS2TokenIds())
            ))
            .toList();

        List<MapObjectsResponse.DynamicCloudInfo> dynamicCloudDtos = dynamicClouds.stream()
            .map(cloud -> new MapObjectsResponse.DynamicCloudInfo(
                cloud.getId().toString(),
                dynamicCloudPostCounts.getOrDefault(cloud.getId(), 0L),
                s2PolygonConverter.convert(cloud.getS2TokenIds())
            ))
            .toList();

        return new MapObjectsResponse(grainDtos, staticCloudDtos, dynamicCloudDtos);
    }

    private Map<String, Member> getAuthors(List<Post> grains) {
        if (grains.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> authorIds = grains.stream()
            .map(Post::getAuthorId)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return memberFinder.findMembersByIds(authorIds).stream()
            .collect(Collectors.toMap(Member::getMemberId, Function.identity()));
    }

    private MapObjectsResponse.Grain.Author createAuthorDto(Post post, Member author) {
        String authorId = post.getAuthorId();
        String customNickname = post.getCustomNickname();
        boolean isAnonymous = post.isAnonymous();
        String profileImageUrl = null;

        if (author != null && post.getStatus() == PostStatus.ACTIVE
            && author.getProfileImage() != null) {
            try {
                profileImageUrl = viewUrlIssueService.issueViewUrl(author.getProfileImage()).url();
            } catch (Exception e) {
                log.warn("Failed to issue view URL for profile image key in map query: {}",
                    author.getProfileImage(), e);
            }
        }

        if (author == null) {
            return new MapObjectsResponse.Grain.Author(null, "익명의 몽글러", null);
        } else if (StringUtils.hasText(customNickname)) {
            return new MapObjectsResponse.Grain.Author(authorId, customNickname,
                profileImageUrl);
        } else if (isAnonymous) {
            return new MapObjectsResponse.Grain.Author(authorId, "익명의 몽글러", null);
        } else {
            return new MapObjectsResponse.Grain.Author(authorId, author.getNickname(),
                profileImageUrl);
        }
    }


    private Map<Long, Long> getStaticCloudPostCounts(List<StaticCloud> clouds) {
        if (clouds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> cloudIds = clouds.stream().map(StaticCloud::getId).toList();
        return postQueryRepository.countPostsByStaticCloudIds(cloudIds);
    }

    private Map<Long, Long> getDynamicCloudPostCounts(List<DynamicCloud> clouds) {
        if (clouds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> cloudIds = clouds.stream().map(DynamicCloud::getId).toList();
        return postQueryRepository.countPostsByDynamicCloudIds(cloudIds);
    }
}

