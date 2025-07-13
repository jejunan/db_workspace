// Request DTOs / 요청 DTO들

// User registration request DTO / 사용자 등록 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDTO {
    @NotBlank(message = "Username is required") // Username validation / 사용자명 검증
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters") // Length validation / 길이 검증
    private String username;

    @NotBlank(message = "Password is required") // Password validation / 비밀번호 검증
    @Size(min = 8, message = "Password must be at least 8 characters") // Minimum length / 최소 길이
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]",
            message = "Password must contain at least one uppercase, lowercase, digit, and special character")
    // Password strength validation / 비밀번호 강도 검증
    private String password;

    @NotBlank(message = "Email is required") // Email validation / 이메일 검증
    @Email(message = "Invalid email format") // Email format validation / 이메일 형식 검증
    private String email;
}

// User login request DTO / 사용자 로그인 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
    @NotBlank(message = "Username is required") // Username validation / 사용자명 검증
    private String username;

    @NotBlank(message = "Password is required") // Password validation / 비밀번호 검증
    private String password;
}

// Profile update request DTO / 프로필 업데이트 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDTO {
    @Email(message = "Invalid email format") // Email format validation / 이메일 형식 검증
    private String email;

    @Size(max = 100, message = "First name cannot exceed 100 characters") // Length validation / 길이 검증
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters") // Length validation / 길이 검증
    private String lastName;

    @Size(max = 500, message = "Bio cannot exceed 500 characters") // Length validation / 길이 검증
    private String bio;
}

// Password change request DTO / 비밀번호 변경 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequestDTO {
    @NotBlank(message = "Current password is required") // Current password validation / 현재 비밀번호 검증
    private String currentPassword;

    @NotBlank(message = "New password is required") // New password validation / 새 비밀번호 검증
    @Size(min = 8, message = "Password must be at least 8 characters") // Minimum length / 최소 길이
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]",
            message = "Password must contain at least one uppercase, lowercase, digit, and special character")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required") // Confirmation validation / 확인 검증
    private String confirmPassword;

    // Custom validation to ensure passwords match / 비밀번호 일치를 보장하는 커스텀 검증
    @AssertTrue(message = "New password and confirmation must match")
    public boolean isPasswordsMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}

// Response DTOs / 응답 DTO들

// User response DTO (without sensitive data) / 사용자 응답 DTO (민감한 데이터 제외)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id; // User ID / 사용자 ID
    private String username; // Username / 사용자명
    private String email; // Email address / 이메일 주소
    private String firstName; // First name / 이름
    private String lastName; // Last name / 성
    private LocalDateTime createdDate; // Account creation date / 계정 생성일
    private LocalDateTime lastLogin; // Last login timestamp / 마지막 로그인 시간
    private boolean isActive; // Account status / 계정 상태
    // Note: Password is intentionally excluded for security / 참고: 보안을 위해 비밀번호는 의도적으로 제외됨
}

// Login response DTO / 로그인 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String token; // JWT token (can be null for failed login) / JWT 토큰 (로그인 실패 시 null 가능)
    private String message; // Response message / 응답 메시지
    private String username; // Logged in username / 로그인된 사용자명
    private LocalDateTime loginTime; // Login timestamp / 로그인 시간

    // Constructor for successful login / 성공적인 로그인을 위한 생성자
    public LoginResponseDTO(String token, String message, String username) {
        this.token = token;
        this.message = message;
        this.username = username;
        this.loginTime = LocalDateTime.now();
    }
}

// Logout response DTO / 로그아웃 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutResponseDTO {
    private String message; // Logout message / 로그아웃 메시지
    private String status; // Logout status / 로그아웃 상태
    private LocalDateTime logoutTime; // Logout timestamp / 로그아웃 시간

    // Constructor with automatic timestamp / 자동 타임스탬프가 포함된 생성자
    public LogoutResponseDTO(String message, String status) {
        this.message = message;
        this.status = status;
        this.logoutTime = LocalDateTime.now();
    }
}

// Token refresh response DTO / 토큰 갱신 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponseDTO {
    private String newToken; // New JWT token / 새로운 JWT 토큰
    private String message; // Response message / 응답 메시지
    private LocalDateTime refreshTime; // Refresh timestamp / 갱신 시간

    // Constructor with automatic timestamp / 자동 타임스탬프가 포함된 생성자
    public TokenRefreshResponseDTO(String newToken, String message) {
        this.newToken = newToken;
        this.message = message;
        this.refreshTime = LocalDateTime.now();
    }
}

// User profile DTO / 사용자 프로필 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id; // User ID / 사용자 ID
    private String username; // Username / 사용자명
    private String email; // Email address / 이메일 주소
    private String firstName; // First name / 이름
    private String lastName; // Last name / 성
    private String bio; // User biography / 사용자 소개
    private LocalDateTime createdDate; // Account creation date / 계정 생성일
    private LocalDateTime lastLogin; // Last login timestamp / 마지막 로그인 시간
    private LocalDateTime updatedDate; // Last profile update / 마지막 프로필 업데이트
}

// Password change response DTO / 비밀번호 변경 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeResponseDTO {
    private String message; // Response message / 응답 메시지
    private LocalDateTime changeTime; // Password change timestamp / 비밀번호 변경 시간

    // Constructor with automatic timestamp / 자동 타임스탬프가 포함된 생성자
    public PasswordChangeResponseDTO(String message) {
        this.message = message;
        this.changeTime = LocalDateTime.now();
    }
}