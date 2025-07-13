package com.onion.backend.jwt;

// JWT related imports for token processing / 토큰 처리를 위한 JWT 관련 import
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
// Lombok annotations for code generation / 코드 생성을 위한 Lombok 어노테이션
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
// Spring annotations for configuration and component management / 설정 및 컴포넌트 관리를 위한 Spring 어노테이션
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
// Spring utility for string operations / 문자열 작업을 위한 Spring 유틸리티
import org.springframework.util.StringUtils;

// Java time and date APIs / Java 시간 및 날짜 API
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component // Marks this class as a Spring component for automatic detection / 자동 감지를 위해 이 클래스를 Spring 컴포넌트로 표시
@Slf4j // Lombok: generates logger field for logging / Lombok: 로깅을 위한 로거 필드 생성
public class JwtUtil {

    @Value("${jwt.secret}") // Injects JWT secret from application properties / application properties에서 JWT 시크릿 주입
    // ✅ SECURITY IMPROVEMENT: Externalized configuration instead of hardcoding / 보안 개선: 하드코딩 대신 외부화된 설정
    private String secretKey;

    @Value("${jwt.expiration:3600000}") // Injects expiration time with default fallback / 기본값 대체와 함께 만료 시간 주입
    @Getter // Lombok: generates getter for external access / Lombok: 외부 접근을 위한 getter 생성
    private long expirationTime;

    @Value("${jwt.refresh-expiration:86400000}") // Refresh token expiration (24 hours) / 리프레시 토큰 만료 시간 (24시간)
    private long refreshExpirationTime;

    @Value("${jwt.issuer:onion-backend}") // Token issuer identification / 토큰 발행자 식별
    private String issuer;

    // Generates JWT access token with username as subject / 사용자명을 주체로 하여 JWT 액세스 토큰 생성
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>(); // Create claims map for additional data / 추가 데이터를 위한 클레임 맵 생성
        return createToken(claims, username, expirationTime);
    }

    // Generates JWT access token with additional claims / 추가 클레임과 함께 JWT 액세스 토큰 생성
    public String generateToken(String username, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>(additionalClaims); // Copy additional claims / 추가 클레임 복사
        return createToken(claims, username, expirationTime);
    }

    // Generates JWT refresh token for token renewal / 토큰 갱신을 위한 JWT 리프레시 토큰 생성
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>(); // Create claims map / 클레임 맵 생성
        claims.put("type", "refresh"); // Mark as refresh token / 리프레시 토큰으로 표시
        return createToken(claims, username, refreshExpirationTime);
    }

    // Creates JWT token with specified claims, subject and expiration / 지정된 클레임, 주체 및 만료 시간으로 JWT 토큰 생성
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date(); // Current timestamp / 현재 타임스탬프
        Date expirationDate = new Date(now.getTime() + expiration); // Calculate expiration time / 만료 시간 계산

        try {
            String token = Jwts.builder() // Creates JWT builder instance / JWT 빌더 인스턴스 생성
                    .setClaims(claims) // Sets additional claims / 추가 클레임 설정
                    .setSubject(subject) // Sets the subject (username) of the token / 토큰의 주체(사용자명) 설정
                    .setIssuer(issuer) // Sets token issuer / 토큰 발행자 설정
                    .setIssuedAt(now) // Sets token issued time / 토큰 발행 시간 설정
                    .setExpiration(expirationDate) // Sets token expiration time / 토큰 만료 시간 설정
                    .signWith(SignatureAlgorithm.HS256, secretKey) // Signs token with HS256 algorithm and secret key / HS256 알고리즘과 비밀 키로 토큰 서명
                    .compact(); // Builds and returns the JWT string / JWT 문자열을 빌드하고 반환

            log.debug("Generated JWT token for user: {} with expiration: {}", subject, expirationDate);
            return token;

        } catch (Exception e) {
            log.error("Error generating JWT token for user: {}", subject, e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    // Validates JWT token signature, expiration and format / JWT 토큰 서명, 만료 및 형식 검증
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) { // Check if token is not empty / 토큰이 비어있지 않은지 확인
            log.debug("Token validation failed: token is empty");
            return false;
        }

        try {
            Jwts.parser() // Creates JWT parser instance / JWT 파서 인스턴스 생성
                    .setSigningKey(secretKey) // Sets the signing key for validation / 검증을 위한 서명 키 설정
                    .parseClaimsJws(token); // Parses and validates the token / 토큰을 파싱하고 검증

            log.debug("Token validation successful");
            return true; // Token is valid / 토큰이 유효함

        } catch (ExpiredJwtException e) {
            log.debug("Token validation failed: token is expired");
        } catch (UnsupportedJwtException e) {
            log.debug("Token validation failed: token is unsupported");
        } catch (MalformedJwtException e) {
            log.debug("Token validation failed: token is malformed");
        } catch (SignatureException e) {
            log.debug("Token validation failed: signature validation failed");
        } catch (IllegalArgumentException e) {
            log.debug("Token validation failed: token compact is invalid");
        } catch (Exception e) {
            log.error("Unexpected error during token validation", e);
        }

        return false; // Token is invalid / 토큰이 무효함
    }

    // Extracts username from JWT token safely / JWT 토큰에서 사용자명을 안전하게 추출
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Extracts expiration date from JWT token safely / JWT 토큰에서 만료 날짜를 안전하게 추출
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // Extracts issued date from JWT token safely / JWT 토큰에서 발행 날짜를 안전하게 추출
    public Date getIssuedAtDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    // Extracts specific claim from JWT token using function / 함수를 사용하여 JWT 토큰에서 특정 클레임 추출
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = getAllClaimsFromToken(token); // Get all claims from token / 토큰에서 모든 클레임 가져오기
            return claimsResolver.apply(claims); // Apply function to extract specific claim / 특정 클레임 추출을 위해 함수 적용
        } catch (Exception e) {
            log.error("Error extracting claim from token", e);
            return null; // Return null on error / 오류 시 null 반환
        }
    }

    // Extracts all claims from JWT token / JWT 토큰에서 모든 클레임 추출
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser() // Creates JWT parser instance / JWT 파서 인스턴스 생성
                .setSigningKey(secretKey) // Sets the signing key for parsing / 파싱을 위한 서명 키 설정
                .parseClaimsJws(token) // Parses the token and extracts claims / 토큰을 파싱하고 클레임 추출
                .getBody(); // Gets the claims body / 클레임 본문 가져오기
    }

    // Checks if JWT token is expired / JWT 토큰이 만료되었는지 확인
    public boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token); // Get expiration date / 만료 날짜 가져오기
            return expiration != null && expiration.before(new Date()); // Check if expired / 만료되었는지 확인
        } catch (Exception e) {
            log.debug("Error checking token expiration, assuming expired", e);
            return true; // Assume expired on error / 오류 시 만료된 것으로 가정
        }
    }

    // Checks if JWT token is a refresh token / JWT 토큰이 리프레시 토큰인지 확인
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token); // Get all claims / 모든 클레임 가져오기
            return "refresh".equals(claims.get("type")); // Check if type is refresh / 타입이 리프레시인지 확인
        } catch (Exception e) {
            log.debug("Error checking token type", e);
            return false; // Not a refresh token on error / 오류 시 리프레시 토큰이 아님
        }
    }

    // Gets remaining time until token expiration in seconds / 토큰 만료까지 남은 시간을 초 단위로 가져오기
    public long getRemainingExpirationTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token); // Get expiration date / 만료 날짜 가져오기
            if (expiration == null) return 0; // Return 0 if no expiration / 만료 시간이 없으면 0 반환

            long remaining = expiration.getTime() - System.currentTimeMillis(); // Calculate remaining time / 남은 시간 계산
            return Math.max(0, remaining / 1000); // Return seconds, minimum 0 / 초 단위로 반환, 최소 0
        } catch (Exception e) {
            log.debug("Error calculating remaining expiration time", e);
            return 0; // Return 0 on error / 오류 시 0 반환
        }
    }

    // Converts Date to LocalDateTime for modern date handling / 현대적인 날짜 처리를 위해 Date를 LocalDateTime으로 변환
    public LocalDateTime getExpirationTimeFromToken(String token) {
        Date expirationDate = getExpirationDateFromToken(token); // Get expiration as Date / Date로 만료 시간 가져오기
        if (expirationDate == null) return null; // Return null if no date / 날짜가 없으면 null 반환

        return expirationDate.toInstant() // Convert to Instant / Instant로 변환
                .atZone(ZoneId.systemDefault()) // Apply system timezone / 시스템 시간대 적용
                .toLocalDateTime(); // Convert to LocalDateTime / LocalDateTime으로 변환
    }

    // Gets expiration time in seconds for cookie/cache configuration / 쿠키/캐시 설정을 위해 만료 시간을 초 단위로 가져오기
    public int getExpirationTime() {
        return (int) (expirationTime / 1000); // Convert milliseconds to seconds / 밀리초를 초로 변환
    }

    // Utility method to check if token needs refresh (expires within threshold) / 토큰이 갱신이 필요한지 확인하는 유틸리티 메서드 (임계값 내에서 만료)
    public boolean shouldRefreshToken(String token) {
        try {
            long remainingTime = getRemainingExpirationTime(token); // Get remaining time / 남은 시간 가져오기
            long refreshThreshold = expirationTime / 1000 / 4; // Refresh if less than 1/4 of expiration time remains / 만료 시간의 1/4 미만이 남으면 갱신

            boolean shouldRefresh = remainingTime > 0 && remainingTime < refreshThreshold;
            if (shouldRefresh) {
                log.debug("Token should be refreshed, remaining time: {} seconds", remainingTime);
            }
            return shouldRefresh;
        } catch (Exception e) {
            log.debug("Error checking if token should be refreshed", e);
            return false;
        }
    }

    // Validates token format without full parsing (for performance) / 전체 파싱 없이 토큰 형식 검증 (성능용)
    public boolean isValidTokenFormat(String token) {
        if (!StringUtils.hasText(token)) return false; // Check if not empty / 비어있지 않은지 확인

        String[] parts = token.split("\\."); // Split by dots / 점으로 분할
        return parts.length == 3; // JWT should have 3 parts (header.payload.signature) / JWT는 3부분이어야 함 (헤더.페이로드.서명)
    }
}