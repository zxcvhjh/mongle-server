package com.algangi.mongle.stats.application.service;

import com.algangi.mongle.global.util.StatsKeyUtils;
import com.algangi.mongle.reaction.domain.model.TargetType;
import com.algangi.mongle.stats.domain.StatsKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsSyncService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    private static final int SCAN_BATCH_SIZE = 200;
    private static final int PROCESSING_BATCH_SIZE = 300;
    private static final int MAX_KEYS_PER_SYNC = 10000;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncPostCommentCountsToDb() {
        log.info("[Sync] 게시물 댓글 수 동기화 시작");
        syncRedisCountsToDb(
                StatsKeyUtils.trackingKey(StatsKeyPrefix.COMMENTS, TargetType.POST),
                "post",
                "comment_count"
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncPostViewCountsToDb() {
        log.info("[Sync] 게시물 조회수 동기화 시작");
        syncRedisCountsToDb(
                StatsKeyUtils.trackingKey(StatsKeyPrefix.VIEWS, TargetType.POST),
                "post",
                "view_count"
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncReactionCountsToDb() {
        log.info("[Sync] 반응 수 동기화 시작");

        syncRedisCountsToDb(
                StatsKeyUtils.trackingKey(StatsKeyPrefix.LIKES, TargetType.POST),
                "post", "like_count"
        );
        syncRedisCountsToDb(
                StatsKeyUtils.trackingKey(StatsKeyPrefix.DISLIKES, TargetType.POST),
                "post", "dislike_count"
        );
        syncRedisCountsToDb(
                StatsKeyUtils.trackingKey(StatsKeyPrefix.LIKES, TargetType.COMMENT),
                "comment", "like_count"
        );
        syncRedisCountsToDb(
                StatsKeyUtils.trackingKey(StatsKeyPrefix.DISLIKES, TargetType.COMMENT),
                "comment", "dislike_count"
        );

        log.info("[Sync] 반응 수 동기화 완료");
    }

    private void syncRedisCountsToDb(String trackingSetKey, String tableName, String columnName) {
        List<Object[]> dbBatchArgs = new ArrayList<>(PROCESSING_BATCH_SIZE);
        List<String> keysForProcessing = new ArrayList<>(PROCESSING_BATCH_SIZE);
        int totalProcessed = 0;

        ScanOptions options = ScanOptions.scanOptions()
                .count(SCAN_BATCH_SIZE)
                .build();

        try (Cursor<String> cursor = redisTemplate.opsForSet().scan(trackingSetKey, options)) {
            while (cursor.hasNext() && totalProcessed < MAX_KEYS_PER_SYNC) {
                String counterKey = cursor.next();
                keysForProcessing.add(counterKey);

                // 배치 크기에 도달하면 처리
                if (keysForProcessing.size() >= PROCESSING_BATCH_SIZE) {
                    int processed = processBatch(
                            keysForProcessing,
                            dbBatchArgs,
                            trackingSetKey,
                            tableName,
                            columnName
                    );
                    totalProcessed += processed;
                    keysForProcessing.clear();
                    dbBatchArgs.clear();

                    // 최대 처리량 체크
                    if (totalProcessed >= MAX_KEYS_PER_SYNC) {
                        log.info("[Sync] ⚠ 최대 처리량 도달 - Table: {}, 처리: {}, 남은 작업은 다음 스케줄에서 처리",
                                tableName, totalProcessed);
                        break;
                    }
                }
            }

            // 남은 항목 처리
            if (!keysForProcessing.isEmpty() && totalProcessed < MAX_KEYS_PER_SYNC) {
                int processed = processBatch(
                        keysForProcessing,
                        dbBatchArgs,
                        trackingSetKey,
                        tableName,
                        columnName
                );
                totalProcessed += processed;
            }

            log.info("[Sync] 완료 - Table: {}, Column: {}, 처리: {}",
                    tableName, columnName, totalProcessed);

        } catch (Exception e) {
            log.error("[Sync] 동기화 오류 - Table: {}, Column: {}, 처리: {}",
                    tableName, columnName, totalProcessed, e);
        }
    }

    private int processBatch(
            List<String> counterKeys,
            List<Object[]> dbBatchArgs,
            String trackingSetKey,
            String tableName,
            String columnName
    ) {
        try {
            // 1. Pipeline으로 모든 카운터 값 조회
            List<String> values = getValuesPipelined(counterKeys);
            log.info("[Sync]   ✓ Pipeline 조회 완료 - {}개", values.size());

            // 2. DB 배치 준비
            buildDbBatchArgs(counterKeys, values, dbBatchArgs);
            log.info("[Sync]   ✓ DB 배치 준비 완료 - 유효: {}/{}개", dbBatchArgs.size(), counterKeys.size());

            // 3. DB UPSERT
            if (!dbBatchArgs.isEmpty()) {
                flushBatchUpdateToDb(tableName, columnName, dbBatchArgs);
                log.info("[Sync]   ✓ DB 업데이트 완료 - {}건", dbBatchArgs.size());
            } else {
                log.warn("[Sync]   ⚠ 유효한 데이터가 없음 (모두 null 또는 파싱 실패)");

                for (int i = 0; i < Math.min(3, counterKeys.size()); i++) {
                    log.warn("[Sync]     - Key[{}]: {} → Value: {}",
                            i, counterKeys.get(i), values.get(i));
                }
            }

            // 4. 추적 SET에서 제거 (DB 업데이트 성공 건만 제거)
            if (!counterKeys.isEmpty()) {
                Long removed = redisTemplate.opsForSet().remove(trackingSetKey, counterKeys.toArray());
                log.info("[Sync]   ✓ 추적 SET 정리 완료 - 제거: {}건", removed);
            }

            return dbBatchArgs.size();

        } catch (Exception e) {
            log.error("[Sync]   ✗ 배치 처리 실패 - Table: {}, Keys: {}", tableName, counterKeys.size(), e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getValuesPipelined(List<String> keys) {
        return (List<String>) (List<?>) redisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    var keySerializer = redisTemplate.getStringSerializer();
                    for (String key : keys) {
                        byte[] serializedKey = keySerializer.serialize(key);
                        connection.stringCommands().get(serializedKey);
                    }
                    return null;
                },
                redisTemplate.getStringSerializer()
        );
    }

    private void buildDbBatchArgs(
            List<String> keys,
            List<String> values,
            List<Object[]> dbBatchArgs
    ) {
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String countStr = values.get(i);

            if (!StringUtils.hasText(countStr)) {
                continue;
            }

            try {
                long count = Long.parseLong(countStr);
                String id = StatsKeyUtils.extractId(key);

                dbBatchArgs.add(new Object[]{count, id});

            } catch (NumberFormatException e) {
                log.warn("[Sync] 숫자 변환 실패 - Key: {}, Value: {}", key, countStr);
            } catch (IllegalArgumentException e) {
                log.warn("[Sync] ID 추출 실패 - Key: {}", key);
            }
        }
    }

    private void flushBatchUpdateToDb(
            String tableName,
            String columnName,
            List<Object[]> batchArgs
    ) {
        if (batchArgs.isEmpty()) {
            return;
        }

        String sql = String.format(
                "UPDATE %s SET %s = ? WHERE id = ?",
                tableName, columnName
        );

        try {
            int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
            log.info("[Sync]     - DB 배치 결과: {} rows affected", results.length);
        } catch (Exception e) {
            log.error("[Sync]     - DB Batch 실패 - Table: {}, Column: {}, Size: {}",
                    tableName, columnName, batchArgs.size(), e);

            for (int i = 0; i < Math.min(3, batchArgs.size()); i++) {
                Object[] args = batchArgs.get(i);
                log.error("[Sync]       - 실패 데이터[{}]: {}={}, id={}",
                        i, columnName, args[0], args[1]);
            }
            throw new RuntimeException("DB Batch Update failed: ".concat(e.getMessage()), e);
        }
    }
}