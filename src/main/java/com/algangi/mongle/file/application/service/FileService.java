package com.algangi.mongle.file.application.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.algangi.mongle.file.application.dto.FileMetadata;
import com.algangi.mongle.file.application.dto.PresignedUrl;
import com.algangi.mongle.file.application.util.FileOptimizationUtils;
import com.algangi.mongle.file.domain.FileHandler;
import com.algangi.mongle.file.domain.FileType;
import com.algangi.mongle.file.exception.FileErrorCode;
import com.algangi.mongle.file.presentation.dto.UploadUrlRequest;
import com.algangi.mongle.file.presentation.dto.UploadUrlResponse;
import com.algangi.mongle.file.presentation.dto.ViewUrlRequest;
import com.algangi.mongle.file.presentation.dto.ViewUrlResponse;
import com.algangi.mongle.global.exception.ApplicationException;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Map<FileType, FileHandler> handlerMap = new EnumMap<>(FileType.class);
    private final List<FileHandler> handlers;
    private final StorageService storageService;
    private final ViewUrlIssueService viewUrlIssueService;
    private final FileOptimizationUtils fileOptimizationUtils;
    @Value("${mongle.aws.s3.presigned-url-expiration-minutes}")
    private long expirationMinutes;

    @PostConstruct
    public void init() {
        handlerMap.putAll(handlers.stream()
            .collect(Collectors.toMap(FileHandler::getFileType, Function.identity())));
    }

    public UploadUrlResponse issueUploadUrls(UploadUrlRequest request) {
        FileHandler handler = getHandler(request.fileType());
        List<FileMetadata> files = handler.createMetadata(request.files());
        handler.validateFiles(files);

        List<PresignedUrl> issuedUrls = files.stream()
            .map(file -> {
                String fileKey = handler.generateFileKey(file.fileName());
                String url = storageService.issueUploadPresignedUrl(fileKey, expirationMinutes);
                Instant expiresAt = Instant.now()
                    .plus(expirationMinutes, ChronoUnit.MINUTES); // <<< 수정
                return new PresignedUrl(fileKey, url, expiresAt);
            }).toList();

        return UploadUrlResponse.of(issuedUrls);
    }

    public ViewUrlResponse issueViewUrls(ViewUrlRequest request) {
        return ViewUrlResponse.of(viewUrlIssueService.issueViewUrls(request.fileKeyList()));
    }

    /**
     * 임시 파일을 영구 파일로 전환합니다.
     *
     * <p><strong>주의:</strong> 이 메서드는 이벤트 리스너(PostCreatedEventListener 등)에서만
     * 호출되어야 합니다. 이벤트 발생 시점에 이미 권한 검증이 완료되어 있습니다.</p>
     *
     * <p>파일 소유권 검증은 게시글/댓글 생성 시점에 이미 수행되었으므로,
     * 이 메서드에서는 별도의 검증을 하지 않습니다.</p>
     *
     * @param fileKeys 영구 파일로 전환할 파일 키 목록
     */
    public void commitFiles(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return;
        }

        fileKeys.parallelStream().forEach(storageService::changeTagToPermanent);
    }

    /**
     * 영구 파일을 삭제합니다.
     *
     * <p><strong>주의:</strong> 이 메서드는 이벤트 리스너(PostUpdatedEventListener,
     * WithdrawalCleanupService 등)에서만 호출되어야 합니다.
     * 이벤트 발생 시점에 이미 권한 검증이 완료되어 있습니다.</p>
     *
     * <p>파일 소유권 검증은 게시글/댓글 수정/삭제 시점에 이미 수행되었으므로,
     * 이 메서드에서는 별도의 검증을 하지 않습니다.</p>
     *
     * @param fileKeys 삭제할 파일 키 목록
     */
    public void deletePermanentFiles(List<String> fileKeys) {
        storageService.deleteBulkFiles(fileKeys);
    }

    private FileHandler getHandler(FileType fileType) {
        FileHandler handler = handlerMap.get(fileType);
        if (handler == null) {
            throw new ApplicationException(FileErrorCode.INVALID_FILE_TYPE);
        }
        return handler;
    }
}
