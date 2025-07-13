package com.onion.backend.repository;

// JwtBlacklist entity class / JwtBlacklist 엔티티 클래스
import com.onion.backend.entity.JwtBlacklist;
// Spring Data JPA repository interface providing CRUD operations / CRUD 작업을 제공하는 Spring Data JPA 리포지토리 인터페이스
import org.springframework.data.jpa.repository.JpaRepository;
// Spring Data JPA modifying query annotation / Spring Data JPA 수정 쿼리 어노테이션
import org.springframework.data.jpa.repository.Modifying;
// Spring Data JPA custom query annotation / Spring Data JPA 커스텀 쿼리 어노테이션
import org.springframework.data.jpa.repository.Query;
// Spring Data parameter annotation / Spring Data 매개변수 어노테이션
import org.springframework.data.repository.query.Param;
// Spring transaction annotation / Spring 트랜잭션 어노테이션
import org.springframework.transaction.annotation.Transactional;
// Annotation marking this interface as a repository component / 이 인터페이스가 리포지토리 컴포넌트임을 나타내는 어노테이션
import org.springframework.stereotype.Repository;
// Spring Data pagination support / Spring Data 페이징 지원
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

// Java LocalDateTime for date/time operations / 날짜/시간 작업을 위한 Java LocalDateTime
import java.time.LocalDateTime;
// Java List interface for collections / 컬렉션을 위한 Java List 인터페이스
import java.util.List;
// Wrapper class for handling potentially null values safely / null 값을 안전하게 처리하기 위한 래퍼 클래스
import java.util.Optional;

@Repository // Marks this interface as a repository component for Spring component scanning / Spring 컴포넌트 스캔을 위한 리포지토리 컴포넌트 표시
public interface JwtBlacklistRepository extends JpaRepository<JwtBlacklist, Long> {
    // Extends JpaRepository<Entity Type, Primary Key Type> / JpaRepository<엔티티 타입, 기본 키 타입> 상속
    // Automatically provides basic CRUD methods (save, findById, delete, etc.) / 기본 CRUD 메서드 자동 제공 (save, findById, delete 등)

    // ===== CORE BLACKLIST OPERATIONS ===== / ===== 핵심 블랙리스트 작업 =====

    // Find blacklist entry by token hash / 토큰 해시로 블랙리스트 항목 찾기
    Optional<JwtBlacklist> findByTokenHash(String tokenHash);
    // Returns Optional to handle null safely / null을 안전하게 처리하기 위해 Optional 반환
    // ✅ GOOD: Uses Optional for null safety / 좋음: null 안전성을 위해 Optional 사용

    // Check if token hash exists in blacklist (most frequently used method) / 토큰 해시가 블랙리스트에 존재하는지 확인 (가장 자주 사용되는 메서드)
    boolean existsByTokenHash(String tokenHash);
    // Returns boolean for quick existence check / 빠른 존재 확인을 위해 boolean 반환
    // ✅ PERFORMANCE: Efficient existence check without loading entity / 성능: 엔티티 로드 없이 효율적인 존재 확인

    // Check if multiple tokens exist in blacklist (batch validation) / 여러 토큰이 블랙리스트에 존재하는지 확인 (배치 검증)
    @Query("SELECT j.tokenHash FROM JwtBlacklist j WHERE j.tokenHash IN :tokenHashes")
    List<String> findExistingTokenHashes(@Param("tokenHashes") List<String> tokenHashes);
    // Returns only existing token hashes for batch processing / 배치 처리를 위해 존재하는 토큰 해시만 반환
    // ✅ NEW FEATURE: Batch validation for improved performance / 새로운 기능: 성능 향상을 위한 배치 검증

    // ===== USER-SPECIFIC OPERATIONS ===== / ===== 사용자별 작업 =====

    // Find blacklist entries for a specific user with pagination / 페이징과 함께 특정 사용자의 블랙리스트 항목 찾기
    Page<JwtBlacklist> findByUsername(String username, Pageable pageable);
    // ✅ IMPROVEMENT: Added pagination for large datasets / 개선: 대용량 데이터셋을 위한 페이징 추가

    // Find blacklist entries for a specific user without pagination (for small datasets) / 페이징 없이 특정 사용자의 블랙리스트 항목 찾기 (소규모 데이터셋용)
    List<JwtBlacklist> findByUsername(String username);
    // Useful for user-specific token management / 사용자별 토큰 관리에 유용

    // Count blacklist entries for a specific user / 특정 사용자의 블랙리스트 항목 수 계산
    long countByUsername(String username);
    // Useful for monitoring user token activity / 사용자 토큰 활동 모니터링에 유용

    // Find recent blacklist entries for a specific user / 특정 사용자의 최근 블랙리스트 항목 찾기
    List<JwtBlacklist> findTop5ByUsernameOrderByCreatedAtDesc(String username);
    // Returns 5 most recent entries for a user / 사용자의 가장 최근 항목 5개 반환
    // ✅ NEW FEATURE: User-specific recent entries / 새로운 기능: 사용자별 최근 항목

    // ===== CLEANUP OPERATIONS ===== / ===== 정리 작업 =====

    // Find expired blacklist entries with pagination for safe cleanup / 안전한 정리를 위해 페이징과 함께 만료된 블랙리스트 항목 찾기
    Slice<JwtBlacklist> findByExpirationTimeBefore(LocalDateTime dateTime, Pageable pageable);
    // ✅ IMPROVEMENT: Uses Slice for memory-efficient batch processing / 개선: 메모리 효율적인 배치 처리를 위해 Slice 사용

    // Find expired blacklist entries (for backward compatibility) / 만료된 블랙리스트 항목 찾기 (하위 호환성용)
    List<JwtBlacklist> findByExpirationTimeBefore(LocalDateTime dateTime);
    // Returns entries that have expired before the given date / 주어진 날짜 이전에 만료된 항목 반환

    // Delete expired blacklist entries in batches / 배치로 만료된 블랙리스트 항목 삭제
    @Modifying // Indicates this query modifies data / 이 쿼리가 데이터를 수정함을 나타냄
    @Transactional // Ensures operation is wrapped in transaction / 작업이 트랜잭션으로 래핑됨을 보장
    @Query("DELETE FROM JwtBlacklist j WHERE j.expirationTime < :expirationTime")
    int deleteByExpirationTimeBefore(@Param("expirationTime") LocalDateTime expirationTime);
    // Returns number of deleted records / 삭제된 레코드 수 반환
    // ✅ PERFORMANCE: Bulk delete is more efficient than individual deletes / 성능: 대량 삭제가 개별 삭제보다 효율적

    // Delete expired entries in batches with limit / 제한과 함께 배치로 만료된 항목 삭제
    @Modifying // Indicates this query modifies data / 이 쿼리가 데이터를 수정함을 나타냄
    @Transactional // Ensures operation is wrapped in transaction / 작업이 트랜잭션으로 래핑됨을 보장
    @Query(value = "DELETE FROM jwt_blacklist WHERE expiration_time < :expirationTime LIMIT :batchSize", nativeQuery = true)
    int deleteExpiredEntriesInBatch(@Param("expirationTime") LocalDateTime expirationTime, @Param("batchSize") int batchSize);
    // ✅ NEW FEATURE: Batch size control for large cleanup operations / 새로운 기능: 대규모 정리 작업을 위한 배치 크기 제어

    // Count expired blacklist entries / 만료된 블랙리스트 항목 수 계산
    long countByExpirationTimeBefore(LocalDateTime dateTime);
    // Useful for cleanup planning and monitoring / 정리 계획 및 모니터링에 유용
    // ✅ NEW FEATURE: Count before cleanup for better planning / 새로운 기능: 더 나은 계획을 위한 정리 전 카운트

    // ===== MONITORING AND AUDITING ===== / ===== 모니터링 및 감사 =====

    // Find recent blacklist entries for monitoring / 모니터링을 위한 최근 블랙리스트 항목 찾기
    List<JwtBlacklist> findTop10ByOrderByCreatedAtDesc();
    // Returns 10 most recent blacklist entries / 가장 최근 블랙리스트 항목 10개 반환

    // Find blacklist entries by reason with pagination / 페이징과 함께 사유별 블랙리스트 항목 찾기
    Page<JwtBlacklist> findByReason(String reason, Pageable pageable);
    // ✅ IMPROVEMENT: Added pagination for large result sets / 개선: 큰 결과 세트를 위한 페이징 추가

    // Find blacklist entries by reason (for backward compatibility) / 사유별 블랙리스트 항목 찾기 (하위 호환성용)
    List<JwtBlacklist> findByReason(String reason);
    // Useful for auditing different types of token invalidation / 다양한 유형의 토큰 무효화 감사에 유용

    // Custom query to find entries created within a time range with pagination / 페이징과 함께 시간 범위 내에 생성된 항목을 찾는 커스텀 쿼리
    @Query("SELECT j FROM JwtBlacklist j WHERE j.createdAt BETWEEN :startTime AND :endTime ORDER BY j.createdAt DESC")
    Page<JwtBlacklist> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              Pageable pageable);
    // ✅ IMPROVEMENT: Added pagination and ordering / 개선: 페이징 및 정렬 추가

    // Custom query to find entries created within a time range (for backward compatibility) / 시간 범위 내에 생성된 항목을 찾는 커스텀 쿼리 (하위 호환성용)
    @Query("SELECT j FROM JwtBlacklist j WHERE j.createdAt BETWEEN :startTime AND :endTime")
    List<JwtBlacklist> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    // Useful for generating reports on token blacklisting activity / 토큰 블랙리스트 활동 보고서 생성에 유용

    // ===== STATISTICS AND REPORTING ===== / ===== 통계 및 보고 =====

    // Count blacklist entries by reason / 사유별 블랙리스트 항목 수 계산
    long countByReason(String reason);
    // Useful for generating statistics on invalidation reasons / 무효화 사유에 대한 통계 생성에 유용
    // ✅ NEW FEATURE: Reason-based statistics / 새로운 기능: 사유 기반 통계

    // Get blacklist statistics for a time period / 시간 기간별 블랙리스트 통계 가져오기
    @Query("SELECT j.reason, COUNT(j) FROM JwtBlacklist j WHERE j.createdAt BETWEEN :startTime AND :endTime GROUP BY j.reason")
    List<Object[]> getBlacklistStatisticsByReason(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    // Returns reason and count pairs for statistical analysis / 통계 분석을 위한 사유와 카운트 쌍 반환
    // ✅ NEW FEATURE: Grouped statistics for reporting / 새로운 기능: 보고를 위한 그룹화된 통계

    // Get daily blacklist activity / 일일 블랙리스트 활동 가져오기
    @Query("SELECT DATE(j.createdAt), COUNT(j) FROM JwtBlacklist j WHERE j.createdAt >= :startDate GROUP BY DATE(j.createdAt) ORDER BY DATE(j.createdAt)")
    List<Object[]> getDailyBlacklistActivity(@Param("startDate") LocalDateTime startDate);
    // Returns daily counts for trend analysis / 트렌드 분석을 위한 일일 카운트 반환
    // ✅ NEW FEATURE: Daily activity tracking / 새로운 기능: 일일 활동 추적

    // ===== ADMINISTRATIVE OPERATIONS ===== / ===== 관리 작업 =====

    // Delete all blacklist entries for a specific user efficiently / 특정 사용자의 모든 블랙리스트 항목을 효율적으로 삭제
    @Modifying // Indicates this query modifies data / 이 쿼리가 데이터를 수정함을 나타냄
    @Transactional // Ensures operation is wrapped in transaction / 작업이 트랜잭션으로 래핑됨을 보장
    @Query("DELETE FROM JwtBlacklist j WHERE j.username = :username")
    int deleteAllByUsername(@Param("username") String username);
    // ✅ IMPROVEMENT: More efficient bulk delete with custom query / 개선: 커스텀 쿼리로 더 효율적인 대량 삭제
    // Returns number of deleted records / 삭제된 레코드 수 반환

    // Delete blacklist entries older than specified days / 지정된 일수보다 오래된 블랙리스트 항목 삭제
    @Modifying // Indicates this query modifies data / 이 쿼리가 데이터를 수정함을 나타냄
    @Transactional // Ensures operation is wrapped in transaction / 작업이 트랜잭션으로 래핑됨을 보장
    @Query("DELETE FROM JwtBlacklist j WHERE j.createdAt < :cutoffDate")
    int deleteEntriesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    // Useful for general cleanup based on age / 나이 기반 일반 정리에 유용
    // ✅ NEW FEATURE: Age-based cleanup / 새로운 기능: 나이 기반 정리

    // ===== HEALTH CHECK OPERATIONS ===== / ===== 상태 확인 작업 =====

    // Count total active blacklist entries / 전체 활성 블랙리스트 항목 수 계산
    @Query("SELECT COUNT(j) FROM JwtBlacklist j WHERE j.expirationTime > :currentTime")
    long countActiveEntries(@Param("currentTime") LocalDateTime currentTime);
    // Useful for system health monitoring / 시스템 상태 모니터링에 유용
    // ✅ NEW FEATURE: Active entries count for health checks / 새로운 기능: 상태 확인을 위한 활성 항목 수

    // Find the oldest active blacklist entry / 가장 오래된 활성 블랙리스트 항목 찾기
    @Query("SELECT j FROM JwtBlacklist j WHERE j.expirationTime > :currentTime ORDER BY j.createdAt ASC")
    Optional<JwtBlacklist> findOldestActiveEntry(@Param("currentTime") LocalDateTime currentTime);
    // Useful for understanding data retention patterns / 데이터 보존 패턴 이해에 유용
    // ✅ NEW FEATURE: Data retention analysis / 새로운 기능: 데이터 보존 분석

    // Check if cleanup is needed based on table size / 테이블 크기 기반으로 정리가 필요한지 확인
    @Query("SELECT COUNT(j) > :threshold FROM JwtBlacklist j")
    boolean isCleanupNeeded(@Param("threshold") long threshold);
    // Returns true if cleanup is recommended / 정리가 권장되면 true 반환
    // ✅ NEW FEATURE: Automated cleanup decision support / 새로운 기능: 자동 정리 결정 지원
}