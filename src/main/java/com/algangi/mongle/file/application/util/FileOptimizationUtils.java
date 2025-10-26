package com.algangi.mongle.file.application.util;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class FileOptimizationUtils {

    private static final Set<String> OPTIMIZABLE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg");

    public boolean isOptimizableImage(String fileKey) {
        if (fileKey == null) {
            return false;
        }
        int lastDotIndex = fileKey.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return false;
        }
        String extension = fileKey.substring(lastDotIndex).toLowerCase();
        return OPTIMIZABLE_EXTENSIONS.contains(extension);
    }

    public String getOptimizedFileKey(String originalFileKey) {
        int lastDotIndex = originalFileKey.lastIndexOf('.');
        if (lastDotIndex == -1 || !isOptimizableImage(originalFileKey)) {
            // 변환 대상이 아니면 원본 키를 그대로 반환하거나 예외 처리 (상황에 맞게)
            return originalFileKey;
        }
        return originalFileKey.substring(0, lastDotIndex) + ".webp";
    }
}
