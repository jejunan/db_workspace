package com.onion.backend.entity;

// JPA related annotations for entity mapping / 엔티티 매핑을 위한 JPA 관련 어노테이션
import jakarta.persistence.*;
// Lombok annotations for code generation / 코드 생성을 위한 Lombok 어노테이션
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
// Spring Data annotation for automatic creation date management / 생성일 자동 관리를 위한 Spring Data 어노테이션
import org.springframework.data.annotation.CreatedDate;
// Spring Data JPA auditing listener / Spring Data JPA 감사 리스너
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Java 8+ date/time API / Java 8+ 날짜/시간 API
import java.time.LocalDateTime;
// Java security for message digest (hashing) / 메시지 다이제스트(해싱)를 위한 Java 보안
import java.security.MessageDigest;
// Java utility for Base64 encoding / Base64 인코딩을 위한 Java 유틸리티
import java.util.Base64;

@Entity // Marks this class as a JPA entity (maps to database table) / 이 클래스가 JPA 엔티티임을 표시 (데이터베이스 테이블에 매핑)
@Table(name = "jwt_blacklist", // Specifies table name / 테이블 이름 지정
        indexes = {
                @Index(name = "idx_token_hash", columnList = "tokenHash"), // Index for fast token lookup / 빠른 토큰 조회를 위한 인덱스
                @Index(name = "idx_username", columnList = "username"), // Index for user-based queries / 사용자 기반 쿼리를 위한 인덱스
                @Index(name = "idx_expiration_time", columnList = "expirationTime") // Index for cleanup queries / 정리 쿼리를 위한 인덱스
        })
@EntityListeners(AuditingEntityListener.class) // Enables JPA auditing for automatic field management / 자동 필드 관리를 위한 JPA 감사 활성화
@Getter // Lombok: generates getter methods for all fields / Lombok: 모든 필드에 대한 getter 메서드 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: generates protected no-args constructor for JPA / Lombok: JPA를 위한 protected 기본 생성자 생성
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Lombok: generates private all-args constructor for builder / Lombok: 빌더를 위한 private 전체 인수 생성자 생성
@Builder // Lombok: generates builder pattern implementation / Lombok: 빌더 패턴 구현 생성
public class JwtBlacklist {

    @Id // Marks this field as the primary key / 이 필드를 기본 키로 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment strategy / 자동 증가 전략
    private Long id; // Unique identifier for blacklist entry / 블랙리스트 항목의 고유 식별자

    @Column(name = "token_hash", nullable = false, unique = true, length = 64) // Stores hashed token instead of full token / 전체 토큰 대신 해시된 토큰 저장
    // ✅ IMPROVEMENT: Using hash instead of full token for better performance and security / 개선: 더 나은 성능과 보안을 위해 전체 토큰 대신 해시 사용
    private String tokenHash; // SHA-256 hash of the JWT token / JWT 토큰의 SHA-256 해시

    @Column(nullable = false) // Database column that cannot be null / null이 될 수 없는 데이터베이스 컬럼
    private LocalDateTime expirationTime; // When the token naturally expires / 토큰이 자연적으로 만료되는 시간

    @Column(nullable = false, length = 50) // Database column with length constraint / 길이 제약이 있는 데이터베이스 컬럼
    private String username; // Username associated with the blacklisted token / 블랙리스트된 토큰과 연관된 사용자명

    @CreatedDate // Automatically sets creation timestamp / 생성 타임스탬프 자동 설정
    @Column(name = "created_at", nullable = false, updatable = false) // Immutable creation timestamp / 불변 생성 타임스탬프
    private LocalDateTime createdAt; // When the blacklist entry was created / 블랙리스트 항목이 생성된 시간

    @Column(length = 255) // Optional reason for blacklisting / 블랙리스트 사유 (선택사항)
    private String reason; // Reason for blacklisting (e.g., "logout", "password_change", "security_breach") / 블랙리스트 사유 (예: "logout", "password_change", "security_breach")

    // Static factory method for creating blacklist entry from token / 토큰에서 블랙리스트 항목을 생성하는 정적 팩토리 메서드
    public static JwtBlacklist fromToken(String token, String username, LocalDateTime expirationTime, String reason) {
        return JwtBlacklist.builder() // Uses builder pattern for object creation / 객체 생성을 위한 빌더 패턴 사용
                .tokenHash(hashToken(token)) // Hash the token for storage / 저장을 위해 토큰 해시화
                .username(username) // Set username / 사용자명 설정
                .expirationTime(expirationTime) // Set expiration time / 만료 시간 설정
                .reason(reason != null ? reason : "logout") // Set reason with default / 기본값과 함께 사유 설정
                .build(); // Build the object / 객체 빌드
    }

    // Hash token using SHA-256 for secure storage / 안전한 저장을 위해 SHA-256을 사용하여 토큰 해시화
    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Create SHA-256 digest / SHA-256 다이제스트 생성
            byte[] hash = digest.digest(token.getBytes()); // Hash the token bytes / 토큰 바이트 해시화
            return Base64.getEncoder().encodeToString(hash); // Encode hash as Base64 string / 해시를 Base64 문자열로 인코딩
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e); // Handle hashing errors / 해싱 오류 처리
        }
    }

    // Utility method to check if token matches this blacklist entry / 토큰이 이 블랙리스트 항목과 일치하는지 확인하는 유틸리티 메서드
    public boolean matchesToken(String token) {
        return this.tokenHash.equals(hashToken(token)); // Compare hashed tokens / 해시된 토큰 비교
    }

    // Check if this blacklist entry has expired and can be cleaned up / 이 블랙리스트 항목이 만료되어 정리될 수 있는지 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expirationTime); // Compare current time with expiration / 현재 시간과 만료 시간 비교
    }

    // Lifecycle callback to set creation time before persisting / 저장 전 생성 시간을 설정하는 생명주기 콜백
    @PrePersist // Executes before entity is persisted to database / 엔티티가 데이터베이스에 저장되기 전 실행
    protected void onCreate() {
        if (this.createdAt == null) { // Only set if not already set / 아직 설정되지 않은 경우에만 설정
            this.createdAt = LocalDateTime.now(); // Set current timestamp / 현재 타임스탬프 설정
        }
    }

    // Override toString for debugging (excluding sensitive token hash) / 디버깅을 위한 toString 오버라이드 (민감한 토큰 해시 제외)
    @Override
    public String toString() {
        return "JwtBlacklist{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", expirationTime=" + expirationTime +
                ", createdAt=" + createdAt +
                ", reason='" + reason + '\'' +
                '}'; // Excludes token hash for security / 보안을 위해 토큰 해시 제외
    }
}