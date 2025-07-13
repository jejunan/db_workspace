package com.onion.backend.entity;

// JPA related annotations for entity mapping / 엔티티 매핑을 위한 JPA 관련 어노테이션
import jakarta.persistence.*;
// Jakarta validation annotations / Jakarta 검증 어노테이션
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
// Lombok annotations for code generation / 코드 생성을 위한 Lombok 어노테이션
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.ToString;
import lombok.EqualsAndHashCode;
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
@Table(name = "roles", // Specifies table name / 테이블 이름 지정
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_role_name", columnNames = "name") // Unique constraint for role name / 역할 이름 고유 제약조건
        })
@EntityListeners(AuditingEntityListener.class) // Enables JPA auditing for automatic field management / 자동 필드 관리를 위한 JPA 감사 활성화
@Getter // Lombok: generates getter methods for all fields / Lombok: 모든 필드에 대한 getter 메서드 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: generates protected no-args constructor for JPA / Lombok: JPA를 위한 protected 기본 생성자 생성
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Lombok: generates private all-args constructor for builder / Lombok: 빌더를 위한 private 전체 인수 생성자 생성
@Builder // Lombok: generates builder pattern implementation / Lombok: 빌더 패턴 구현 생성
@ToString(exclude = "users") // Lombok: generates toString excluding users to avoid circular reference / Lombok: 순환 참조를 피하기 위해 users를 제외한 toString 생성
@EqualsAndHashCode(of = "name") // Lombok: generates equals and hashCode based on name field / Lombok: name 필드를 기반으로 equals와 hashCode 생성
public class Role {

    @Id // Marks this field as the primary key / 이 필드를 기본 키로 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment strategy / 자동 증가 전략
    private Long id; // Unique role identifier / 역할 고유 식별자

    @NotBlank(message = "Role name is required") // Bean validation for non-blank role name / 비어있지 않은 역할 이름을 위한 Bean 검증
    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters") // Length validation / 길이 검증
    @Column(nullable = false, unique = true, length = 50) // Database constraints / 데이터베이스 제약조건
    private String name; // Role name (e.g., "ADMIN", "USER", "MODERATOR") / 역할 이름 (예: "ADMIN", "USER", "MODERATOR")

    @Size(max = 255, message = "Description cannot exceed 255 characters") // Length validation / 길이 검증
    @Column(length = 255) // Database column with length constraint / 길이 제약이 있는 데이터베이스 컬럼
    private String description; // Role description / 역할 설명

    @Column(name = "is_active", nullable = false) // Database column for role status / 역할 상태를 위한 데이터베이스 컬럼
    @Builder.Default // Lombok: sets default value for builder / Lombok: 빌더를 위한 기본값 설정
    private Boolean isActive = true; // Role activation status / 역할 활성화 상태

    @CreatedDate // Automatically sets creation timestamp / 생성 타임스탬프 자동 설정
    @Column(name = "created_at", nullable = false, updatable = false) // Immutable creation timestamp / 불변 생성 타임스탬프
    private LocalDateTime createdAt; // When the role was created / 역할이 생성된 시간

    @LastModifiedDate // Automatically updates modification timestamp / 수정 타임스탬프 자동 갱신
    @Column(name = "updated_at") // Database column for update timestamp / 업데이트 타임스탬프를 위한 데이터베이스 컬럼
    private LocalDateTime updatedAt; // When the role was last modified / 역할이 마지막으로 수정된 시간

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY) // Bidirectional relationship mapped by User.roles / User.roles에 의해 매핑된 양방향 관계
    @Builder.Default // Lombok: sets default value for builder / Lombok: 빌더를 위한 기본값 설정
    private Set<User> users = new HashSet<>(); // Users assigned to this role / 이 역할에 할당된 사용자들

    // Predefined role constants for common roles / 일반적인 역할을 위한 미리 정의된 역할 상수
    public static final String ADMIN = "ADMIN"; // Administrator role with full access / 전체 접근 권한을 가진 관리자 역할
    public static final String USER = "USER"; // Standard user role / 표준 사용자 역할
    public static final String MODERATOR = "MODERATOR"; // Moderator role with limited admin access / 제한된 관리자 접근 권한을 가진 중재자 역할
    public static final String GUEST = "GUEST"; // Guest role with minimal access / 최소 접근 권한을 가진 게스트 역할

    // Static factory methods for creating common roles / 일반적인 역할 생성을 위한 정적 팩토리 메서드

    // Create admin role / 관리자 역할 생성
    public static Role createAdminRole() {
        return Role.builder() // Uses builder pattern for object creation / 객체 생성을 위한 빌더 패턴 사용
                .name(ADMIN) // Set role name to ADMIN / 역할 이름을 ADMIN으로 설정
                .description("Administrator with full system access") // Set role description / 역할 설명 설정
                .isActive(true) // Set role as active / 역할을 활성으로 설정
                .build(); // Build the role object / 역할 객체 빌드
    }

    // Create standard user role / 표준 사용자 역할 생성
    public static Role createUserRole() {
        return Role.builder() // Uses builder pattern for object creation / 객체 생성을 위한 빌더 패턴 사용
                .name(USER) // Set role name to USER / 역할 이름을 USER로 설정
                .description("Standard user with basic access") // Set role description / 역할 설명 설정
                .isActive(true) // Set role as active / 역할을 활성으로 설정
                .build(); // Build the role object / 역할 객체 빌드
    }

    // Create moderator role / 중재자 역할 생성
    public static Role createModeratorRole() {
        return Role.builder() // Uses builder pattern for object creation / 객체 생성을 위한 빌더 패턴 사용
                .name(MODERATOR) // Set role name to MODERATOR / 역할 이름을 MODERATOR로 설정
                .description("Moderator with content management access") // Set role description / 역할 설명 설정
                .isActive(true) // Set role as active / 역할을 활성으로 설정
                .build(); // Build the role object / 역할 객체 빌드
    }

    // Business methods for role management / 역할 관리를 위한 비즈니스 메서드

    // Activate role / 역할 활성화
    public void activate() {
        this.isActive = true; // Set role as active / 역할을 활성으로 설정
    }

    // Deactivate role / 역할 비활성화
    public void deactivate() {
        this.isActive = false; // Set role as inactive / 역할을 비활성으로 설정
    }

    // Update role description / 역할 설명 업데이트
    public void updateDescription(String newDescription) {
        this.description = newDescription; // Set new description / 새로운 설명 설정
    }

    // Check if role is system role (cannot be deleted) / 시스템 역할인지 확인 (삭제 불가)
    public boolean isSystemRole() {
        return ADMIN.equals(this.name) || USER.equals(this.name); // Check if role is admin or user / 역할이 관리자 또는 사용자인지 확인
    }

    // Get number of users assigned to this role / 이 역할에 할당된 사용자 수 가져오기
    public int getUserCount() {
        return users != null ? users.size() : 0; // Return user count or 0 if null / 사용자 수 반환 또는 null인 경우 0
    }

    // Add user to role / 역할에 사용자 추가
    public void addUser(User user) {
        if (this.users == null) { // Initialize if null / null인 경우 초기화
            this.users = new HashSet<>();
        }
        this.users.add(user); // Add user to role's user set / 역할의 사용자 세트에 사용자 추가
        user.addRole(this); // Add role to user's role set / 사용자의 역할 세트에 역할 추가
    }

    // Remove user from role / 역할에서 사용자 제거
    public void removeUser(User user) {
        if (this.users != null) { // Check if users set exists / 사용자 세트가 존재하는지 확인
            this.users.remove(user); // Remove user from role's user set / 역할의 사용자 세트에서 사용자 제거
            user.removeRole(this); // Remove role from user's role set / 사용자의 역할 세트에서 역할 제거
        }
    }

    // Check if role has any users assigned / 역할에 할당된 사용자가 있는지 확인
    public boolean hasUsers() {
        return users != null && !users.isEmpty(); // Return true if users exist / 사용자가 존재하면 true 반환
    }

    // Get formatted role name for display / 표시를 위한 형식화된 역할 이름 가져오기
    public String getDisplayName() {
        if (name == null) return "Unknown Role"; // Handle null name / null 이름 처리

        // Convert from UPPER_CASE to Title Case / UPPER_CASE에서 Title Case로 변환
        return name.substring(0, 1).toUpperCase() + // First character uppercase / 첫 번째 문자 대문자
                name.substring(1).toLowerCase(); // Rest lowercase / 나머지 소문자
    }

    // Validate role before saving / 저장 전 역할 검증
    @PrePersist // Executes before entity is persisted to database / 엔티티가 데이터베이스에 저장되기 전 실행
    @PreUpdate // Executes before entity is updated in database / 엔티티가 데이터베이스에서 업데이트되기 전 실행
    private void validateRole() {
        if (name != null) { // Check if name exists / 이름이 존재하는지 확인
            this.name = name.trim().toUpperCase(); // Normalize role name to uppercase / 역할 이름을 대문자로 정규화
        }

        if (description != null) { // Check if description exists / 설명이 존재하는지 확인
            this.description = description.trim(); // Trim whitespace from description / 설명에서 공백 제거
        }
    }
}