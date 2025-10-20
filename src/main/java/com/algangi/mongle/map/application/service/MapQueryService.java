package com.algangi.mongle.map.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.algangi.mongle.postViewLog.application.service.PostViewLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algangi.mongle.block.application.service.BlockQueryService;
import com.algangi.mongle.dynamicCloud.domain.model.DynamicCloud;
import com.algangi.mongle.dynamicCloud.domain.repository.DynamicCloudRepository;
import com.algangi.mongle.global.infrastructure.S2CellService;
import com.algangi.mongle.global.util.S2PolygonConverter;
import com.algangi.mongle.map.presentation.dto.MapObjectsRequest;
import com.algangi.mongle.map.presentation.dto.MapObjectsResponse;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.PostStatus;
import com.algangi.mongle.post.domain.repository.PostQueryRepository;
import com.algangi.mongle.staticCloud.domain.model.StaticCloud;
import com.algangi.mongle.staticCloud.repository.StaticCloudRepository;
import com.algangi.mongle.file.application.service.ViewUrlIssueService;

import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

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
        Set<String> viewedPostIds = postIdsToCheck.isEmpty()
                ? java.util.Collections.emptySet()
                : postViewLogService.findViewedPostIdsInList(memberId, postIdsToCheck);

        List<StaticCloud> staticClouds = staticCloudRepository.findCloudsInCells(s2cellTokens);
        List<DynamicCloud> dynamicClouds = dynamicCloudRepository.findActiveCloudsInCells(
            s2cellTokens);

        Map<String, Member> authors = getAuthors(grains);
        Map<Long, Long> staticCloudPostCounts = getStaticCloudPostCounts(staticClouds);
        Map<Long, Long> dynamicCloudPostCounts = getDynamicCloudPostCounts(dynamicClouds);

        Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);

        List<MapObjectsResponse.Grain> grainDtos = grains.stream()
            .map(post -> {
                Member author = authors.get(post.getAuthorId());

                String profileImageUrl = null;
                if (post.getStatus() == PostStatus.ACTIVE && author != null && author.getProfileImage() != null) {
                    profileImageUrl = viewUrlIssueService.issueViewUrl(author.getProfileImage()).url();
                }

                MapObjectsResponse.Grain.Author authorDto = (author != null)
                    ? new MapObjectsResponse.Grain.Author(author.getMemberId(),
                    author.getNickname(), profileImageUrl)
                    : new MapObjectsResponse.Grain.Author(null, "익명의 몽글러", null);

                boolean isViewed = viewedPostIds.contains(post.getId());
                boolean isRecent = post.getCreatedDate().isAfter(thirtyMinutesAgo);
                boolean hasGlareEffect = !isViewed && isRecent;

                return new MapObjectsResponse.Grain(
                    post.getId(),
                    post.getLocation().getLatitude(),
                    post.getLocation().getLongitude(),
                    authorDto,
                    isViewed,
                    hasGlareEffect
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
            .distinct()
            .toList();

        return memberFinder.findMembersByIds(authorIds).stream()
            .collect(Collectors.toMap(Member::getMemberId, Function.identity()));
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