// Updated UserService.java for improved User entity / 개선된 User 엔티티를 위한 업데이트된 UserService.java

package com.onion.backend.service;

// DTO classes for request and response data / 요청 및 응답 데이터를 위한 DTO 클래스들
import com.onion.backend.dto.*;
// Entity classes / 엔티티 클래스들
import com.onion.backend.entity.User;
import com.onion.backend.entity.Role;
// Repository interfaces / 리포지토리 인터페이스들
import com.onion.backend.repository.UserRepository;
import com.onion.backend.repository.RoleRepository;
// Lombok annotation for constructor generation / 생성자 생성을 위한 Lombok 어노테이션
import lombok.RequiredArgsConstructor;
// Lombok annotation for logging / 로깅을 위한 Lombok 어노테이션
import lombok.extern.slf4j.Slf4j;
// Spring pagination support / Spring 페이징 지원
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// Spring Security password encoding / Spring Security 비밀번호 인코딩
import org.springframework.security.crypto.password.PasswordEncoder;
// Spring stereotype annotation / Spring 스테레오타입 어노테이션
import org.springframework.stereotype.Service;
// Spring transaction annotation / Spring 트랜잭션 어노테이션
import org.springframework.transaction.annotation.Transactional;

// Java 8+ date/time API / Java 8+ 날짜/시간 API
import java.time.LocalDateTime;

@Service // Marks this class as a service layer component / 이 클래스를 서비스 계층 컴포넌트로 표시
@RequiredArgsConstructor // Lombok: generates constructor with final fields / Lombok: final 필드로 생성자 생성
@Slf4j // Lombok: generates logger field / Lombok: 로거 필드 생성
@Transactional // All methods run in transaction context / 모든 메서드가 트랜잭션 컨텍스트에서 실행
public class UserService {

    // Repository dependencies / 리포지토리 의존성
    private final UserRepository userRepository; // Repository for user data operations / 사용자 데이터 작업을 위한 리포지토리
    private final RoleRepository roleRepository; // Repository for role data operations / 역할 데이터 작업을 위한 리포지토리
    private final PasswordEncoder passwordEncoder; // Service for password encoding / 비밀번호 인코딩을 위한 서비스

    // Create new user account / 새 사용자 계정 생성
    public UserResponseDTO createUser(SignUpRequestDTO signUpRequest) {
        log.info("Creating user with username: {}", signUpRequest.getUsername());

        // Validate unique constraints / 고유 제약조건 검증
        validateUserUniqueness(signUpRequest.getUsername(), signUpRequest.getEmail());

        // Get default user role / 기본 사용자 역할 가져오기
        Role userRole = roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new RuntimeException("Default USER role not found"));

        // Build user entity / 사용자 엔티티 빌드
        User user = User.builder()
                .username(signUpRequest.getUsername()) // Set username / 사용자명 설정
                .password(passwordEncoder.encode(signUpRequest.getPassword())) // Encode password / 비밀번호 인코딩
                .email(signUpRequest.getEmail()) // Set email / 이메일 설정
                .firstName(signUpRequest.getFirstName()) // Set first name if provided / 제공된 경우 이름 설정
                .lastName(signUpRequest.getLastName()) // Set last name if provided / 제공된 경우 성 설정
                .isActive(true) // Account active by default / 기본적으로 계정 활성화
                .isEmailVerified(false) // Email needs verification / 이메일 검증 필요
                .build();

        // Add default role / 기본 역할 추가
        user.addRole(userRole);

        // Save user and return DTO / 사용자 저장 및 DTO 반환
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        return convertToUserResponseDTO(savedUser);
    }

    // Get paginated list of users / 페이징된 사용자 목록 가져오기
    @Transactional(readOnly = true) // Read-only transaction for performance / 성능을 위한 읽기 전용 트랜잭션
    public Page<UserResponseDTO> getUsers(Pageable pageable) {
        log.debug("Retrieving users with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        return userRepository.findAll(pageable) // Get paginated users / 페이징된 사용자 가져오기
                .map(this::convertToUserResponseDTO); // Convert to DTO / DTO로 변환
    }

    // Delete user by ID / ID로 사용자 삭제
    public void deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Soft delete by deactivating instead of hard delete / 하드 삭제 대신 비활성화를 통한 소프트 삭제
        user.deactivate();
        userRepository.save(user);

        log.info("User with ID {} deactivated successfully", userId);
    }

    // Get user profile by username / 사용자명으로 사용자 프로필 가져오기
    @Transactional(readOnly = true) // Read-only transaction / 읽기 전용 트랜잭션
    public UserProfileDTO getUserProfile(String username) {
        log.debug("Retrieving profile for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return convertToUserProfileDTO(user);
    }

    // Update user profile / 사용자 프로필 업데이트
    public UserProfileDTO updateUserProfile(String username, UpdateProfileRequestDTO updateRequest) {
        log.info("Updating profile for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Update fields if provided / 제공된 경우 필드 업데이트
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            validateEmailUniqueness(updateRequest.getEmail(), user.getId());
            user.setEmail(updateRequest.getEmail());
            user.setIsEmailVerified(false); // Reset email verification / 이메일 검증 재설정
        }

        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }

        if (updateRequest.getBio() != null) {
            user.setBio(updateRequest.getBio());
        }

        User savedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", username);

        return convertToUserProfileDTO(savedUser);
    }

    // Change user password / 사용자 비밀번호 변경
    public void changePassword(String username, PasswordChangeRequestDTO passwordChangeRequest) {
        log.info("Changing password for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Verify current password / 현재 비밀번호 검증
        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid current password");
        }

        // Update password / 비밀번호 업데이트
        user.updatePassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", username);
    }

    // Update last login timestamp / 마지막 로그인 타임스탬프 업데이트
    public void updateLastLogin(String username) {
        log.debug("Updating last login for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        user.updateLastLogin();
        userRepository.save(user);
    }

    // Private helper methods / 비공개 도우미 메서드

    // Validate username and email uniqueness / 사용자명과 이메일 고유성 검증
    private void validateUserUniqueness(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }
    }

    // Validate email uniqueness for profile update / 프로필 업데이트를 위한 이메일 고유성 검증
    private void validateEmailUniqueness(String email, Long excludeUserId) {
        if (userRepository.existsByEmailAndIdNot(email, excludeUserId)) {
            throw new RuntimeException("Email already exists: " + email);
        }
    }

    // Convert User entity to UserResponseDTO / User 엔티티를 UserResponseDTO로 변환
    private UserResponseDTO convertToUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdDate(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .isActive(user.getIsActive())
                .build();
    }

    // Convert User entity to UserProfileDTO / User 엔티티를 UserProfileDTO로 변환
    private UserProfileDTO convertToUserProfileDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .createdDate(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .updatedDate(user.getUpdatedAt())
                .build();
    }
}

// Updated CustomUserDetailsService.java for improved User entity / 개선된 User 엔티티를 위한 업데이트된 CustomUserDetailsService.java

@Service // Marks this class as a service layer component / 이 클래스를 서비스 계층 컴포넌트로 표시
@RequiredArgsConstructor // Lombok: generates constructor with final fields / Lombok: final 필드로 생성자 생성
@Slf4j // Lombok: generates logger field / Lombok: 로거 필드 생성
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; // Repository for user data operations / 사용자 데이터 작업을 위한 리포지토리

    @Override // Overrides UserDetailsService method / UserDetailsService 메서드 오버라이드
    @Transactional(readOnly = true) // Read-only transaction / 읽기 전용 트랜잭션
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsernameAndIsActiveTrue(username) // Find active user only / 활성 사용자만 찾기
                .orElseThrow(() -> new UsernameNotFoundException("User not found or inactive: " + username));

        // Convert user roles to Spring Security authorities / 사용자 역할을 Spring Security 권한으로 변환
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .filter(Role::getIsActive) // Only include active roles / 활성 역할만 포함
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())) // Add ROLE_ prefix / ROLE_ 접두사 추가
                .collect(Collectors.toSet());

        log.debug("User {} loaded with roles: {}", username,
                authorities.stream().map(SimpleGrantedAuthority::getAuthority).collect(Collectors.toList()));

        // Return Spring Security User implementation / Spring Security User 구현체 반환
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), // Username for authentication / 인증용 사용자명
                user.getPassword(), // Encoded password / 인코딩된 비밀번호
                user.isEnabled(), // Account enabled status / 계정 활성화 상태
                user.isAccountNonExpired(), // Account non-expired status / 계정 만료되지 않음 상태
                user.isCredentialsNonExpired(), // Credentials non-expired status / 자격증명 만료되지 않음 상태
                user.isAccountNonLocked(), // Account non-locked status / 계정 잠기지 않음 상태
                authorities // User authorities / 사용자 권한
        );
    }
}