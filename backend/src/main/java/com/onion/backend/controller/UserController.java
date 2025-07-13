package com.onion.backend.controller;

// DTO classes for request and response data / 요청 및 응답 데이터를 위한 DTO 클래스들
import com.onion.backend.dto.*;
// JWT utility class for token operations / JWT 토큰 작업을 위한 유틸리티 클래스
import com.onion.backend.jwt.JwtUtil;
// Service for loading user details from database / 데이터베이스에서 사용자 세부정보 로드를 위한 서비스
import com.onion.backend.service.CustomUserDetailsService;
// Service class containing user business logic / 사용자 비즈니스 로직을 포함하는 서비스 클래스
import com.onion.backend.service.UserService;
// JWT blacklist service for token revocation / 토큰 취소를 위한 JWT 블랙리스트 서비스
import com.onion.backend.service.JwtBlacklistService;
// Swagger annotations for API documentation / API 문서화를 위한 Swagger 어노테이션
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
// HTTP cookie class for session management / 세션 관리를 위한 HTTP 쿠키 클래스
import jakarta.servlet.http.Cookie;
// HTTP servlet response interface / HTTP 서블릿 응답 인터페이스
import jakarta.servlet.http.HttpServletResponse;
// Jakarta validation annotations / Jakarta 검증 어노테이션
import jakarta.validation.Valid;
// Lombok annotation for generating constructor / 생성자 생성을 위한 Lombok 어노테이션
import lombok.RequiredArgsConstructor;
// Lombok annotation for logging / 로깅을 위한 Lombok 어노테이션
import lombok.extern.slf4j.Slf4j;
// Spring pagination support / Spring 페이징 지원
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
// HTTP status codes enumeration / HTTP 상태 코드 열거형
import org.springframework.http.HttpStatus;
// Spring wrapper for HTTP response with status and body / 상태와 본문을 가진 HTTP 응답을 위한 Spring 래퍼
import org.springframework.http.ResponseEntity;
// Authentication manager interface for handling authentication / 인증 처리를 위한 인증 매니저 인터페이스
import org.springframework.security.authentication.AuthenticationManager;
// Authentication token for username/password authentication / 사용자명/비밀번호 인증을 위한 인증 토큰
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Base exception for authentication failures / 인증 실패를 위한 기본 예외
import org.springframework.security.core.AuthenticationException;
// Interface for user details in Spring Security / Spring Security의 사용자 세부정보를 위한 인터페이스
import org.springframework.security.core.userdetails.UserDetails;
// Spring validation annotation / Spring 검증 어노테이션
import org.springframework.validation.annotation.Validated;
// Spring MVC annotations for REST API / REST API를 위한 Spring MVC 어노테이션
import org.springframework.web.bind.annotation.*;

@RestController // Combines @Controller and @ResponseBody - returns JSON responses automatically / @Controller와 @ResponseBody 결합 - JSON 응답 자동 반환
@RequestMapping("/api/users") // Sets base URL path for all endpoints in this controller / 이 컨트롤러의 모든 엔드포인트에 대한 기본 URL 경로 설정
@RequiredArgsConstructor // Lombok: generates constructor with final fields for dependency injection / Lombok: 의존성 주입을 위한 final 필드로 생성자 생성
@Validated // Enables method-level validation / 메서드 수준 검증 활성화
@Slf4j // Lombok: generates logger field for logging / Lombok: 로깅을 위한 로거 필드 생성
@Tag(name = "User Management", description = "APIs for user registration, authentication, and management") // Swagger tag for grouping endpoints / 엔드포인트 그룹화를 위한 Swagger 태그
public class UserController {

    // ✅ IMPROVEMENT: Using constructor injection via @RequiredArgsConstructor / 개선: @RequiredArgsConstructor를 통한 생성자 주입 사용
    private final AuthenticationManager authenticationManager; // Manager for authentication operations / 인증 작업을 위한 매니저
    private final UserService userService; // Service for user business logic / 사용자 비즈니스 로직을 위한 서비스
    private final JwtUtil jwtUtil; // Utility for JWT token operations / JWT 토큰 작업을 위한 유틸리티
    private final CustomUserDetailsService userDetailsService; // Service for loading user details / 사용자 세부정보 로드를 위한 서비스
    private final JwtBlacklistService jwtBlacklistService; // Service for managing token blacklist / 토큰 블랙리스트 관리를 위한 서비스

    @GetMapping("") // Maps HTTP GET requests to /api/users / HTTP GET 요청을 /api/users에 매핑
    @Operation(summary = "Get Users", description = "Retrieve paginated list of users") // Swagger operation documentation / Swagger 작업 문서화
    @SecurityRequirement(name = "bearerAuth") // Requires Bearer token authentication / Bearer 토큰 인증 필요
    @ApiResponse(responseCode = "200", description = "Successfully retrieved users") // Swagger response documentation / Swagger 응답 문서화
    public ResponseEntity<Page<UserResponseDTO>> getUsers(
            @PageableDefault(size = 20, sort = "createdDate") Pageable pageable) { // Pagination with default settings / 기본 설정으로 페이징
        // ✅ IMPROVEMENT: Fixed method name and added pagination / 개선: 메서드명 수정 및 페이징 추가
        log.info("Retrieving users with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        // ✅ IMPROVEMENT: Added logging for monitoring / 개선: 모니터링을 위한 로깅 추가
        Page<UserResponseDTO> users = userService.getUsers(pageable);
        // ✅ IMPROVEMENT: Returns DTO without sensitive data / 개선: 민감한 데이터 없이 DTO 반환
        return ResponseEntity.ok(users);
    }

    @PostMapping("/signUp") // Maps HTTP POST requests to /api/users/signUp / HTTP POST 요청을 /api/users/signUp에 매핑
    @Operation(summary = "User Registration", description = "Create a new user account") // Swagger operation documentation / Swagger 작업 문서화
    @ApiResponse(responseCode = "201", description = "User created successfully") // Swagger response documentation / Swagger 응답 문서화
    @ApiResponse(responseCode = "400", description = "Invalid input data") // Swagger error response documentation / Swagger 오류 응답 문서화
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody SignUpRequestDTO signUpRequest) { // Validates input and uses proper DTO / 입력 검증 및 적절한 DTO 사용
        // ✅ IMPROVEMENT: Added @Valid annotation for input validation / 개선: 입력 검증을 위한 @Valid 어노테이션 추가
        log.info("Creating new user with username: {}", signUpRequest.getUsername());
        UserResponseDTO user = userService.createUser(signUpRequest);
        // ✅ IMPROVEMENT: Returns DTO without password / 개선: 비밀번호 없이 DTO 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
        // ✅ IMPROVEMENT: Returns 201 Created status / 개선: 201 Created 상태 반환
    }

    @DeleteMapping("/{userId}") // Maps HTTP DELETE requests to /api/users/{userId} / HTTP DELETE 요청을 /api/users/{userId}에 매핑
    @Operation(summary = "Delete User", description = "Delete a user by ID") // Swagger operation documentation / Swagger 작업 문서화
    @SecurityRequirement(name = "bearerAuth") // Requires Bearer token authentication / Bearer 토큰 인증 필요
    @ApiResponse(responseCode = "204", description = "User deleted successfully") // Swagger response documentation / Swagger 응답 문서화
    @ApiResponse(responseCode = "404", description = "User not found") // Swagger error response documentation / Swagger 오류 응답 문서화
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to be deleted", required = true) // Swagger parameter documentation / Swagger 매개변수 문서화
            @PathVariable Long userId) {
        log.info("Deleting user with ID: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login") // Maps HTTP POST requests to /api/users/login / HTTP POST 요청을 /api/users/login에 매핑
    @Operation(summary = "User Login", description = "Authenticate user and return JWT token") // Swagger operation documentation / Swagger 작업 문서화
    @ApiResponse(responseCode = "200", description = "Login successful") // Swagger response documentation / Swagger 응답 문서화
    @ApiResponse(responseCode = "401", description = "Invalid credentials") // Swagger error response documentation / Swagger 오류 응답 문서화
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequest, // Uses DTO instead of request parameters / 요청 매개변수 대신 DTO 사용
            HttpServletResponse response) {
        // ✅ IMPROVEMENT: Using @RequestBody with DTO for security / 개선: 보안을 위해 DTO와 @RequestBody 사용

        try {
            log.info("Login attempt for username: {}", loginRequest.getUsername());
            // Authenticate user credentials / 사용자 자격증명 인증
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
            String token = jwtUtil.generateToken(userDetails.getUsername());

            // Create secure HTTP-only cookie / 보안 HTTP 전용 쿠키 생성
            Cookie cookie = new Cookie("onion_token", token);
            cookie.setHttpOnly(true); // Prevents XSS attacks / XSS 공격 방지
            cookie.setSecure(true); // HTTPS only in production / 프로덕션에서 HTTPS 전용
            cookie.setPath("/"); // Available for entire application / 전체 애플리케이션에서 사용 가능
            cookie.setMaxAge(jwtUtil.getExpirationTime()); // Uses configurable expiration / 설정 가능한 만료 시간 사용
            // ✅ IMPROVEMENT: Using configurable expiration time / 개선: 설정 가능한 만료 시간 사용

            response.addCookie(cookie);
            log.info("User {} logged in successfully", loginRequest.getUsername());

            return ResponseEntity.ok(new LoginResponseDTO(token, "Login successful", userDetails.getUsername()));
            // ✅ IMPROVEMENT: Structured response with DTO / 개선: DTO를 사용한 구조화된 응답

        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for username: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDTO(null, "Invalid credentials", null));
            // ✅ IMPROVEMENT: Proper error handling with structured response / 개선: 구조화된 응답으로 적절한 오류 처리
        }
    }

    @PostMapping("/logout") // Maps HTTP POST requests to /api/users/logout / HTTP POST 요청을 /api/users/logout에 매핑
    @Operation(summary = "User Logout", description = "Logout user and invalidate JWT token") // Swagger operation documentation / Swagger 작업 문서화
    @SecurityRequirement(name = "bearerAuth") // Requires Bearer token authentication / Bearer 토큰 인증 필요
    @ApiResponse(responseCode = "200", description = "Logout successful") // Swagger response documentation / Swagger 응답 문서화
    public ResponseEntity<LogoutResponseDTO> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader, // Gets token from header / 헤더에서 토큰 가져오기
            HttpServletResponse response) {
        // ✅ IMPROVEMENT: Added proper return type and token blacklisting / 개선: 적절한 반환 타입 및 토큰 블랙리스트 추가

        // Extract token from Authorization header / Authorization 헤더에서 토큰 추출
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);
            // Add token to blacklist / 토큰을 블랙리스트에 추가
            jwtBlacklistService.addToBlacklist(token, username);
            log.info("Token blacklisted for user: {}", username);
        }

        // Clear cookie by setting it to expire immediately / 즉시 만료되도록 설정하여 쿠키 지우기
        Cookie cookie = new Cookie("onion_token", "");
        cookie.setHttpOnly(true); // Maintains security settings / 보안 설정 유지
        cookie.setSecure(true); // HTTPS only in production / 프로덕션에서 HTTPS 전용
        cookie.setPath("/"); // Same path as when created / 생성 시와 동일한 경로
        cookie.setMaxAge(0); // Expires immediately / 즉시 만료
        response.addCookie(cookie);

        log.info("User logged out successfully");
        return ResponseEntity.ok(new LogoutResponseDTO("Logout successful", "Token invalidated"));
        // ✅ IMPROVEMENT: Returns structured response DTO / 개선: 구조화된 응답 DTO 반환
    }

    @PostMapping("/refresh") // Maps HTTP POST requests to /api/users/refresh / HTTP POST 요청을 /api/users/refresh에 매핑
    @Operation(summary = "Refresh Token", description = "Refresh JWT token using existing valid token") // Swagger operation documentation / Swagger 작업 문서화
    @SecurityRequirement(name = "bearerAuth") // Requires Bearer token authentication / Bearer 토큰 인증 필요
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully") // Swagger response documentation / Swagger 응답 문서화
    @ApiResponse(responseCode = "401", description = "Invalid or expired token") // Swagger error response documentation / Swagger 오류 응답 문서화
    public ResponseEntity<TokenRefreshResponseDTO> refreshToken(
            @RequestHeader("Authorization") String authHeader, // Gets token from Authorization header / Authorization 헤더에서 토큰 가져오기
            HttpServletResponse response) {
        // ✅ NEW FEATURE: Token refresh functionality / 새로운 기능: 토큰 갱신 기능

        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix / "Bearer " 접두사 제거

            // Validate current token / 현재 토큰 검증
            if (!jwtUtil.validateToken(token) || jwtBlacklistService.isBlacklisted(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new TokenRefreshResponseDTO(null, "Invalid or expired token"));
            }

            String username = jwtUtil.getUsernameFromToken(token);
            // Add old token to blacklist / 기존 토큰을 블랙리스트에 추가
            jwtBlacklistService.addToBlacklist(token, username);

            // Generate new token / 새 토큰 생성
            String newToken = jwtUtil.generateToken(username);

            // Update cookie with new token / 새 토큰으로 쿠키 업데이트
            Cookie cookie = new Cookie("onion_token", newToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(jwtUtil.getExpirationTime());
            response.addCookie(cookie);

            log.info("Token refreshed for user: {}", username);
            return ResponseEntity.ok(new TokenRefreshResponseDTO(newToken, "Token refreshed successfully"));

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenRefreshResponseDTO(null, "Token refresh failed"));
        }
    }

    @GetMapping("/profile") // Maps HTTP GET requests to /api/users/profile / HTTP GET 요청을 /api/users/profile에 매핑
    @Operation(summary = "Get User Profile", description = "Get current user's profile information") // Swagger operation documentation / Swagger 작업 문서화
    @SecurityRequirement(name = "bearerAuth") // Requires Bearer token authentication / Bearer 토큰 인증 필요
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully") // Swagger response documentation / Swagger 응답 문서화
    public ResponseEntity<UserProfileDTO> getUserProfile(
            @RequestHeader("Authorization") String authHeader) { // Gets token from Authorization header / Authorization 헤더에서 토큰 가져오기
        // ✅ NEW FEATURE: User profile endpoint / 새로운 기능: 사용자 프로필 엔드포인트

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        UserProfileDTO profile = userService.getUserProfile(username);

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile") // Maps HTTP PUT requests to /api/users/profile / HTTP PUT 요청을 /api/users/profile에 매핑
    @Operation(summary = "Update User Profile", description = "Update current user's profile information") // Swagger operation documentation / Swagger 작업 문서화
    @SecurityRequirement(name = "bearerAuth") // Requires Bearer token authentication / Bearer 토큰 인증 필요
    @ApiResponse(responseCode = "200", description = "Profile updated successfully") // Swagger response documentation / Swagger 응답 문서화
    public ResponseEntity<UserProfileDTO> updateUserProfile(
            @Valid @RequestBody UpdateProfileRequestDTO updateRequest, // Validates input DTO / 입력 DTO 검증
            @RequestHeader("Authorization") String authHeader) { // Gets token from Authorization header / Authorization 헤더에서 토큰 가져오기
        // ✅ NEW FEATURE: Profile update endpoint / 새로운 기능: 프로필 업데이트 엔드포인트

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        UserProfileDTO updatedProfile = userService.updateUserProfile(username, updateRequest);

        log.info("Profile updated for user: {}", username);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/change-password") // Maps HTTP POST requests to /api/users/change-password / HTTP POST 요청을 /api/users/change-password에 매핑
    @Operation(summary = "Change Password", description = "Change current user's password") // Swagger operation documentation / Swagger 작업 문서화
    @SecurityRequirement(name = "bearerAuth") // Requires Bearer token authentication / Bearer 토큰 인증 필요
    @ApiResponse(responseCode = "200", description = "Password changed successfully") // Swagger response documentation / Swagger 응답 문서화
    @ApiResponse(responseCode = "400", description = "Invalid current password") // Swagger error response documentation / Swagger 오류 응답 문서화
    public ResponseEntity<PasswordChangeResponseDTO> changePassword(
            @Valid @RequestBody PasswordChangeRequestDTO passwordChangeRequest, // Validates input DTO / 입력 DTO 검증
            @RequestHeader("Authorization") String authHeader) { // Gets token from Authorization header / Authorization 헤더에서 토큰 가져오기
        // ✅ NEW FEATURE: Password change endpoint / 새로운 기능: 비밀번호 변경 엔드포인트

        try {
            String token = authHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);

            userService.changePassword(username, passwordChangeRequest);
            log.info("Password changed for user: {}", username);

            return ResponseEntity.ok(new PasswordChangeResponseDTO("Password changed successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new PasswordChangeResponseDTO("Invalid current password"));
        }
    }

    // ❌ REMOVED: Dangerous token validation endpoint / 제거됨: 위험한 토큰 검증 엔드포인트
    // The previous /token/validation endpoint was removed due to security concerns:
    // 이전 /token/validation 엔드포인트는 보안 문제로 인해 제거되었습니다:
    // - Exposed tokens in URL parameters / URL 매개변수에 토큰 노출
    // - Could be abused for brute force attacks / 무차별 대입 공격에 남용 가능
    // - Server logs would contain sensitive tokens / 서버 로그에 민감한 토큰 포함
}