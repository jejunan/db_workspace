package com.onion.backend.jwt;

// JWT blacklist service for managing revoked tokens / 취소된 토큰 관리를 위한 JWT 블랙리스트 서비스
import com.onion.backend.service.JwtBlacklistService;
// Filter chain interface for servlet filter processing / 서블릿 필터 처리를 위한 필터 체인 인터페이스
import jakarta.servlet.FilterChain;
// Servlet exception for filter processing errors / 필터 처리 오류를 위한 서블릿 예외
import jakarta.servlet.ServletException;
// HTTP cookie class for session management / 세션 관리를 위한 HTTP 쿠키 클래스
import jakarta.servlet.http.Cookie;
// HTTP servlet request interface / HTTP 서블릿 요청 인터페이스
import jakarta.servlet.http.HttpServletRequest;
// HTTP servlet response interface / HTTP 서블릿 응답 인터페이스
import jakarta.servlet.http.HttpServletResponse;
// Lombok annotation for constructor generation / 생성자 생성을 위한 Lombok 어노테이션
import lombok.RequiredArgsConstructor;
// Lombok annotation for logging / 로깅을 위한 Lombok 어노테이션
import lombok.extern.slf4j.Slf4j;
// Spring beans factory annotation / Spring 빈 팩토리 어노테이션
import org.springframework.beans.factory.annotation.Value;
// Spring Security context holder for managing security context / 보안 컨텍스트 관리를 위한 Spring Security 컨텍스트 홀더
import org.springframework.security.core.context.SecurityContextHolder;
// Interface for user details used in authentication / 인증에 사용되는 사용자 세부정보 인터페이스
import org.springframework.security.core.userdetails.UserDetails;
// Service interface for loading user details / 사용자 세부정보 로드를 위한 서비스 인터페이스
import org.springframework.security.core.userdetails.UserDetailsService;
// Exception thrown when user is not found / 사용자를 찾을 수 없을 때 발생하는 예외
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// Class for building web authentication details / 웹 인증 세부정보 빌드를 위한 클래스
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
// Authentication token implementation for username/password / 사용자명/비밀번호를 위한 인증 토큰 구현
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Spring stereotype annotation / Spring 스테레오타입 어노테이션
import org.springframework.stereotype.Component;
// Base class for filters that should only execute once per request / 요청당 한 번만 실행되어야 하는 필터를 위한 기본 클래스
import org.springframework.web.filter.OncePerRequestFilter;
// Spring utility for string operations / 문자열 작업을 위한 Spring 유틸리티
import org.springframework.util.StringUtils;

// Java IO exception / Java IO 예외
import java.io.IOException;
// Java Set for collections / 컬렉션을 위한 Java Set
import java.util.Set;
// Java Arrays utility / Java 배열 유틸리티
import java.util.Arrays;

@Component // Marks this class as a Spring component for automatic detection / 자동 감지를 위해 이 클래스를 Spring 컴포넌트로 표시
@RequiredArgsConstructor // Lombok: generates constructor with final fields / Lombok: final 필드로 생성자 생성
@Slf4j // Lombok: generates logger field for logging / Lombok: 로깅을 위한 로거 필드 생성
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JWT utility for token operations / 토큰 작업을 위한 JWT 유틸리티
    private final JwtUtil jwtUtil;
    // Service for loading user details from database / 데이터베이스에서 사용자 세부정보 로드를 위한 서비스
    private final UserDetailsService userDetailsService;
    // Service for managing JWT token blacklist / JWT 토큰 블랙리스트 관리를 위한 서비스
    private final JwtBlacklistService jwtBlacklistService;

    @Value("${jwt.cookie.name:onion_token}") // Configurable cookie name with default value / 기본값과 함께 설정 가능한 쿠키 이름
    private String jwtCookieName;

    // Endpoints that should bypass JWT authentication / JWT 인증을 우회해야 하는 엔드포인트
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/api/users/signUp", // User registration endpoint / 사용자 등록 엔드포인트
            "/api/users/login", // User login endpoint / 사용자 로그인 엔드포인트
            "/swagger-ui", // Swagger UI endpoints / Swagger UI 엔드포인트
            "/v3/api-docs", // OpenAPI documentation / OpenAPI 문서
            "/actuator" // Spring Actuator endpoints / Spring Actuator 엔드포인트
    );

    @Override // Overrides the main filter method that processes each request / 각 요청을 처리하는 메인 필터 메서드 오버라이드
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            // Check if current path should be excluded from JWT authentication / 현재 경로가 JWT 인증에서 제외되어야 하는지 확인
            if (shouldSkipAuthentication(request)) {
                log.debug("Skipping JWT authentication for path: {}", request.getRequestURI());
                chain.doFilter(request, response);
                return;
            }

            // Extract JWT token from request headers or cookies / 요청 헤더나 쿠키에서 JWT 토큰 추출
            String token = resolveToken(request);

            if (token != null) {
                log.debug("JWT token found, validating...");
                authenticateWithToken(token, request);
            } else {
                log.debug("No JWT token found in request");
            }

        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            // Clear security context on authentication failure / 인증 실패 시 보안 컨텍스트 정리
            SecurityContextHolder.clearContext();
            // Set error response / 오류 응답 설정
            setErrorResponse(response, "Authentication failed");
            return;
        }

        // Continue with the filter chain / 필터 체인 계속 진행
        chain.doFilter(request, response);
    }

    // Authenticate user with provided JWT token / 제공된 JWT 토큰으로 사용자 인증
    private void authenticateWithToken(String token, HttpServletRequest request) {
        // Validate token: format, expiration, and blacklist status / 토큰 검증: 형식, 만료, 블랙리스트 상태
        if (!jwtUtil.validateToken(token)) {
            log.debug("JWT token validation failed");
            return;
        }

        if (jwtBlacklistService.isTokenBlacklisted(token)) {
            log.debug("JWT token is blacklisted");
            return;
        }

        // Extract username from validated token / 검증된 토큰에서 사용자명 추출
        String username = jwtUtil.getUsernameFromToken(token);
        if (!StringUtils.hasText(username)) {
            log.debug("No username found in JWT token");
            return;
        }

        // Check if user is already authenticated in current security context / 현재 보안 컨텍스트에서 사용자가 이미 인증되었는지 확인
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("User already authenticated in security context");
            return;
        }

        try {
            // Load user details from database / 데이터베이스에서 사용자 세부정보 로드
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Create authentication token with user details and authorities / 사용자 세부정보 및 권한으로 인증 토큰 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, // Principal (user details) / 주체 (사용자 세부정보)
                            null, // Credentials (not needed for token auth) / 자격증명 (토큰 인증에는 불필요)
                            userDetails.getAuthorities() // User authorities / 사용자 권한
                    );

            // Set additional authentication details from request / 요청에서 추가 인증 세부정보 설정
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context for current thread / 현재 스레드의 보안 컨텍스트에 인증 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("User {} authenticated successfully via JWT", username);

        } catch (UsernameNotFoundException e) {
            log.debug("User not found: {}", username);
        } catch (Exception e) {
            log.error("Error loading user details for username {}: {}", username, e.getMessage());
        }
    }

    // Extracts JWT token from Authorization header or cookies / Authorization 헤더나 쿠키에서 JWT 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        // First priority: Authorization header with Bearer prefix / 첫 번째 우선순위: Bearer 접두사가 있는 Authorization 헤더
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7); // Remove "Bearer " prefix / "Bearer " 접두사 제거
            log.debug("JWT token found in Authorization header");
            return token;
        }

        // Second priority: JWT cookie / 두 번째 우선순위: JWT 쿠키
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (jwtCookieName.equals(cookie.getName())) { // Use configurable cookie name / 설정 가능한 쿠키 이름 사용
                    // ✅ IMPROVEMENT: Fixed cookie name inconsistency / 개선: 쿠키 이름 불일치 수정
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.debug("JWT token found in cookie: {}", jwtCookieName);
                        return token;
                    }
                }
            }
        }

        return null; // No valid token found / 유효한 토큰을 찾지 못함
    }

    // Check if current request path should skip JWT authentication / 현재 요청 경로가 JWT 인증을 건너뛰어야 하는지 확인
    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI(); // Get request URI / 요청 URI 가져오기

        // Check if path starts with any excluded path / 경로가 제외된 경로로 시작하는지 확인
        return EXCLUDED_PATHS.stream()
                .anyMatch(excludedPath -> path.startsWith(excludedPath));
    }

    // Set error response for authentication failures / 인증 실패에 대한 오류 응답 설정
    private void setErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Set HTTP 401 status / HTTP 401 상태 설정
        response.setContentType("application/json"); // Set JSON content type / JSON 콘텐츠 타입 설정
        response.setCharacterEncoding("UTF-8"); // Set UTF-8 encoding / UTF-8 인코딩 설정

        // Write JSON error response / JSON 오류 응답 작성
        String jsonResponse = String.format(
                "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message,
                java.time.Instant.now().toString()
        );
        response.getWriter().write(jsonResponse);
    }

    @Override // Override to specify filter should not run for certain conditions / 특정 조건에서 필터가 실행되지 않도록 지정하기 위해 오버라이드
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Additional condition to skip filter entirely for static resources / 정적 리소스에 대해 필터를 완전히 건너뛰기 위한 추가 조건
        String path = request.getRequestURI();
        return path.startsWith("/static/") || // Static resources / 정적 리소스
                path.startsWith("/css/") || // CSS files / CSS 파일
                path.startsWith("/js/") || // JavaScript files / JavaScript 파일
                path.startsWith("/images/") || // Image files / 이미지 파일
                path.endsWith(".ico"); // Favicon / 파비콘
    }

    @Override // Clean up security context after request processing / 요청 처리 후 보안 컨텍스트 정리
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            super.doFilterInternal(request, response, filterChain); // Call parent implementation / 부모 구현 호출
        } finally {
            // Clean up security context to prevent memory leaks / 메모리 누수 방지를 위한 보안 컨텍스트 정리
            // Note: This is optional for thread-per-request model but good practice / 참고: 요청당 스레드 모델에서는 선택사항이지만 좋은 관행
            if (shouldClearSecurityContext(request)) {
                SecurityContextHolder.clearContext();
                log.debug("Security context cleared for request: {}", request.getRequestURI());
            }
        }
    }

    // Determine if security context should be cleared / 보안 컨텍스트를 정리해야 하는지 결정
    private boolean shouldClearSecurityContext(HttpServletRequest request) {
        // Clear context for certain paths or conditions / 특정 경로나 조건에서 컨텍스트 정리
        String path = request.getRequestURI();
        return path.startsWith("/api/users/logout") || // Logout endpoint / 로그아웃 엔드포인트
                path.startsWith("/actuator/"); // Actuator endpoints / Actuator 엔드포인트
    }
}