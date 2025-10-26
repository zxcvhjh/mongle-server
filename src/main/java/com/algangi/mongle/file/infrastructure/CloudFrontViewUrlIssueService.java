package com.algangi.mongle.file.infrastructure;

import java.util.List;

import org.springframework.stereotype.Service;

import com.algangi.mongle.file.application.dto.PresignedUrl;
import com.algangi.mongle.file.application.service.StorageService;
import com.algangi.mongle.file.application.service.ViewUrlIssueService;
import com.algangi.mongle.file.application.util.FileOptimizationUtils;
import com.algangi.mongle.global.config.CloudFrontProperties;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.global.exception.AwsErrorCode;
import com.algangi.mongle.global.util.PemUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontException;

@Service
public class CloudFrontViewUrlIssueService implements ViewUrlIssueService {

    public static final String HTTPS = "https://";
    public static final String DIR_DELIMITER = "/";
    private final CloudFrontProperties cloudFrontProperties;
    private final CloudFrontUtilities cloudFrontUtilities;
    private final FileOptimizationUtils fileOptimizationUtils;
    private final StorageService storageService;
    private PrivateKey privateKey;

    public CloudFrontViewUrlIssueService(CloudFrontProperties cloudFrontProperties,
        FileOptimizationUtils fileOptimizationUtils, StorageService storageService) {
        this.cloudFrontProperties = cloudFrontProperties;
        this.cloudFrontUtilities = CloudFrontUtilities.create();
        this.fileOptimizationUtils = fileOptimizationUtils;
        this.storageService = storageService;
    }

    @PostConstruct
    public void init() {
        try {
            this.privateKey = PemUtils.loadPrivateKey(cloudFrontProperties.privateKeyFilePath());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Private Key 로딩 중 오류 발생", e);
        }
    }

    @Override
    public List<PresignedUrl> issueViewUrls(List<String> fileKeyList) {
        return fileKeyList.parallelStream()
            .map(this::issueViewUrl)
            .toList();
    }


    @Override
    public PresignedUrl issueViewUrl(String fileKey) {
        // TODO: originUrl, webpUrl 둘 다 반환 후 안드로이드단에서 폴백하도록 변경
        Instant expirationTime = Instant.now()
            .plus(cloudFrontProperties.expirationMinutes(), ChronoUnit.MINUTES);

        if (fileOptimizationUtils.isOptimizableImage(fileKey)) {
            String optimizedFileKey = fileOptimizationUtils.getOptimizedFileKey(fileKey);
            // TODO: 이후 안드로이드 단에서 검증하도록 변경
            if (storageService.checkFileExists(optimizedFileKey)) {
                //optimizedFileKey
                String optimizedFileUrl = generateSignedViewUrl(optimizedFileKey, expirationTime);
                return new PresignedUrl(fileKey, optimizedFileUrl, expirationTime);
            }
        }
        //originalFileKey
        String originalFileUrl = generateSignedViewUrl(fileKey, expirationTime);
        return new PresignedUrl(fileKey, originalFileUrl, expirationTime);
    }

    private String generateSignedViewUrl(String fileKey, Instant expirationTime) {
        String resourceUrl = HTTPS + cloudFrontProperties.domain() + DIR_DELIMITER + fileKey;

        try {
            CannedSignerRequest signerRequest = CannedSignerRequest.builder()
                .resourceUrl(resourceUrl)
                .privateKey(privateKey)
                .keyPairId(cloudFrontProperties.keyPairId())
                .expirationDate(expirationTime)
                .build();

            return cloudFrontUtilities.getSignedUrlWithCannedPolicy(signerRequest)
                .url();
        } catch (CloudFrontException e) {
            throw new ApplicationException(AwsErrorCode.CLOUDFRONT_PRESIGNED_URL_ISSUE_FAILED, e)
                .addErrorInfo("awsErrorMessage", e.getMessage());
        }
    }
}