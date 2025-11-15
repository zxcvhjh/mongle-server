package com.algangi.mongle.post.application.service;

import com.algangi.mongle.global.domain.service.CellService;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Location;
import com.algangi.mongle.post.domain.service.LocationRandomizer;
import com.algangi.mongle.staticCloud.domain.model.StaticCloud;
import com.algangi.mongle.staticCloud.repository.StaticCloudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 게시글 위치 처리 서비스
 * - 위치 랜덤화
 * - S2 토큰 생성
 * - Static Cloud 조회
 */
@Service
@RequiredArgsConstructor
public class PostLocationService {

    private final CellService cellService;
    private final LocationRandomizer locationRandomizer;
    private final StaticCloudRepository staticCloudRepository;

    /**
     * 위치 정보 처리 결과
     */
    public record LocationProcessingResult(
        Location finalLocation,
        String s2TokenId,
        Optional<StaticCloud> staticCloud
    ) {}

    /**
     * 위치 정보를 처리하여 최종 위치, S2 토큰, Static Cloud를 반환
     *
     * @param originalLocation 원본 위치
     * @param member 작성자
     * @param isRandomLocationEnabled 위치 랜덤화 옵션
     * @return 처리된 위치 정보
     */
    public LocationProcessingResult processLocation(
        Location originalLocation,
        Member member,
        boolean isRandomLocationEnabled
    ) {
        // 원본 위치의 S2 토큰 생성
        String originalS2TokenId = cellService.generateS2TokenIdFrom(
            originalLocation.getLatitude(),
            originalLocation.getLongitude()
        );

        // Static Cloud 조회
        Optional<StaticCloud> staticCloud = staticCloudRepository.findByS2TokenId(originalS2TokenId);

        // 위치 랜덤화 처리
        Location finalLocation = shouldRandomizeLocation(member, isRandomLocationEnabled, staticCloud)
            ? locationRandomizer.randomize(originalLocation)
            : originalLocation;

        // 최종 위치의 S2 토큰 생성
        String finalS2TokenId = cellService.generateS2TokenIdFrom(
            finalLocation.getLatitude(),
            finalLocation.getLongitude()
        );

        return new LocationProcessingResult(finalLocation, finalS2TokenId, staticCloud);
    }

    /**
     * 위치 랜덤화 여부 판단
     * - 관리자가 아니고
     * - 랜덤 위치 옵션이 활성화되어 있고
     * - Static Cloud가 아닌 경우에만 랜덤화
     */
    private boolean shouldRandomizeLocation(
        Member member,
        boolean isRandomLocationEnabled,
        Optional<StaticCloud> staticCloud
    ) {
        return !member.isAdmin()
            && isRandomLocationEnabled
            && staticCloud.isEmpty();
    }

    /**
     * 위도/경도로부터 S2 토큰 생성
     */
    public String generateS2TokenId(double latitude, double longitude) {
        return cellService.generateS2TokenIdFrom(latitude, longitude);
    }
}
