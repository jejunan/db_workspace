package com.onion.backend.repository;

// User entity class representing database user table / 데이터베이스 사용자 테이블을 나타내는 User 엔티티 클래스
import com.onion.backend.entity.User;
// Spring Data pagination and sorting support / Spring Data 페이징 및 정렬 지원
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
// Spring Data JPA repository interface providing CRUD operations / CRUD 작업을 제공하는 Spring Data JPA 리포지토리 인터페이스
import org.springframework.data.jpa.repository.JpaRepository;
// Spring Data JPA specification executor for dynamic queries / 동적 쿼리를 위한 Spring Data JPA 사양 실행자
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
// Spring Data JPA custom query annotation / Spring Data JPA 커스텀 쿼리 어노테이션
import org.springframework.data.jpa.repository.Query;
// Spring Data JPA modifying query annotation / Spring Data JPA 수정 쿼리 어노테이션
import org.springframework.data.jpa.repository.Modifying;
// Spring Data parameter annotation / Spring Data 매개변수 어노테이션
import org.springframework.data.repository.query.Param;
// Spring transaction annotation / Spring 트랜잭션 어노테이션
import org.springframework.transaction.annotation.Transactional;
// Annotation marking this interface as a repository component / 이 인터페이스가 리포지토리 컴포넌트임을 나타내는 어노테이션
import org.springframework.stereotype.Repository;

// Java LocalDateTime for date/time operations / 날짜/시간 작업을 위한 Java LocalDateTime
import java.time.LocalDateTime;
// Java List interface for collections / 컬렉션을 위한 Java List 인터페이스
import java.util.List;
// Wrapper class for handling potentially null values safely / null 값을 안전하게 처리하기 위한 래퍼 클래스
import java.util.Optional;
// Java Set for collections / 컬렉션을 위한 Java Set
import java.util.Set;

@Repository // Marks this interface as a repository component for Spring component scanning / Spring 컴포넌트 스캔을 위한 리포지토리 컴포넌트 표시
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    // Extends JpaRepository for basic CRUD and JpaSpecificationExecutor for dynamic queries / 기본 CRUD를 위한 JpaRepository와 동적 쿼리를 위한 JpaSpecificationExecutor 확장
    // ✅ IMPROVEMENT: Added JpaSpecificationExecutor for complex dynamic queries / 개선: 복잡한 동적 쿼리를 위한 JpaSpecificationExecutor 추가

    // ===== BASIC QUERY METHODS ===== / ===== 기본 조회 메서드들 =====

    // Find user by username (case-sensitive) / 사용자명으로 사용자 찾기 (대소문자 구분)
    Optional<User> findByUsername(String username);
    // Returns Optional to handle null safely / null을 안전하게 처리하기 위해 Optional 반환

    // Find user by username (case-insensitive) / 사용자명으로 사용자 찾기 (대소문자 구분 안함)
    Optional<User> findByUsernameIgnoreCase(String username);
    // ✅ NEW FEATURE: Case-insensitive username lookup / 새로운 기능: 대소문자 구분 안하는 사용자명 조회

    // Find user by email address / 이메일 주소로 사용자 찾기
    Optional<User> findByEmail(String email);
    // Returns Optional to handle null safely / null을 안전하게 처리하기 위해 Optional 반환

    // Find user by email (case-insensitive) / 이메일로 사용자 찾기 (대소문자 구분 안함)
    Optional<User> findByEmailIgnoreCase(String email);
    // ✅ NEW FEATURE: Case-insensitive email lookup / 새로운 기능: 대소문자 구분 안하는 이메일 조회

    // Find active user by username / 사용자명으로 활성 사용자 찾기
    Optional<User> findByUsernameAndIsActiveTrue(String username);
    // For authentication - only returns active users / 인증용 - 활성 사용자만 반환

    // Find users by multiple IDs efficiently / 여러 ID로 효율적으로 사용자 찾기
    List<User> findByIdIn(Set<Long> ids);
    // ✅ NEW FEATURE: Batch user lookup / 새로운 기능: 배치 사용자 조회

    // ===== EXISTENCE CHECK METHODS ===== / ===== 존재 확인 메서드들 =====

    // Check if username exists (case-sensitive) / 사용자명이 존재하는지 확인 (대소문자 구분)
    boolean existsByUsername(String username);
    // Efficient existence check without loading entity / 엔티티 로드 없이 효율적인 존재 확인

    // Check if username exists (case-insensitive) / 사용자명이 존재하는지 확인 (대소문자 구분 안함)
    boolean existsByUsernameIgnoreCase(String username);
    // ✅ NEW FEATURE: Case-insensitive username existence check / 새로운 기능: 대소문자 구분 안하는 사용자명 존재 확인

    // Check if email exists / 이메일이 존재하는지 확인
    boolean existsByEmail(String email);
    // Efficient existence check without loading entity / 엔티티 로드 없이 효율적인 존재 확인

    // Check if email exists (case-insensitive) / 이메일이 존재하는지 확인 (대소문자 구분 안함)
    boolean existsByEmailIgnoreCase(String email);
    // ✅ NEW FEATURE: Case-insensitive email existence check / 새로운 기능: 대소문자 구분 안하는 이메일 존재 확인

    // Check if email exists for different user (for profile updates) / 다른 사용자의 이메일이 존재하는지 확인 (프로필 업데이트용)
    boolean existsByEmailAndIdNot(String email, Long id);
    // Allows checking email uniqueness excluding current user / 현재 사용자를 제외한 이메일 고유성 확인 허용

    // Check if email exists for different user (case-insensitive) / 다른 사용자의 이메일이 존재하는지 확인 (대소문자 구분 안함)
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    // ✅ NEW FEATURE: Case-insensitive email uniqueness check / 새로운 기능: 대소문자 구분 안하는 이메일 고유성 확인

    // ===== ACTIVE USER QUERIES ===== / ===== 활성 사용자 조회 =====

    // Find all active users with pagination / 페이징과 함께 모든 활성 사용자 찾기
    Page<User> findByIsActiveTrue(Pageable pageable);
    // Uses pagination for potentially large datasets / 잠재적으로 큰 데이터셋을 위한 페이징 사용

    // Find active users who logged in after specific date with pagination / 페이징과 함께 특정 날짜 이후에 로그인한 활성 사용자 찾기
    Page<User> findByIsActiveTrueAndLastLoginAfter(LocalDateTime date, Pageable pageable);
    // ✅ IMPROVEMENT: Added pagination for large result sets / 개선: 큰 결과 세트를 위한 페이징 추가

    // Find active users who logged in after specific date (for backward compatibility) / 특정 날짜 이후에 로그인한 활성 사용자 찾기 (하위 호환성용)
    List<User> findByIsActiveTrueAndLastLoginAfter(LocalDateTime date);
    // For backward compatibility with existing code / 기존 코드와의 하위 호환성을 위함

    // Find active users who haven't logged in recently / 최근에 로그인하지 않은 활성 사용자 찾기
    Page<User> findByIsActiveTrueAndLastLoginBeforeOrLastLoginIsNull(LocalDateTime date, Pageable pageable);
    // ✅ NEW FEATURE: Find inactive users for engagement campaigns / 새로운 기능: 참여 캠페인을 위한 비활성 사용자 찾기

    // Find users created within date range / 날짜 범위 내에 생성된 사용자 찾기
    Page<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    // ✅ NEW FEATURE: User registration analytics / 새로운 기능: 사용자 등록 분석

    // ===== ROLE-BASED USER QUERIES ===== / ===== 역할별 사용자 조회 =====

    // Find active users by role name with pagination / 페이징과 함께 역할 이름으로 활성 사용자 찾기
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isActive = true")
    Page<User> findByRoleNameAndActiveTrue(@Param("roleName") String roleName, Pageable pageable);
    // ✅ IMPROVEMENT: Added pagination for large role memberships / 개선: 큰 역할 멤버십을 위한 페이징 추가

    // Find active users by role name (for backward compatibility) / 역할 이름으로 활성 사용자 찾기 (하위 호환성용)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isActive = true")
    List<User> findByRoleNameAndActiveTrue(@Param("roleName") String roleName);
    // For backward compatibility with existing code / 기존 코드와의 하위 호환성을 위함

    // Find users by multiple role names / 여러 역할 이름으로 사용자 찾기
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames AND u.isActive = true")
    Page<User> findByRoleNamesAndActiveTrue(@Param("roleNames") Set<String> roleNames, Pageable pageable);
    // ✅ NEW FEATURE: Multi-role user lookup / 새로운 기능: 다중 역할 사용자 조회

    // Find users who have all specified roles / 지정된 모든 역할을 가진 사용자 찾기
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
            "(SELECT COUNT(r) FROM u.roles r WHERE r.name IN :roleNames) = :roleCount")
    Page<User> findByAllRoles(@Param("roleNames") Set<String> roleNames, @Param("roleCount") long roleCount, Pageable pageable);
    // ✅ NEW FEATURE: Users with all specified roles / 새로운 기능: 지정된 모든 역할을 가진 사용자

    // ===== EMAIL VERIFICATION STATUS QUERIES ===== / ===== 이메일 검증 상태별 조회 =====

    // Find users with unverified emails with pagination / 페이징과 함께 검증되지 않은 이메일을 가진 사용자 찾기
    Page<User> findByIsEmailVerifiedFalse(Pageable pageable);
    // ✅ IMPROVEMENT: Added pagination for large datasets / 개선: 큰 데이터셋을 위한 페이징 추가

    // Find users with unverified emails (for backward compatibility) / 검증되지 않은 이메일을 가진 사용자 찾기 (하위 호환성용)
    List<User> findByIsEmailVerifiedFalse();
    // For backward compatibility with existing code / 기존 코드와의 하위 호환성을 위함

    // Find active users with unverified emails / 검증되지 않은 이메일을 가진 활성 사용자 찾기
    Page<User> findByIsActiveTrueAndIsEmailVerifiedFalse(Pageable pageable);
    // ✅ NEW FEATURE: Active users needing email verification / 새로운 기능: 이메일 검증이 필요한 활성 사용자

    // Find users who need email verification reminders / 이메일 검증 알림이 필요한 사용자 찾기
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.isEmailVerified = false AND u.createdAt < :cutoffDate")
    Page<User> findUsersNeedingEmailVerificationReminder(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
    // ✅ NEW FEATURE: Users needing verification reminders / 새로운 기능: 검증 알림이 필요한 사용자

    // ===== SEARCH AND FILTERING ===== / ===== 검색 및 필터링 =====

    // Search users by username or email (case-insensitive) / 사용자명 또는 이메일로 사용자 검색 (대소문자 구분 안함)
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND u.isActive = true")
    Page<User> searchActiveUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
    // ✅ NEW FEATURE: Comprehensive user search / 새로운 기능: 포괄적인 사용자 검색

    // Find users by name (first name or last name) / 이름(이름 또는 성)으로 사용자 찾기
    Page<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndIsActiveTrue(
            String firstName, String lastName, Pageable pageable);
    // ✅ NEW FEATURE: Name-based user search / 새로운 기능: 이름 기반 사용자 검색

    // ===== STATISTICAL QUERIES ===== / ===== 통계 쿼리들 =====

    // Count active users / 활성 사용자 수 계산
    long countByIsActiveTrue();
    // Efficient count operation / 효율적인 카운트 작업

    // Count users with verified emails / 검증된 이메일을 가진 사용자 수 계산
    long countByIsEmailVerifiedTrue();
    // Efficient count operation / 효율적인 카운트 작업

    // Count active users with verified emails / 검증된 이메일을 가진 활성 사용자 수 계산
    long countByIsActiveTrueAndIsEmailVerifiedTrue();
    // ✅ NEW FEATURE: Active and verified users count / 새로운 기능: 활성 및 검증된 사용자 수

    // Count users by role name / 역할 이름별 사용자 수 계산
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);
    // Custom JPQL query for role-based counting / 역할 기반 카운팅을 위한 커스텀 JPQL 쿼리

    // Count users created in date range / 날짜 범위에 생성된 사용자 수 계산
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    // ✅ NEW FEATURE: Registration analytics / 새로운 기능: 등록 분석

    // Get user registration statistics by month / 월별 사용자 등록 통계 가져오기
    @Query("SELECT YEAR(u.createdAt), MONTH(u.createdAt), COUNT(u) " +
            "FROM User u WHERE u.createdAt >= :startDate " +
            "GROUP BY YEAR(u.createdAt), MONTH(u.createdAt) " +
            "ORDER BY YEAR(u.createdAt), MONTH(u.createdAt)")
    List<Object[]> getUserRegistrationStatsByMonth(@Param("startDate") LocalDateTime startDate);
    // ✅ NEW FEATURE: Monthly registration statistics / 새로운 기능: 월별 등록 통계

    // ===== ADMINISTRATIVE OPERATIONS ===== / ===== 관리 작업 =====

    // Soft delete users (deactivate) by updating isActive flag / isActive 플래그 업데이트로 사용자 소프트 삭제 (비활성화)
    @Modifying // Indicates this query modifies data / 이 쿼리가 데이터를 수정함을 나타냄
    @Transactional // Ensures operation is wrapped in transaction / 작업이 트랜잭션으로 래핑됨을 보장
    @Query("UPDATE User u SET u.isActive = false WHERE u.id IN :userIds")
    int deactivateUsers(@Param("userIds") Set<Long> userIds);
    // ✅ NEW FEATURE: Batch user deactivation / 새로운 기능: 배치 사용자 비활성화

    // Reactivate users by updating isActive flag / isActive 플래그 업데이트로 사용자 재활성화
    @Modifying // Indicates this query modifies data / 이 쿼리가 데이터를 수정함을 나타냄
    @Transactional // Ensures operation is wrapped in transaction / 작업이 트랜잭션으로 래핑됨을 보장
    @Query("UPDATE User u SET u.isActive = true WHERE u.id IN :userIds")
    int reactivateUsers(@Param("userIds") Set<Long> userIds);
    // ✅ NEW FEATURE: Batch user reactivation / 새로운 기능: 배치 사용자 재활성화

    // Update email verification status for multiple users / 여러 사용자의 이메일 검증 상태 업데이트
    @Modifying // Indicates this query modifies data / 이 쿼리가 데이터를 수정함을 나타냄
    @Transactional // Ensures operation is wrapped in transaction / 작업이 트랜잭션으로 래핑됨을 보장
    @Query("UPDATE User u SET u.isEmailVerified = :verified WHERE u.id IN :userIds")
    int updateEmailVerificationStatus(@Param("userIds") Set<Long> userIds, @Param("verified") boolean verified);
    // ✅ NEW FEATURE: Batch email verification update / 새로운 기능: 배치 이메일 검증 업데이트

    // ===== CLEANUP OPERATIONS ===== / ===== 정리 작업 =====

    // Find inactive users for cleanup (never logged in and old accounts) / 정리를 위한 비활성 사용자 찾기 (로그인한 적 없고 오래된 계정)
    @Query("SELECT u FROM User u WHERE u.isActive = false AND u.createdAt < :cutoffDate")
    Slice<User> findInactiveUsersForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
    // ✅ NEW FEATURE: Find old inactive accounts for cleanup / 새로운 기능: 정리를 위한 오래된 비활성 계정 찾기

    // Find users who never logged in and are old / 로그인한 적 없고 오래된 사용자 찾기
    Page<User> findByLastLoginIsNullAndCreatedAtBefore(LocalDateTime cutoffDate, Pageable pageable);
    // ✅ NEW FEATURE: Never-logged-in old accounts / 새로운 기능: 로그인한 적 없는 오래된 계정

    // Count users eligible for cleanup / 정리 대상 사용자 수 계산
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = false AND u.createdAt < :cutoffDate")
    long countInactiveUsersForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate);
    // ✅ NEW FEATURE: Count users eligible for cleanup / 새로운 기능: 정리 대상 사용자 수 계산

    // ===== SECURITY AND MONITORING ===== / ===== 보안 및 모니터링 =====

    // Find users with recent password changes / 최근 비밀번호 변경한 사용자 찾기
    Page<User> findByPasswordChangedAtAfter(LocalDateTime date, Pageable pageable);
    // ✅ NEW FEATURE: Recent password changes tracking / 새로운 기능: 최근 비밀번호 변경 추적

    // Find users who haven't changed password in a long time / 오랫동안 비밀번호를 변경하지 않은 사용자 찾기
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
            "(u.passwordChangedAt IS NULL OR u.passwordChangedAt < :cutoffDate)")
    Page<User> findUsersWithOldPasswords(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
    // ✅ NEW FEATURE: Users needing password updates / 새로운 기능: 비밀번호 업데이트가 필요한 사용자

    // Find recently registered users / 최근 등록된 사용자 찾기
    Page<User> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date, Pageable pageable);
    // ✅ NEW FEATURE: Recent registrations monitoring / 새로운 기능: 최근 등록 모니터링

    // Find users with suspicious activity patterns / 의심스러운 활동 패턴을 가진 사용자 찾기
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.lastLogin IS NOT NULL AND " +
            "u.lastLogin > :recentDate AND " +
            "(SELECT COUNT(j) FROM JwtBlacklist j WHERE j.username = u.username AND j.createdAt > :recentDate) > :threshold")
    Page<User> findUsersWithSuspiciousActivity(@Param("recentDate") LocalDateTime recentDate,
                                               @Param("threshold") long threshold,
                                               Pageable pageable);
    // ✅ NEW FEATURE: Security monitoring for suspicious activity / 새로운 기능: 의심스러운 활동에 대한 보안 모니터링

    // ===== REPORTING AND ANALYTICS ===== / ===== 보고 및 분석 =====

    // Get user activity summary / 사용자 활동 요약 가져오기
    @Query("SELECT " +
            "COUNT(u) as totalUsers, " +
            "SUM(CASE WHEN u.isActive = true THEN 1 ELSE 0 END) as activeUsers, " +
            "SUM(CASE WHEN u.isEmailVerified = true THEN 1 ELSE 0 END) as verifiedUsers, " +
            "SUM(CASE WHEN u.lastLogin > :recentDate THEN 1 ELSE 0 END) as recentlyActiveUsers " +
            "FROM User u")
    Object[] getUserActivitySummary(@Param("recentDate") LocalDateTime recentDate);
    // ✅ NEW FEATURE: Comprehensive user activity summary / 새로운 기능: 포괄적인 사용자 활동 요약

    // Get role distribution statistics / 역할 분포 통계 가져오기
    @Query("SELECT r.name, COUNT(DISTINCT u) " +
            "FROM User u JOIN u.roles r WHERE u.isActive = true " +
            "GROUP BY r.name ORDER BY COUNT(DISTINCT u) DESC")
    List<Object[]> getRoleDistributionStats();
    // ✅ NEW FEATURE: Role distribution for analytics / 새로운 기능: 분석을 위한 역할 분포

    // Get daily user activity for the last N days / 지난 N일간의 일일 사용자 활동 가져오기
    @Query("SELECT DATE(u.lastLogin), COUNT(DISTINCT u) " +
            "FROM User u WHERE u.lastLogin >= :startDate AND u.isActive = true " +
            "GROUP BY DATE(u.lastLogin) ORDER BY DATE(u.lastLogin)")
    List<Object[]> getDailyActiveUsers(@Param("startDate") LocalDateTime startDate);
    // ✅ NEW FEATURE: Daily active user analytics / 새로운 기능: 일일 활성 사용자 분석

    // ===== HEALTH CHECK OPERATIONS ===== / ===== 상태 확인 작업 =====

    // Check if user repository is healthy / 사용자 리포지토리가 건강한지 확인
    @Query("SELECT COUNT(u) > 0 FROM User u")
    boolean isRepositoryHealthy();
    // ✅ NEW FEATURE: Repository health check / 새로운 기능: 리포지토리 상태 확인

    // Get oldest user account / 가장 오래된 사용자 계정 가져오기
    Optional<User> findTopByOrderByCreatedAtAsc();
    // ✅ NEW FEATURE: Find oldest user account / 새로운 기능: 가장 오래된 사용자 계정 찾기

    // Get newest user account / 가장 새로운 사용자 계정 가져오기
    Optional<User> findTopByOrderByCreatedAtDesc();
    // ✅ NEW FEATURE: Find newest user account / 새로운 기능: 가장 새로운 사용자 계정 찾기

    // Get users with the most roles / 가장 많은 역할을 가진 사용자 가져오기
    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY SIZE(u.roles) DESC")
    Page<User> findUsersWithMostRoles(Pageable pageable);
    // ✅ NEW FEATURE: Users with most roles for analysis / 새로운 기능: 분석을 위한 가장 많은 역할을 가진 사용자

    // ===== CUSTOM FINDER METHODS ===== / ===== 커스텀 찾기 메서드 =====

    // Find users by email domain / 이메일 도메인으로 사용자 찾기
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%@', :domain)) AND u.isActive = true")
    Page<User> findByEmailDomain(@Param("domain") String domain, Pageable pageable);
    // ✅ NEW FEATURE: Find users by email domain / 새로운 기능: 이메일 도메인으로 사용자 찾기

    // Find users who joined in a specific month/year / 특정 월/년에 가입한 사용자 찾기
    @Query("SELECT u FROM User u WHERE YEAR(u.createdAt) = :year AND MONTH(u.createdAt) = :month")
    Page<User> findByJoinMonth(@Param("year") int year, @Param("month") int month, Pageable pageable);
    // ✅ NEW FEATURE: Find users by join month / 새로운 기능: 가입 월로 사용자 찾기

    // Find users with complete profiles / 완전한 프로필을 가진 사용자 찾기
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.isEmailVerified = true AND " +
            "u.firstName IS NOT NULL AND u.lastName IS NOT NULL AND u.bio IS NOT NULL")
    Page<User> findUsersWithCompleteProfiles(Pageable pageable);
    // ✅ NEW FEATURE: Users with complete profiles / 새로운 기능: 완전한 프로필을 가진 사용자

    // Find users missing profile information / 프로필 정보가 누락된 사용자 찾기
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
            "(u.firstName IS NULL OR u.lastName IS NULL OR u.bio IS NULL)")
    Page<User> findUsersWithIncompleteProfiles(Pageable pageable);
    // ✅ NEW FEATURE: Users with incomplete profiles / 새로운 기능: 불완전한 프로필을 가진 사용자

    // ===== PAGINATION HELPERS ===== / ===== 페이징 도우미 =====

    // Find all users with flexible pagination / 유연한 페이징으로 모든 사용자 찾기
    @Query("SELECT u FROM User u WHERE " +
            "(:includeInactive = true OR u.isActive = true) AND " +
            "(:includeUnverified = true OR u.isEmailVerified = true)")
    Page<User> findAllWithFilters(@Param("includeInactive") boolean includeInactive,
                                  @Param("includeUnverified") boolean includeUnverified,
                                  Pageable pageable);
    // ✅ NEW FEATURE: Flexible user filtering / 새로운 기능: 유연한 사용자 필터링

    // Count users with flexible filters / 유연한 필터로 사용자 수 계산
    @Query("SELECT COUNT(u) FROM User u WHERE " +
            "(:includeInactive = true OR u.isActive = true) AND " +
            "(:includeUnverified = true OR u.isEmailVerified = true)")
    long countWithFilters(@Param("includeInactive") boolean includeInactive,
                          @Param("includeUnverified") boolean includeUnverified);
    // ✅ NEW FEATURE: Flexible user counting / 새로운 기능: 유연한 사용자 카운팅
}