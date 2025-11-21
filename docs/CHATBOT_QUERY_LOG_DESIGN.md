# 챗봇 질문-답변 로깅 기능 설계

## 📋 목적
사용자가 어떤 질문을 하는지, AI가 어떤 답변을 제공하는지 수집하여 서비스 개선에 활용

---

## 🏗️ 아키텍처 설계 (헥사고날 아키텍처 기반)

### 1️⃣ Domain 계층

#### ChatbotQueryLog 엔티티
```
chatbot/domain/model/ChatbotQueryLog.java
```

**필드:**
- `id`: String (ULID)
- `question`: String (사용자 질문, 최대 2000자)
- `answer`: Text (AI 답변, 긴 텍스트)
- `hasAnswer`: Boolean (답변 존재 여부)
- `hasImages`: Boolean (이미지 존재 여부)
- `references`: String (참고 URL)
- `responseTimeMs`: Long (응답 시간, 밀리초)
- `isSuccess`: Boolean (성공 여부)
- `errorMessage`: String (에러 메시지, nullable)
- `createdAt`: Instant (생성 시간, TimeBaseEntity 상속)
- `updatedAt`: Instant (수정 시간, TimeBaseEntity 상속)

#### Repository 인터페이스
```
chatbot/domain/repository/ChatbotQueryLogRepository.java
```

**메서드:**
- `save(ChatbotQueryLog log)`: 로그 저장
- `findAll(Pageable)`: 전체 로그 조회 (페이징)
- `findByCreatedAtBetween(Instant start, Instant end, Pageable)`: 기간별 조회
- `countByIsSuccess(Boolean isSuccess)`: 성공/실패 카운트

---

### 2️⃣ Infrastructure 계층

#### JPA 구현
```
chatbot/infrastructure/persistence/ChatbotQueryLogJpaRepository.java
chatbot/infrastructure/persistence/ChatbotQueryLogRepositoryImpl.java
```

---

### 3️⃣ Application 계층

#### ChatbotService 수정
```java
@Service
public class ChatbotService {

    private final AiChatbotClient aiChatbotClient;
    private final ChatbotQueryLogRepository queryLogRepository;

    public ChatbotAnswerResponse getAnswer(String question) {
        long startTime = System.currentTimeMillis();

        try {
            // AI 서버 호출
            AiAnswerResponse aiResponse = aiChatbotClient.getAnswer(question);
            long responseTime = System.currentTimeMillis() - startTime;

            // DTO 변환
            ChatbotAnswerResponse response = ...;

            // 로그 저장 (비동기)
            saveQueryLog(question, aiResponse, responseTime, true, null);

            return response;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;

            // 에러 로그 저장
            saveQueryLog(question, null, responseTime, false, e.getMessage());

            throw e;
        }
    }

    private void saveQueryLog(String question, AiAnswerResponse response,
                              long responseTime, boolean isSuccess, String errorMessage) {
        try {
            ChatbotQueryLog log = ChatbotQueryLog.builder()
                .question(question)
                .answer(response != null ? response.getAnswer() : null)
                .hasAnswer(response != null && response.hasAnswer())
                .hasImages(response != null && response.hasImages())
                .references(response != null ? response.getReferences() : null)
                .responseTimeMs(responseTime)
                .isSuccess(isSuccess)
                .errorMessage(errorMessage)
                .build();

            queryLogRepository.save(log);
        } catch (Exception e) {
            // 로깅 실패해도 메인 기능은 정상 동작
            log.error("Failed to save chatbot query log", e);
        }
    }
}
```

---

### 4️⃣ Presentation 계층 (선택적)

#### 관리자용 API (선택사항)
```
chatbot/presentation/controller/ChatbotLogController.java
```

**엔드포인트:**
- `GET /api/admin/chatbot/logs`: 로그 목록 조회
- `GET /api/admin/chatbot/logs/stats`: 통계 조회

---

## 📊 데이터베이스 스키마

### chatbot_query_log 테이블

```sql
CREATE TABLE chatbot_query_log (
    id VARCHAR(26) PRIMARY KEY COMMENT 'ULID',
    question VARCHAR(2000) NOT NULL COMMENT '사용자 질문',
    answer TEXT COMMENT 'AI 답변',
    has_answer BOOLEAN NOT NULL DEFAULT FALSE COMMENT '답변 존재 여부',
    has_images BOOLEAN NOT NULL DEFAULT FALSE COMMENT '이미지 존재 여부',
    references VARCHAR(500) COMMENT '참고 URL',
    response_time_ms BIGINT NOT NULL COMMENT '응답 시간(ms)',
    is_success BOOLEAN NOT NULL DEFAULT TRUE COMMENT '성공 여부',
    error_message VARCHAR(1000) COMMENT '에러 메시지',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시간',
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시간',
    INDEX idx_created_at (created_at),
    INDEX idx_is_success (is_success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='챗봇 질문-답변 로그';
```

---

## 🔄 Flyway 마이그레이션

```
src/main/resources/db/migration/V{next_version}__create_chatbot_query_log.sql
```

---

## 📈 활용 방안

### 1. 자주 묻는 질문 분석
```sql
SELECT question, COUNT(*) as cnt
FROM chatbot_query_log
WHERE is_success = TRUE
GROUP BY question
ORDER BY cnt DESC
LIMIT 20;
```

### 2. 실패 패턴 분석
```sql
SELECT question, error_message, COUNT(*) as cnt
FROM chatbot_query_log
WHERE is_success = FALSE
GROUP BY question, error_message
ORDER BY cnt DESC;
```

### 3. 응답 시간 분석
```sql
SELECT
    AVG(response_time_ms) as avg_time,
    MAX(response_time_ms) as max_time,
    MIN(response_time_ms) as min_time
FROM chatbot_query_log
WHERE is_success = TRUE
  AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY);
```

### 4. 답변 품질 분석
```sql
SELECT
    has_answer,
    has_images,
    COUNT(*) as cnt
FROM chatbot_query_log
WHERE is_success = TRUE
GROUP BY has_answer, has_images;
```

---

## 🔒 개인정보 보호 고려사항

### 1. 민감정보 필터링
- 질문에 개인정보(이메일, 전화번호, 학번 등) 포함 시 마스킹
- 정규식으로 패턴 감지 후 `***` 처리

### 2. 보관 기간 설정
- 로그 보관 기간: 6개월 ~ 1년
- 배치 작업으로 주기적 삭제

```java
@Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
public void deleteOldLogs() {
    Instant cutoffDate = Instant.now().minus(180, ChronoUnit.DAYS);
    queryLogRepository.deleteByCreatedAtBefore(cutoffDate);
}
```

---

## 🚀 구현 단계

### Phase 1: 기본 로깅 (필수)
- [x] 엔티티 및 Repository 생성
- [x] Flyway 마이그레이션
- [x] Service에서 로그 저장

### Phase 2: 통계 API (선택)
- [ ] 관리자용 조회 API
- [ ] 통계 대시보드

### Phase 3: 고도화 (선택)
- [ ] 개인정보 자동 마스킹
- [ ] 자동 삭제 배치 작업
- [ ] 엑셀 다운로드 기능

---

## 💡 장점

1. **서비스 개선**: 사용자 니즈 파악
2. **AI 품질 평가**: 답변 정확도 측정
3. **성능 모니터링**: 응답 시간 추적
4. **에러 디버깅**: 실패 패턴 분석

---

## ⚠️ 주의사항

1. **성능 영향 최소화**: 로그 저장 실패 시 메인 기능 영향 없도록
2. **비동기 처리 고려**: 대량 로그 발생 시 `@Async` 적용
3. **개인정보 보호**: GDPR, 개인정보보호법 준수
4. **인덱스 최적화**: created_at, is_success에 인덱스 필수

---

**작성일**: 2025-11-20
**작성자**: Claude (AI Assistant)
