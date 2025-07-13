package com.onion.backend.config;

// Custom JWT authentication filter for processing JWT tokens / JWT 토큰 처리를 위한 커스텀 JWT 인증 필터
import com.onion.backend.jwt.JwtAuthenticationFilter;
// JWT utility class for token operations (create, validate, parse) / JWT 토큰 작업을 위한 유틸리티 클래스 (생성, 검증, 파싱)
import com.onion.backend.jwt.JwtUtil;
// Custom service for loading user details from database / 데이터베이스에서 사용자 정보를 로드하는 커스텀 서비스
import com.onion.backend.service.CustomUserDetailsService;
// JWT Blacklist service for managing revoked tokens / 취소된 토큰 관리를 위한 JWT 블랙리스트 서비스
import com.onion.backend.service.JwtBlacklistService;
// Annotation to define a Spring bean in configuration class / 설정 클래스에서 Spring 빈을 정의하는 어노테이션
import org.springframework.context.annotation.Bean;
// Annotation to mark this class as a Spring configuration class / 이 클래스가 Spring 설정 클래스임을 표시하는 어노테이션
import org.springframework.context.annotation.Configuration;
// Core interface for authentication management in Spring Security / Spring Security에서 인증 관리를 위한 핵심 인터페이스
import org.springframework.security.authentication.AuthenticationManager;
// Builder class for creating and configuring AuthenticationManager / AuthenticationManager 생성 및 구성을 위한 빌더 클래스
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
// Main configuration class for HTTP security settings / HTTP 보안 설정을 위한 메인 설정 클래스
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// Annotation to enable Spring Security web security support / Spring Security 웹 보안 지원을 활성화하는 어노테이션
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// Concrete implementation of PasswordEncoder using BCrypt hashing / BCrypt 해싱을 사용하는 PasswordEncoder의 구체적 구현
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// Interface for password encoding operations / 비밀번호 인코딩 작업을 위한 인터페이스
import org.springframework.security.crypto.password.PasswordEncoder;
// Interface representing the security filter chain / 보안 필터 체인을 나타내는 인터페이스
import org.springframework.security.web.SecurityFilterChain;
// Enum defining session creation policies for stateless/stateful apps / 무상태/상태 유지 앱을 위한 세션 생성 정책을 정의하는 열거형
import org.springframework.security.config.http.SessionCreationPolicy;
// Default Spring Security filter for username/password authentication / 사용자명/비밀번호 인증을 위한 Spring Security 기본 필터
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// Exception handler for authentication failures / 인증 실패를 위한 예외 핸들러
import org.springframework.security.web.AuthenticationEntryPoint;
// Exception handler for access denied scenarios / 접근 거부 시나리오를 위한 예외 핸들러
import org.springframework.security.web.access.AccessDeniedHandler;
// Lombok annotation for generating constructor with required arguments / 필수 인수로 생성자를 생성하는 Lombok 어노테이션
import lombok.RequiredArgsConstructor;

@Configuration // Marks this class as a configuration class containing bean definitions / 빈 정의를 포함하는 설정 클래스임을 표시
@EnableWebSecurity // Enables Spring Security's web security support and integrates with Spring MVC / Spring Security의 웹 보안 지원을 활성화하고 Spring MVC와 통합
@RequiredArgsConstructor // Lombok: generates constructor with final fields for dependency injection / Lombok: 의존성 주입을 위한 final 필드로 생성자 생성
public class SecurityConfig {

    // ✅ IMPROVEMENT: Using constructor injection instead of field injection / 개선: 필드 주입 대신 생성자 주입 사용
    private final CustomUserDetailsService userDetailsService; // Custom service for loading user details from database / 데이터베이스에서 사용자 정보를 로드하는 커스텀 서비스
    private final JwtUtil jwtUtil; // JWT utility class for token operations / JWT 토큰 작업을 위한 유틸리티 클래스
    private final JwtBlacklistService jwtBlacklistService; // JWT Blacklist service for managing revoked tokens / 취소된 토큰 관리를 위한 JWT 블랙리스트 서비스

    @Bean // Creates and registers JWT authentication filter as a Spring bean / JWT 인증 필터를 Spring 빈으로 생성 및 등록
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService, jwtBlacklistService);
        // ✅ IMPROVEMENT: Filter is now a bean managed by Spring / 개선: 필터가 이제 Spring에 의해 관리되는 빈임
    }

    @Bean // Creates and registers a Spring bean for security filter chain / 보안 필터 체인을 위한 Spring 빈 생성 및 등록
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Disables CSRF protection using modern lambda syntax / 최신 람다 구문을 사용하여 CSRF 보호 비활성화
                // ✅ IMPROVEMENT: Using modern Spring Security configuration syntax / 개선: 최신 Spring Security 설정 구문 사용
                .authorizeHttpRequests(auth -> auth // Configures request-based authorization rules / 요청 기반 권한 부여 규칙 설정
                        .requestMatchers( // Specifies public endpoints that don't require authentication / 인증이 필요하지 않은 공개 엔드포인트 지정
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/users/signUp",
                                "/api/users/login",
                                "/api/users/refresh" // Added refresh token endpoint / 리프레시 토큰 엔드포인트 추가
                        ).permitAll()
                        .anyRequest().authenticated() // All other requests require authentication / 다른 모든 요청은 인증 필요
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Configures stateless session management / 무상태 세션 관리 설정
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class) // Adds JWT filter before default authentication filter / 기본 인증 필터 전에 JWT 필터 추가
                // ✅ IMPROVEMENT: Using bean injection instead of direct instantiation / 개선: 직접 인스턴스화 대신 빈 주입 사용
                .exceptionHandling(ex -> ex // Configures exception handling for authentication and authorization / 인증 및 권한 부여를 위한 예외 처리 설정
                        .authenticationEntryPoint(customAuthenticationEntryPoint()) // Sets custom authentication entry point / 커스텀 인증 진입점 설정
                        .accessDeniedHandler(customAccessDeniedHandler()) // Sets custom access denied handler / 커스텀 접근 거부 핸들러 설정
                )
                .build(); // Builds and returns the configured SecurityFilterChain / 설정된 SecurityFilterChain을 빌드하고 반환
    }

    @Bean // Creates and registers a Spring bean for password encoding / 비밀번호 인코딩을 위한 Spring 빈 생성 및 등록
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt algorithm - industry standard for password hashing / BCrypt 알고리즘 - 비밀번호 해싱의 업계 표준
    }

    @Bean // Creates and registers a Spring bean for authentication management / 인증 관리를 위한 Spring 빈 생성 및 등록
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class) // Gets the shared AuthenticationManagerBuilder / 공유 AuthenticationManagerBuilder 가져오기
                .userDetailsService(userDetailsService) // Sets custom user details service / 커스텀 사용자 세부정보 서비스 설정
                .passwordEncoder(passwordEncoder()) // Sets password encoder for authentication / 인증을 위한 비밀번호 인코더 설정
                .and()
                .build(); // Builds and returns the configured AuthenticationManager / 설정된 AuthenticationManager를 빌드하고 반환
    }

    @Bean // Creates custom authentication entry point for handling authentication failures / 인증 실패 처리를 위한 커스텀 인증 진입점 생성
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401); // Sets HTTP status to 401 Unauthorized / HTTP 상태를 401 Unauthorized로 설정
            response.setContentType("application/json"); // Sets response content type to JSON / 응답 콘텐츠 타입을 JSON으로 설정
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
            // Writes JSON error response with exception message / 예외 메시지와 함께 JSON 오류 응답 작성
        };
    }

    @Bean // Creates custom access denied handler for handling authorization failures / 권한 부여 실패 처리를 위한 커스텀 접근 거부 핸들러 생성
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403); // Sets HTTP status to 403 Forbidden / HTTP 상태를 403 Forbidden으로 설정
            response.setContentType("application/json"); // Sets response content type to JSON / 응답 콘텐츠 타입을 JSON으로 설정
            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
            // Writes JSON error response for access denied scenarios / 접근 거부 시나리오에 대한 JSON 오류 응답 작성
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/signUp", "/api/users/login",
                                "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // ✅ 이제 빈 주입 사용
                .build();
    }
}