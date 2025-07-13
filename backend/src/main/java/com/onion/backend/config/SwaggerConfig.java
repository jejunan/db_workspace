package com.onion.backend.config;

// OpenAPI 3.0 specification main model class for defining API structure / API 구조 정의를 위한 OpenAPI 3.0 사양의 메인 모델 클래스
import io.swagger.v3.oas.models.OpenAPI;
// Contact information model for API documentation / API 문서화를 위한 연락처 정보 모델
import io.swagger.v3.oas.models.info.Contact;
// API metadata information container (title, version, description, etc.) / API 메타데이터 정보 컨테이너 (제목, 버전, 설명 등)
import io.swagger.v3.oas.models.info.Info;
// License information model for API documentation / API 문서화를 위한 라이선스 정보 모델
import io.swagger.v3.oas.models.info.License;
// Security requirement configuration for API endpoints / API 엔드포인트를 위한 보안 요구사항 설정
import io.swagger.v3.oas.models.security.SecurityRequirement;
// Security scheme definition (authentication methods) / 보안 스키마 정의 (인증 방법)
import io.swagger.v3.oas.models.security.SecurityScheme;
// Server configuration model for API endpoints / API 엔드포인트를 위한 서버 설정 모델
import io.swagger.v3.oas.models.servers.Server;
// SpringDoc grouped API configuration for organizing endpoints / 엔드포인트 구성을 위한 SpringDoc 그룹화된 API 설정
import org.springdoc.core.models.GroupedOpenApi;
// Annotation for injecting property values from configuration files / 설정 파일에서 속성 값을 주입하기 위한 어노테이션
import org.springframework.beans.factory.annotation.Value;
// Annotation to define a Spring bean in configuration / 설정에서 Spring 빈을 정의하는 어노테이션
import org.springframework.context.annotation.Bean;
// Annotation to mark this class as a Spring configuration / 이 클래스가 Spring 설정임을 표시하는 어노테이션
import org.springframework.context.annotation.Configuration;
// Annotation for conditional bean creation based on active profiles / 활성 프로필을 기반으로 조건부 빈 생성을 위한 어노테이션
import org.springframework.context.annotation.Profile;

// Java List interface for collections / 컬렉션을 위한 Java List 인터페이스
import java.util.List;

@Configuration // Marks this class as a configuration class for Spring IoC container / Spring IoC 컨테이너를 위한 설정 클래스임을 표시
public class SwaggerConfig {

    @Value("${app.version:1.0.0}") // Injects application version from properties with default fallback / 기본값 대체와 함께 속성에서 애플리케이션 버전 주입
    private String appVersion;

    @Value("${app.name:Onion Backend API}") // Injects application name from properties with default fallback / 기본값 대체와 함께 속성에서 애플리케이션 이름 주입
    private String appName;

    @Value("${app.description:REST API for Onion application with JWT authentication}") // Injects app description from properties / 속성에서 앱 설명 주입
    private String appDescription;

    @Value("${server.port:8080}") // Injects server port from properties with default fallback / 기본값 대체와 함께 속성에서 서버 포트 주입
    private String serverPort;

    @Value("${app.contact.name:Development Team}") // Injects contact name from properties / 속성에서 연락처 이름 주입
    private String contactName;

    @Value("${app.contact.email:dev@onion.com}") // Injects contact email from properties / 속성에서 연락처 이메일 주입
    private String contactEmail;

    @Value("${app.contact.url:https://github.com/onion-team}") // Injects contact URL from properties / 속성에서 연락처 URL 주입
    private String contactUrl;

    @Bean // Creates and registers a Spring bean for OpenAPI configuration / OpenAPI 설정을 위한 Spring 빈 생성 및 등록
    public OpenAPI customOpenAPI() {
        return new OpenAPI() // Creates new OpenAPI specification instance / 새로운 OpenAPI 사양 인스턴스 생성
                .info(buildApiInfo()) // Sets API information using builder method / 빌더 메서드를 사용하여 API 정보 설정
                .servers(buildServers()) // Sets server configurations for different environments / 다양한 환경을 위한 서버 설정 지정
                .components(new io.swagger.v3.oas.models.Components() // Defines reusable components for the API specification / API 사양을 위한 재사용 가능한 컴포넌트 정의
                        .addSecuritySchemes("bearerAuth", buildBearerSecurityScheme()) // Adds Bearer token security scheme / Bearer 토큰 보안 스키마 추가
                        .addSecuritySchemes("cookieAuth", buildCookieSecurityScheme())); // Adds cookie-based security scheme / 쿠키 기반 보안 스키마 추가
        // Note: Global security requirements removed - use @SecurityRequirement on individual controllers / 참고: 전역 보안 요구사항 제거됨 - 개별 컨트롤러에서 @SecurityRequirement 사용
    }

    // Builds comprehensive API information with contact and license details / 연락처 및 라이선스 세부정보가 포함된 포괄적인 API 정보 구축
    private Info buildApiInfo() {
        return new Info() // Creates new API info instance / 새로운 API 정보 인스턴스 생성
                .title(appName) // Sets API title from injected property / 주입된 속성에서 API 제목 설정
                // ✅ IMPROVEMENT: Using externalized configuration instead of hardcoding / 개선: 하드코딩 대신 외부화된 설정 사용
                .version(appVersion) // Sets API version from injected property / 주입된 속성에서 API 버전 설정
                .description(appDescription) // Sets API description from injected property / 주입된 속성에서 API 설명 설정
                .contact(new Contact() // Adds contact information for API maintainers / API 관리자를 위한 연락처 정보 추가
                        .name(contactName) // Sets contact name from property / 속성에서 연락처 이름 설정
                        .email(contactEmail) // Sets contact email from property / 속성에서 연락처 이메일 설정
                        .url(contactUrl)) // Sets contact URL from property / 속성에서 연락처 URL 설정
                .license(new License() // Adds license information for legal compliance / 법적 준수를 위한 라이선스 정보 추가
                        .name("MIT License") // Sets license name / 라이선스 이름 설정
                        .url("https://opensource.org/licenses/MIT")); // Sets license URL for reference / 참조를 위한 라이선스 URL 설정
    }

    // Builds server configurations for different environments / 다양한 환경을 위한 서버 설정 구축
    private List<Server> buildServers() {
        Server localServer = new Server() // Creates local development server configuration / 로컬 개발 서버 설정 생성
                .url("http://localhost:" + serverPort) // Sets local server URL with dynamic port injection / 동적 포트 주입으로 로컬 서버 URL 설정
                .description("Local Development Server"); // Sets descriptive name for local environment / 로컬 환경을 위한 설명적 이름 설정

        Server prodServer = new Server() // Creates production server configuration / 프로덕션 서버 설정 생성
                .url("https://api.onion.com") // Sets production server URL / 프로덕션 서버 URL 설정
                .description("Production Server"); // Sets descriptive name for production environment / 프로덕션 환경을 위한 설명적 이름 설정

        return List.of(localServer, prodServer); // Returns immutable list of configured servers / 설정된 서버의 불변 목록 반환
    }

    // Builds JWT Bearer token security scheme configuration / JWT Bearer 토큰 보안 스키마 설정 구축
    private SecurityScheme buildBearerSecurityScheme() {
        return new SecurityScheme() // Creates new security scheme instance / 새로운 보안 스키마 인스턴스 생성
                .type(SecurityScheme.Type.HTTP) // Sets security type to HTTP for standard authentication / 표준 인증을 위해 보안 타입을 HTTP로 설정
                .scheme("bearer") // Sets authentication scheme to bearer token / 인증 스키마를 베어러 토큰으로 설정
                .bearerFormat("JWT") // Specifies bearer token format as JWT / 베어러 토큰 형식을 JWT로 지정
                .description("JWT Bearer token authentication. Format: Authorization: Bearer <token>"); // Provides detailed description with usage format / 사용 형식과 함께 상세한 설명 제공
    }

    // Builds cookie-based security scheme configuration for alternative authentication / 대안 인증을 위한 쿠키 기반 보안 스키마 설정 구축
    private SecurityScheme buildCookieSecurityScheme() {
        return new SecurityScheme() // Creates new security scheme instance / 새로운 보안 스키마 인스턴스 생성
                .type(SecurityScheme.Type.APIKEY) // Sets security type to API key for cookie-based auth / 쿠키 기반 인증을 위해 보안 타입을 API 키로 설정
                .in(SecurityScheme.In.COOKIE) // Specifies that API key is transmitted via cookie / API 키가 쿠키를 통해 전송됨을 지정
                .name("onion_token") // Sets cookie name that contains JWT token / JWT 토큰을 포함하는 쿠키 이름 설정
                .description("JWT token stored in HTTP-only cookie for enhanced security"); // Provides description emphasizing security benefits / 보안 이점을 강조하는 설명 제공
    }

    // Public API group for endpoints that don't require authentication / 인증이 필요하지 않은 엔드포인트를 위한 공개 API 그룹
    @Bean // Creates and registers a Spring bean for public API group / 공개 API 그룹을 위한 Spring 빈 생성 및 등록
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder() // Creates grouped API builder for organizing endpoints / 엔드포인트 구성을 위한 그룹화된 API 빌더 생성
                .group("public") // Sets group name for public endpoints / 공개 엔드포인트를 위한 그룹 이름 설정
                .pathsToMatch("/api/users/signUp", "/api/users/login", "/api/users/refresh") // Specifies paths for public endpoints / 공개 엔드포인트를 위한 경로 지정
                .build(); // Builds the grouped API configuration / 그룹화된 API 설정 빌드
    }

    // Private API group for endpoints that require authentication / 인증이 필요한 엔드포인트를 위한 비공개 API 그룹
    @Bean // Creates and registers a Spring bean for private API group / 비공개 API 그룹을 위한 Spring 빈 생성 및 등록
    public GroupedOpenApi privateApi() {
        return GroupedOpenApi.builder() // Creates grouped API builder for organizing protected endpoints / 보호된 엔드포인트 구성을 위한 그룹화된 API 빌더 생성
                .group("private") // Sets group name for private endpoints / 비공개 엔드포인트를 위한 그룹 이름 설정
                .pathsToMatch("/api/**") // Matches all API paths / 모든 API 경로 매칭
                .pathsToExclude("/api/users/signUp", "/api/users/login", "/api/users/refresh") // Excludes public endpoints from this group / 이 그룹에서 공개 엔드포인트 제외
                .build(); // Builds the grouped API configuration / 그룹화된 API 설정 빌드
    }

    // Development environment specific configuration with enhanced debugging / 향상된 디버깅을 위한 개발 환경별 특정 설정
    @Bean // Creates and registers a Spring bean for development OpenAPI configuration / 개발용 OpenAPI 설정을 위한 Spring 빈 생성 및 등록
    @Profile("dev") // Only creates this bean when 'dev' profile is active / 'dev' 프로필이 활성화된 경우에만 이 빈 생성
    public OpenAPI devOpenAPI() {
        return customOpenAPI() // Uses base OpenAPI configuration as foundation / 기본 OpenAPI 설정을 기반으로 사용
                .info(new Info() // Overrides API info specifically for development environment / 개발 환경을 위해 API 정보를 특별히 재정의
                        .title(appName + " (Development)") // Adds development indicator to title for clarity / 명확성을 위해 제목에 개발 표시기 추가
                        .version(appVersion + "-dev") // Adds development suffix to version / 버전에 개발 접미사 추가
                        .description(appDescription + "\n\n⚠️ **Development Environment**\n\n" +
                                "This is a development instance. All data is for testing purposes only.")); // Adds comprehensive development warning / 포괄적인 개발 경고 추가
    }
}