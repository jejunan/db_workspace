package com.onion.backend.service;

// Service implementation using improved repository / 개선된 리포지토리를 사용하는 서비스 구현

// JWT 블랙리스트 엔티티 - JWT 토큰 무효화 목적
import com.onion.backend.entity.JwtBlacklist;
// JWT 블랙리스트 리포지토리 - 데이터베이스 접근 계층
import com.onion.backend.repository.JwtBlacklistRepository;
// JWT 유틸리티 - 토큰 처리 및 검증 기능
import com.onion.backend.jwt.JwtUtil;
// Lombok - 코드 생성 어노테이션들 (생성자, 로깅 등)
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Spring Data - 페이징 및 정렬 지원
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
// Spring - 스케줄링 기능
import org.springframework.scheduling.annotation.Scheduled;
// Spring - 서비스 컴포넌트 정의
import org.springframework.stereotype.Service;
// Spring - 트랜잭션 관리
import org.springframework.transaction.annotation.Transactional;
// ✅ SECURITY IMPROVEMENT: Add validation support
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
// ✅ PERFORMANCE IMPROVEMENT: Add async support
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

// Java 8+ 날짜/시간 API
import java.time.LocalDateTime;
// Java 컬렉션 프레임워크
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// ✅ IMPROVEMENT: Add custom exceptions for better error handling
// 커스텀 예외 클래스들 - 더 나은 오류 처리를 위함
class BlacklistOperationException extends RuntimeException {
    public BlacklistOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

class TokenValidationException extends RuntimeException {
    public TokenValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

@Service // Spring 서비스 컴포넌트로 등록 - 비즈니스 로직 처리 계층
@RequiredArgsConstructor // Lombok: final 필드들을 매개변수로 하는 생성자 자동 생성
@Slf4j // Lombok: SLF4J 로거 인스턴스 자동 생성 (log 변수)
// ✅ IMPROVEMENT: Remove class-level @Transactional for more granular control
// 클래스 레벨 @Transactional 제거하여 더 세밀한 트랜잭션 제어
public class JwtBlacklistService {

    // 의존성 주입 - final로 선언하여 불변성 보장
    private final JwtBlacklistRepository blacklistRepository; // JWT 블랙리스트 데이터 접근 객체
    private final JwtUtil jwtUtil; // JWT 토큰 처리 유틸리티

    // ✅ IMPROVEMENT: Externalize configuration values
    // 설정값 외부화 - 하드코딩 방지 및 환경별 설정 가능
    @Value("${jwt.blacklist.batch-size:1000}")
    private int batchSize; // 배치 처리 크기 - 메모리 효율성과 성능 균형

    @Value("${jwt.blacklist.cleanup-threshold:50000}")
    private int cleanupThreshold; // 정리 임계값 - 자동 정리 트리거 기준

    @Value("${jwt.blacklist.default-expiration-hours:1}")
    private int defaultExpirationHours; // 기본 만료 시간 - 만료 시간이 없는 토큰 처리용

    // ===== CORE BLACKLIST OPERATIONS ===== / ===== 핵심 블랙리스트 작업 =====

    /**
     * Add token to blacklist with automatic expiration time extraction
     * 토큰을 블랙리스트에 추가 - 자동 만료 시간 추출 기능 포함
     *
     * @param token JWT 토큰 문자열
     * @param username 토큰 소유자 사용자명
     * @param reason 블랙리스트 추가 사유 (로그아웃, 보안 위반 등)
     */
    @Transactional // 데이터 일관성 보장을 위한 트랜잭션 처리
    public void addToBlacklist(String token, String username, String reason) {
        // ✅ IMPROVEMENT: Add input validation
        // 입력값 검증 추가 - 보안 및 안정성 향상
        validateInputs(token, username, reason);

        try {
            // JWT 토큰에서 만료 시간 자동 추출
            LocalDateTime expirationTime = jwtUtil.getExpirationTimeFromToken(token);

            // 만료 시간이 없는 경우 기본값 적용
            if (expirationTime == null) {
                expirationTime = LocalDateTime.now().plusHours(defaultExpirationHours);
                log.warn("Token has no expiration time, using default: {} hours", defaultExpirationHours);
            }

            // 팩토리 메서드를 사용한 엔티티 생성 - 객체 생성 로직 캡슐화
            JwtBlacklist blacklistEntry = JwtBlacklist.fromToken(token, username, expirationTime, reason);
            blacklistRepository.save(blacklistEntry);

            log.info("Token blacklisted for user: {} with reason: {}", username, reason);

        } catch (Exception e) {
            log.error("Failed to blacklist token for user: {}", username, e);
            // ✅ IMPROVEMENT: Use custom exception
            // 커스텀 예외 사용 - 더 명확한 오류 분류
            throw new BlacklistOperationException("Failed to blacklist token for user: " + username, e);
        }
    }

    /**
     * Check if token is blacklisted (most frequently called method)
     * 토큰 블랙리스트 여부 확인 - 가장 빈번히 호출되는 메서드
     *
     * @param token 검증할 JWT 토큰
     * @return true if blacklisted, false otherwise
     */
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션 - 성능 최적화
    public boolean isTokenBlacklisted(String token) {
        // ✅ IMPROVEMENT: Add input validation
        // 빈 토큰 체크 - 불필요한 처리 방지
        if (!StringUtils.hasText(token)) {
            log.debug("Empty token provided for blacklist check");
            return true; // 보안상 블랙리스트된 것으로 처리
        }

        try {
            // 토큰 해시화 후 데이터베이스 조회 - 보안 및 성능 향상
            String tokenHash = JwtBlacklist.hashToken(token);
            return blacklistRepository.existsByTokenHash(tokenHash);
        } catch (Exception e) {
            log.error("Error checking token blacklist status", e);
            // ✅ SECURITY: Fail-safe approach
            // 오류 시 안전 우선 접근법 - 보안 강화
            return true;
        }
    }

    /**
     * Batch validation of multiple tokens for improved performance
     * 배치 토큰 검증 - 대량 토큰 처리 시 성능 향상
     *
     * @param tokens 검증할 토큰 목록
     * @return 토큰별 블랙리스트 여부 맵
     */
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    public Map<String, Boolean> validateTokenBatch(List<String> tokens) {
        // ✅ IMPROVEMENT: Add input validation
        // 입력 검증 - null 및 빈 리스트 처리
        if (tokens == null || tokens.isEmpty()) {
            log.debug("Empty or null token list provided for batch validation");
            return Map.of();
        }

        try {
            // 모든 토큰을 해시화 - 데이터베이스 조회용
            List<String> tokenHashes = tokens.stream()
                    .filter(StringUtils::hasText) // null 및 빈 토큰 필터링
                    .map(JwtBlacklist::hashToken)
                    .collect(Collectors.toList());

            // 단일 쿼리로 존재하는 해시 조회 - 성능 최적화
            List<String> existingHashes = blacklistRepository.findExistingTokenHashes(tokenHashes);

            // 결과 맵 생성 - 원본 토큰과 블랙리스트 상태 매핑
            return tokens.stream()
                    .collect(Collectors.toMap(
                            token -> token,
                            token -> !StringUtils.hasText(token) ||
                                    existingHashes.contains(JwtBlacklist.hashToken(token))
                    ));

        } catch (Exception e) {
            log.error("Error in batch token validation", e);
            // 오류 시 모든 토큰을 블랙리스트된 것으로 처리 - 보안 우선
            return tokens.stream()
                    .collect(Collectors.toMap(token -> token, token -> true));
        }
    }

    // ===== USER MANAGEMENT ===== / ===== 사용자 관리 =====

    /**
     * Get user's blacklist entries with pagination
     * 사용자별 블랙리스트 항목 조회 - 페이징 지원
     */
    @Transactional(readOnly = true)
    public Page<JwtBlacklist> getUserBlacklistEntries(String username, Pageable pageable) {
        // ✅ IMPROVEMENT: Add input validation
        validateUsername(username);
        return blacklistRepository.findByUsername(username, pageable);
    }

    /**
     * Get user's blacklist statistics
     * 사용자 블랙리스트 통계 조회
     */
    @Transactional(readOnly = true)
    public UserBlacklistStats getUserBlacklistStats(String username) {
        validateUsername(username);

        // 통계 데이터 수집
        long totalCount = blacklistRepository.countByUsername(username);
        List<JwtBlacklist> recentEntries = blacklistRepository.findTop5ByUsernameOrderByCreatedAtDesc(username);

        return UserBlacklistStats.builder()
                .username(username)
                .totalBlacklistedTokens(totalCount)
                .recentEntries(recentEntries)
                .build();
    }

    /**
     * Remove all blacklist entries for a user (when account is deleted)
     * 사용자 계정 삭제 시 모든 블랙리스트 항목 제거
     */
    @Transactional // 데이터 정합성을 위한 트랜잭션
    public int removeAllUserEntries(String username) {
        validateUsername(username);

        int deletedCount = blacklistRepository.deleteAllByUsername(username);
        log.info("Removed {} blacklist entries for user: {}", deletedCount, username);
        return deletedCount;
    }

    // ===== CLEANUP OPERATIONS ===== / ===== 정리 작업 =====

    /**
     * Scheduled cleanup of expired blacklist entries
     * 만료된 블랙리스트 항목 스케줄링 정리
     */
    @Scheduled(fixedRateString = "${jwt.blacklist.cleanup-interval:3600000}") // 1시간마다 실행
    @Async("blacklistTaskExecutor") // ✅ IMPROVEMENT: Make cleanup async
    // 비동기 처리로 변경 - 애플리케이션 블로킹 방지
    public CompletableFuture<Void> scheduledCleanup() {
        try {
            // 정리 필요성 체크 - 불필요한 작업 방지
            if (!blacklistRepository.isCleanupNeeded(cleanupThreshold)) {
                log.debug("Blacklist cleanup not needed, table size below threshold: {}", cleanupThreshold);
                return CompletableFuture.completedFuture(null);
            }

            LocalDateTime now = LocalDateTime.now();
            int cleanedUp = performBatchCleanup(now);

            if (cleanedUp > 0) {
                log.info("Scheduled cleanup completed: {} expired entries removed", cleanedUp);
            }

        } catch (Exception e) {
            log.error("Error during scheduled blacklist cleanup", e);
            // 스케줄링 작업 실패 시 애플리케이션은 계속 실행
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Perform batch cleanup of expired entries
     * 만료된 항목 배치 정리 수행
     */
    @Transactional // 트랜잭션으로 데이터 일관성 보장
    public int performBatchCleanup(LocalDateTime cutoffTime) {
        // ✅ IMPROVEMENT: Add input validation
        if (cutoffTime == null) {
            throw new IllegalArgumentException("Cutoff time cannot be null");
        }

        int totalDeleted = 0;

        try {
            // 정리 대상 항목 수 로깅 - 운영 모니터링용
            long expiredCount = blacklistRepository.countByExpirationTimeBefore(cutoffTime);
            log.info("Starting cleanup of {} expired blacklist entries", expiredCount);

            // 배치 단위로 정리 수행 - 메모리 효율성 및 락 시간 최소화
            int deletedInBatch;
            do {
                deletedInBatch = blacklistRepository.deleteExpiredEntriesInBatch(cutoffTime, batchSize);
                totalDeleted += deletedInBatch;

                if (deletedInBatch > 0) {
                    log.debug("Deleted {} entries in current batch, total: {}", deletedInBatch, totalDeleted);
                }

                // 무한 루프 방지 체크
                if (deletedInBatch == 0) break;

            } while (deletedInBatch >= batchSize);

            log.info("Batch cleanup completed: {} total entries removed", totalDeleted);

        } catch (Exception e) {
            log.error("Error during batch cleanup", e);
            // ✅ IMPROVEMENT: Use custom exception
            throw new BlacklistOperationException("Cleanup operation failed", e);
        }

        return totalDeleted;
    }

    /**
     * Clean entries older than specified days
     * 지정된 일수보다 오래된 항목 정리
     */
    @Transactional
    public int cleanOldEntries(int daysOld) {
        // ✅ IMPROVEMENT: Add validation
        if (daysOld < 0) {
            throw new IllegalArgumentException("Days old must be non-negative");
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = blacklistRepository.deleteEntriesOlderThan(cutoffDate);

        log.info("Cleaned {} entries older than {} days", deletedCount, daysOld);
        return deletedCount;
    }

    // ===== MONITORING AND STATISTICS ===== / ===== 모니터링 및 통계 =====

    /**
     * Get blacklist statistics for monitoring dashboard
     * 모니터링 대시보드용 블랙리스트 통계
     */
    @Transactional(readOnly = true)
    public BlacklistStats getBlacklistStats() {
        LocalDateTime now = LocalDateTime.now();

        return BlacklistStats.builder()
                .totalEntries(blacklistRepository.count())
                .activeEntries(blacklistRepository.countActiveEntries(now))
                .expiredEntries(blacklistRepository.countByExpirationTimeBefore(now))
                .recentEntries(blacklistRepository.findTop10ByOrderByCreatedAtDesc())
                .oldestActiveEntry(blacklistRepository.findOldestActiveEntry(now))
                .build();
    }

    /**
     * Get blacklist statistics by reason for a time period
     * 기간별 사유별 블랙리스트 통계
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatisticsByReason(LocalDateTime startTime, LocalDateTime endTime) {
        // ✅ IMPROVEMENT: Add input validation
        validateTimeRange(startTime, endTime);

        List<Object[]> results = blacklistRepository.getBlacklistStatisticsByReason(startTime, endTime);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0], // 사유
                        result -> (Long) result[1]    // 개수
                ));
    }

    /**
     * Get daily blacklist activity for trend analysis
     * 트렌드 분석용 일일 블랙리스트 활동
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getDailyActivity(int days) {
        // ✅ IMPROVEMENT: Add validation
        if (days < 1) {
            throw new IllegalArgumentException("Days must be positive");
        }

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = blacklistRepository.getDailyBlacklistActivity(startDate);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> result[0].toString(), // 날짜
                        result -> (Long) result[1]       // 개수
                ));
    }

    // ===== HEALTH CHECK ===== / ===== 상태 확인 =====

    /**
     * Check blacklist service health
     * 블랙리스트 서비스 상태 확인
     */
    @Transactional(readOnly = true)
    public BlacklistHealthCheck checkHealth() {
        try {
            LocalDateTime now = LocalDateTime.now();
            long totalEntries = blacklistRepository.count();
            long activeEntries = blacklistRepository.countActiveEntries(now);
            long expiredEntries = blacklistRepository.countByExpirationTimeBefore(now);

            // 상태 메트릭 계산
            double activeRatio = totalEntries > 0 ? (double) activeEntries / totalEntries : 0;
            boolean needsCleanup = blacklistRepository.isCleanupNeeded(cleanupThreshold);

            // 상태 판정 로직
            BlacklistHealthStatus status = BlacklistHealthStatus.HEALTHY;
            if (needsCleanup) {
                status = BlacklistHealthStatus.NEEDS_CLEANUP;
            } else if (activeRatio < 0.1) { // 10% 미만 활성
                status = BlacklistHealthStatus.TOO_MANY_EXPIRED;
            }

            return BlacklistHealthCheck.builder()
                    .status(status)
                    .totalEntries(totalEntries)
                    .activeEntries(activeEntries)
                    .expiredEntries(expiredEntries)
                    .activeRatio(activeRatio)
                    .needsCleanup(needsCleanup)
                    .checkTime(now)
                    .build();

        } catch (Exception e) {
            log.error("Health check failed", e);
            return BlacklistHealthCheck.builder()
                    .status(BlacklistHealthStatus.ERROR)
                    .checkTime(LocalDateTime.now())
                    .build();
        }
    }

    // ===== PRIVATE HELPER METHODS ===== / ===== 비공개 도우미 메서드 =====

    /**
     * Validate input parameters for blacklist operations
     * 블랙리스트 작업 입력 매개변수 검증
     */
    private void validateInputs(String token, String username, String reason) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
    }

    /**
     * Validate username parameter
     * 사용자명 매개변수 검증
     */
    private void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
    }

    /**
     * Validate time range parameters
     * 시간 범위 매개변수 검증
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    // ===== INNER CLASSES FOR STATISTICS ===== / ===== 통계용 내부 클래스 =====

    /**
     * User-specific blacklist statistics
     * 사용자별 블랙리스트 통계
     */
    @lombok.Data
    @lombok.Builder
    public static class UserBlacklistStats {
        private String username; // 사용자명
        private long totalBlacklistedTokens; // 총 블랙리스트 토큰 수
        private List<JwtBlacklist> recentEntries; // 최근 항목들
    }

    /**
     * Overall blacklist statistics
     * 전체 블랙리스트 통계
     */
    @lombok.Data
    @lombok.Builder
    public static class BlacklistStats {
        private long totalEntries; // 총 항목 수
        private long activeEntries; // 활성 항목 수
        private long expiredEntries; // 만료된 항목 수
        private List<JwtBlacklist> recentEntries; // 최근 항목들
        private Optional<JwtBlacklist> oldestActiveEntry; // 가장 오래된 활성 항목
    }

    /**
     * Health check result
     * 상태 확인 결과
     */
    @lombok.Data
    @lombok.Builder
    public static class BlacklistHealthCheck {
        private BlacklistHealthStatus status; // 상태
        private long totalEntries; // 총 항목 수
        private long activeEntries; // 활성 항목 수
        private long expiredEntries; // 만료된 항목 수
        private double activeRatio; // 활성 비율
        private boolean needsCleanup; // 정리 필요 여부
        private LocalDateTime checkTime; // 확인 시간
    }

    /**
     * Health status enumeration
     * 상태 열거형
     */
    public enum BlacklistHealthStatus {
        HEALTHY,           // 정상 상태
        NEEDS_CLEANUP,     // 정리 필요
        TOO_MANY_EXPIRED,  // 만료된 항목 과다
        ERROR              // 오류 상태
    }
}