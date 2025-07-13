package com.onion.backend.service;

// User and Role entity classes / User와 Role 엔티티 클래스
import com.onion.backend.entity.User;
import com.onion.backend.entity.Role;
// Repository interface for User entity database operations / User 엔티티 데이터베이스 작업을 위한 리포지토리 인터페이스
import com.onion.backend.repository.UserRepository;
// Lombok annotations for code generation / 코드 생성을 위한 Lombok 어노테이션
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Spring Security interfaces and classes / Spring Security 인터페이스 및 클래스
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// Spring annotations / Spring 어노테이션
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// Spring utility for string operations / 문자열 작업을 위한 Spring 유틸리티
import org.springframework.util.StringUtils;

// Java collections and streams / Java 컬렉션 및 스트림
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service // Marks this class as a service layer component / 이 클래스를 서비스 계층 컴포넌트로 표시
@RequiredArgsConstructor // Lombok: generates constructor with final fields / Lombok: final 필드로 생성자 생성
@Slf4j // Lombok: generates logger field for logging / Lombok: 로깅을 위한 로거 필드 생성
public class CustomUserDetailsService implements UserDetailsService {
    // Implements Spring Security's UserDetailsService interface / Spring Security의 UserDetailsService 인터페이스 구현

    // Repository for accessing user data from database / 데이터베이스에서 사용자 데이터 접근을 위한 리포지토리
    private final UserRepository userRepository;
    // Service for updating user activity / 사용자 활동 업데이트를 위한 서비스
    private final UserService userService;

    @Override // Overrides the method from UserDetailsService interface / UserDetailsService 인터페이스의 메서드 오버라이드
    @Transactional(readOnly = true) // Read-only transaction for performance / 성능을 위한 읽기 전용 트랜잭션
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Validate input parameter / 입력 매개변수 검증
        if (!StringUtils.hasText(username)) {
            log.warn("Authentication attempt with empty username");
            throw new UsernameNotFoundException("Username cannot be empty");
        }

        log.debug("Loading user by username: {}", username);

        // Find active user by username (supports case-insensitive lookup) / 사용자명으로 활성 사용자 찾기 (대소문자 구분 없는 조회 지원)
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseGet(() -> {
                    // Fallback to case-insensitive search if exact match not found / 정확한 일치를 찾지 못한 경우 대소문자 구분 없는 검색으로 대체
                    log.debug("Exact username match not found, trying case-insensitive search for: {}", username);
                    return userRepository.findByUsernameIgnoreCase(username)
                            .filter(User::getIsActive) // Ensure user is active / 사용자가 활성 상태인지 확인
                            .orElse(null);
                })
                .orElseThrow(() -> {
                    log.warn("Authentication failed: User not found or inactive with username: {}", username);
                    return new UsernameNotFoundException("User not found or inactive: " + username);
                });
        // ✅ IMPROVEMENT: Only loads active users and supports case-insensitive lookup / 개선: 활성 사용자만 로드하고 대소문자 구분 없는 조회 지원

        // Update last login time asynchronously / 비동기적으로 마지막 로그인 시간 업데이트
        updateLastLoginAsync(user.getUsername());

        // Convert user roles to Spring Security authorities / 사용자 역할을 Spring Security 권한으로 변환
        Set<GrantedAuthority> authorities = mapRolesToAuthorities(user.getRoles());
        // ✅ IMPROVEMENT: Proper role-based authority mapping / 개선: 적절한 역할 기반 권한 매핑

        log.debug("User {} loaded successfully with {} authorities: {}",
                username, authorities.size(),
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

        // Create and return UserDetails with comprehensive account status checks / 포괄적인 계정 상태 확인과 함께 UserDetails 생성 및 반환
        return new CustomUserPrincipal(
                user.getUsername(), // Username for authentication / 인증용 사용자명
                user.getPassword(), // Encoded password / 인코딩된 비밀번호
                authorities, // User authorities based on roles / 역할 기반 사용자 권한
                user.isEnabled(), // Account enabled status / 계정 활성화 상태
                user.isAccountNonExpired(), // Account expiration status / 계정 만료 상태
                user.isCredentialsNonExpired(), // Credentials expiration status / 자격증명 만료 상태
                user.isAccountNonLocked(), // Account lock status / 계정 잠금 상태
                user // Store original user entity for additional information / 추가 정보를 위한 원본 사용자 엔티티 저장
        );
        // ✅ IMPROVEMENT: Comprehensive account status checks / 개선: 포괄적인 계정 상태 확인
    }

    /**
     * Maps user roles to Spring Security authorities
     * 사용자 역할을 Spring Security 권한에 매핑
     */
    private Set<GrantedAuthority> mapRolesToAuthorities(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            log.debug("User has no roles assigned, granting default USER authority");
            // Grant default USER authority if no roles assigned / 역할이 할당되지 않은 경우 기본 USER 권한 부여
            return Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        Set<GrantedAuthority> authorities = roles.stream()
                .filter(Role::getIsActive) // Only include active roles / 활성 역할만 포함
                .map(role -> {
                    String authority = "ROLE_" + role.getName().toUpperCase(); // Add ROLE_ prefix and normalize / ROLE_ 접두사 추가 및 정규화
                    log.trace("Mapping role {} to authority {}", role.getName(), authority);
                    return new SimpleGrantedAuthority(authority);
                })
                .collect(Collectors.toSet());

        // Ensure at least one authority is granted / 최소 한 개의 권한이 부여되도록 보장
        if (authorities.isEmpty()) {
            log.warn("All user roles are inactive, granting default USER authority");
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }

    /**
     * Updates user's last login time asynchronously
     * 사용자의 마지막 로그인 시간을 비동기적으로 업데이트
     */
    private void updateLastLoginAsync(String username) {
        try {
            // Update last login time in separate transaction / 별도 트랜잭션에서 마지막 로그인 시간 업데이트
            userService.updateLastLogin(username);
            log.debug("Last login updated for user: {}", username);
        } catch (Exception e) {
            // Don't fail authentication if login time update fails / 로그인 시간 업데이트 실패 시 인증을 실패시키지 않음
            log.warn("Failed to update last login time for user: {}", username, e);
        }
    }

    /**
     * Custom UserDetails implementation with additional user information
     * 추가 사용자 정보가 포함된 커스텀 UserDetails 구현
     */
    public static class CustomUserPrincipal implements UserDetails {
        private final String username; // Username / 사용자명
        private final String password; // Encoded password / 인코딩된 비밀번호
        private final Collection<? extends GrantedAuthority> authorities; // User authorities / 사용자 권한
        private final boolean enabled; // Account enabled status / 계정 활성화 상태
        private final boolean accountNonExpired; // Account expiration status / 계정 만료 상태
        private final boolean credentialsNonExpired; // Credentials expiration status / 자격증명 만료 상태
        private final boolean accountNonLocked; // Account lock status / 계정 잠금 상태
        private final User user; // Original user entity / 원본 사용자 엔티티

        // Constructor for CustomUserPrincipal / CustomUserPrincipal 생성자
        public CustomUserPrincipal(String username, String password,
                                   Collection<? extends GrantedAuthority> authorities,
                                   boolean enabled, boolean accountNonExpired,
                                   boolean credentialsNonExpired, boolean accountNonLocked,
                                   User user) {
            this.username = username;
            this.password = password;
            this.authorities = authorities;
            this.enabled = enabled;
            this.accountNonExpired = accountNonExpired;
            this.credentialsNonExpired = credentialsNonExpired;
            this.accountNonLocked = accountNonLocked;
            this.user = user;
        }

        // UserDetails interface implementation / UserDetails 인터페이스 구현
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return accountNonExpired;
        }

        @Override
        public boolean isAccountNonLocked() {
            return accountNonLocked;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return credentialsNonExpired;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        // Additional methods for accessing user information / 사용자 정보 접근을 위한 추가 메서드
        public User getUser() {
            return user;
        }

        public Long getUserId() {
            return user.getId();
        }

        public String getEmail() {
            return user.getEmail();
        }

        public String getFullName() {
            return user.getFullName();
        }

        public boolean isEmailVerified() {
            return user.getIsEmailVerified();
        }

        // Check if user has specific role / 사용자가 특정 역할을 가지고 있는지 확인
        public boolean hasRole(String roleName) {
            String authority = "ROLE_" + roleName.toUpperCase();
            return authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals(authority));
        }

        // Check if user has any of the specified roles / 사용자가 지정된 역할 중 하나라도 가지고 있는지 확인
        public boolean hasAnyRole(String... roleNames) {
            for (String roleName : roleNames) {
                if (hasRole(roleName)) {
                    return true;
                }
            }
            return false;
        }

        // Get all role names without ROLE_ prefix / ROLE_ 접두사 없이 모든 역할 이름 가져오기
        public Set<String> getRoleNames() {
            return authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5)) // Remove ROLE_ prefix / ROLE_ 접두사 제거
                    .collect(Collectors.toSet());
        }

        @Override
        public String toString() {
            return "CustomUserPrincipal{" +
                    "username='" + username + '\'' +
                    ", authorities=" + authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()) +
                    ", enabled=" + enabled +
                    ", accountNonExpired=" + accountNonExpired +
                    ", credentialsNonExpired=" + credentialsNonExpired +
                    ", accountNonLocked=" + accountNonLocked +
                    '}';
        }
    }

    /**
     * Utility method to get current authenticated user
     * 현재 인증된 사용자를 가져오는 유틸리티 메서드
     */
    public static CustomUserPrincipal getCurrentUser() {
        try {
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
                return (CustomUserPrincipal) authentication.getPrincipal();
            }
        } catch (Exception e) {
            log.debug("Failed to get current user from security context", e);
        }
        return null;
    }

    /**
     * Utility method to get current user ID
     * 현재 사용자 ID를 가져오는 유틸리티 메서드
     */
    public static Long getCurrentUserId() {
        CustomUserPrincipal currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUserId() : null;
    }

    /**
     * Utility method to check if current user has specific role
     * 현재 사용자가 특정 역할을 가지고 있는지 확인하는 유틸리티 메서드
     */
    public static boolean currentUserHasRole(String roleName) {
        CustomUserPrincipal currentUser = getCurrentUser();
        return currentUser != null && currentUser.hasRole(roleName);
    }
}