package com.onion.backend.entity;

// JPA related annotations for entity mapping / 엔티티 매핑을 위한 JPA 관련 어노테이션
import jakarta.persistence.*;
// Jakarta validation annotations / Jakarta 검증 어노테이션
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
// Lombok annotations for code generation / 코드 생성을 위한 Lombok 어노테이션
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.ToString;
// Spring Data annotations for automatic timestamp management / 자동 타임스탬프 관리를 위한 Spring Data 어노테이션
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
// Spring Data JPA auditing listener / Spring Data JPA 감사 리스너
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Java 8+ date/time API / Java 8+ 날짜/시간 API
import java.time.LocalDateTime;
// Java Set for collections / 컬렉션을 위한 Java Set
import java.util.Set;
// Java HashSet implementation / Java HashSet 구현
import java.util.HashSet;

@Entity // Marks this class as a JPA entity (maps to database table) / 이 클래스가 JPA 엔티티임을 표시 (데이터베이스 테이블에 매핑)
@Table(name = "users", // Specifies table name (avoids conflict with reserved keyword 'user') / 테이블 이름 지정 (예약어 'user'와의 충돌 방지)
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_username", columnNames = "username"), // Unique constraint for username / 사용자명 고유 제약조건
                @UniqueConstraint(name = "uk_user_email", columnNames = "email") // Unique constraint for email / 이메일 고유 제약조건
        },
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"), // Index for email-based queries / 이메일 기반 쿼리를 위한 인덱스
                @Index(name = "idx_user_last_login", columnList = "lastLogin"), // Index for activity tracking / 활동 추적을 위한 인덱스
                @Index(name = "idx_user_active", columnList = "isActive") // Index for filtering active users / 활성 사용자 필터링을 위한 인덱스
        })
@EntityListeners(AuditingEntityListener.class) // Enables JPA auditing for automatic field management / 자동 필드 관리를 위한 JPA 감사 활성화
@Getter // Lombok: generates getter methods for all fields / Lombok: 모든 필드에 대한 getter 메서드 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: generates protected no-args constructor for JPA / Lombok: JPA를 위한 protected 기본 생성자 생성
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Lombok: generates private all-args constructor for builder / Lombok: 빌더를 위한 private 전체 인수 생성자 생성
@Builder // Lombok: generates builder pattern implementation / Lombok: 빌더 패턴 구현 생성
@ToString(exclude = {"password", "roles"}) // Lombok: generates toString excluding sensitive fields / Lombok: 민감한 필드를 제외한 toString 생성
public class User {

    @Id // Marks this field as the primary key / 이 필드를 기본 키로 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment strategy / 자동 증가 전략
    private Long id; // Unique user identifier / 사용자 고유 식별자

    @NotBlank(message = "Username is required") // Bean validation for non-blank username / 비어있지 않은 사용자명을 위한 Bean 검증
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") // Length validation / 길이 검증
    @Column(nullable = false, unique = true, length = 50) // Database constraints / 데이터베이스 제약조건
    private String username; // Username for authentication / 인증용 사용자명

    @NotBlank(message = "Password is required") // Bean validation for non-blank password / 비어있지 않은 비밀번호를 위한 Bean 검증
    @Column(nullable = false) // Database column that cannot be null / null이 될 수 없는 데이터베이스 컬럼
    private String password; // Encoded password (never store plain text) / 인코딩된 비밀번호 (평문 저장 금지)

    @NotBlank(message = "Email is required") // Bean validation for non-blank email / 비어있지 않은 이메일을 위한 Bean 검증
    @Email(message = "Invalid email format") // Email format validation / 이메일 형식 검증
    @Column(nullable = false, unique = true, length = 100) // Database constraints with length limit / 길이 제한이 있는 데이터베이스 제약조건
    private String email; // User email address / 사용자 이메일 주소

    @Size(max = 50, message = "First name cannot exceed 50 characters") // Length validation / 길이 검증
    @Column(name = "first_name", length = 50) // Database column with custom name / 커스텀 이름을 가진 데이터베이스 컬럼
    private String firstName; // User's first name / 사용자의 이름

    @Size(max = 50, message = "Last name cannot exceed 50 characters") // Length validation / 길이 검증
    @Column(name = "last_name", length = 50) // Database column with custom name / 커스텀 이름을 가진 데이터베이스 컬럼
    private String lastName; // User's last name / 사용자의 성

    @Size(max = 500, message = "Bio cannot exceed 500 characters") // Length validation / 길이 검증
    @Column(length = 500) // Database column with length constraint / 길이 제약이 있는 데이터베이스 컬럼
    private String bio; // User biography / 사용자 소개

    @Column(name = "is_active", nullable = false) // Database column for account status / 계정 상태를 위한 데이터베이스 컬럼
    @Builder.Default // Lombok: sets default value for builder / Lombok: 빌더를 위한 기본값 설정
    private Boolean isActive = true; // Account activation status / 계정 활성화 상태

    @Column(name = "is_email_verified", nullable = false) // Database column for email verification status / 이메일 검증 상태를 위한 데이터베이스 컬럼
    @Builder.Default // Lombok: sets default value for builder / Lombok: 빌더를 위한 기본값 설정
    private Boolean isEmailVerified = false; // Email verification status / 이메일 검증 상태

    @Column(name = "last_login") // Database column for last login timestamp / 마지막 로그인 타임스탬프를 위한 데이터베이스 컬럼
    private LocalDateTime lastLogin; // Timestamp of last successful login / 마지막 성공적인 로그인의 타임스탬프

    @Column(name = "password_changed_at") // Database column for password change tracking / 비밀번호 변경 추적을 위한 데이터베이스 컬럼
    private LocalDateTime passwordChangedAt; // When password was last changed / 비밀번호가 마지막으로 변경된 시간

    @CreatedDate // Automatically sets creation timestamp / 생성 타임스탬프 자동 설정
    @Column(name = "created_at", nullable = false, updatable = false) // Immutable creation timestamp / 불변 생성 타임스탬프
    private LocalDateTime createdAt; // When the user account was created / 사용자 계정이 생성된 시간

    @LastModifiedDate // Automatically updates modification timestamp / 수정 타임스탬프 자동 갱신
    @Column(name = "updated_at") // Database column for update timestamp / 업데이트 타임스탬프를 위한 데이터베이스 컬럼
    private LocalDateTime updatedAt; // When the user account was last modified / 사용자 계정이 마지막으로 수정된 시간

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH) // Many-to-many relationship with roles / 역할과의 다대다 관계
    @JoinTable(name = "user_roles", // Join table name / 조인 테이블 이름
            joinColumns = @JoinColumn(name = "user_id"), // Foreign key to user / 사용자에 대한 외래 키
            inverseJoinColumns = @JoinColumn(name = "role_id")) // Foreign key to role / 역할에 대한 외래 키
    @Builder.Default // Lombok: sets default value for builder / Lombok: 빌더를 위한 기본값 설정
    private Set<Role> roles = new HashSet<>(); // User roles for authorization / 권한 부여를 위한 사용자 역할

    // Business methods for user management / 사용자 관리를 위한 비즈니스 메서드

    // Update last login timestamp / 마지막 로그인 타임스탬프 업데이트
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now(); // Sets current time as last login / 현재 시간을 마지막 로그인으로 설정
    }

    // Update password and set password change timestamp / 비밀번호 업데이트 및 비밀번호 변경 타임스탬프 설정
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword; // Set new encoded password / 새로운 인코딩된 비밀번호 설정
        this.passwordChangedAt = LocalDateTime.now(); // Set password change timestamp / 비밀번호 변경 타임스탬프 설정
    }

    // Activate user account / 사용자 계정 활성화
    public void activate() {
        this.isActive = true; // Set account as active / 계정을 활성으로 설정
    }

    // Deactivate user account / 사용자 계정 비활성화
    public void deactivate() {
        this.isActive = false; // Set account as inactive / 계정을 비활성으로 설정
    }

    // Verify user email / 사용자 이메일 검증
    public void verifyEmail() {
        this.isEmailVerified = true; // Mark email as verified / 이메일을 검증됨으로 표시
    }

    // Add role to user / 사용자에게 역할 추가
    public void addRole(Role role) {
        this.roles.add(role); // Add role to user's role set / 사용자의 역할 세트에 역할 추가
    }

    // Remove role from user / 사용자에서 역할 제거
    public void removeRole(Role role) {
        this.roles.remove(role); // Remove role from user's role set / 사용자의 역할 세트에서 역할 제거
    }

    // Check if user has specific role / 사용자가 특정 역할을 가지고 있는지 확인
    public boolean hasRole(String roleName) {
        return this.roles.stream() // Stream through user roles / 사용자 역할을 스트림으로 처리
                .anyMatch(role -> role.getName().equals(roleName)); // Check if any role matches / 일치하는 역할이 있는지 확인
    }

    // Get user's full name / 사용자의 전체 이름 가져오기
    public String getFullName() {
        if (firstName != null && lastName != null) { // Check if both names exist / 두 이름이 모두 존재하는지 확인
            return firstName + " " + lastName; // Return combined full name / 결합된 전체 이름 반환
        } else if (firstName != null) { // Only first name exists / 이름만 존재하는 경우
            return firstName; // Return first name only / 이름만 반환
        } else if (lastName != null) { // Only last name exists / 성만 존재하는 경우
            return lastName; // Return last name only / 성만 반환
        }
        return username; // Fallback to username / 사용자명으로 대체
    }

    // Check if user account is enabled and email verified / 사용자 계정이 활성화되고 이메일이 검증되었는지 확인
    public boolean isAccountNonExpired() {
        return isActive; // Account is non-expired if active / 활성 상태이면 계정이 만료되지 않음
    }

    public boolean isAccountNonLocked() {
        return isActive; // Account is non-locked if active / 활성 상태이면 계정이 잠기지 않음
    }

    public boolean isCredentialsNonExpired() {
        return true; // For now, credentials don't expire / 현재로서는 자격증명이 만료되지 않음
    }

    public boolean isEnabled() {
        return isActive && isEmailVerified; // Enabled if both active and email verified / 활성 상태이고 이메일이 검증되면 활성화됨
    }
}