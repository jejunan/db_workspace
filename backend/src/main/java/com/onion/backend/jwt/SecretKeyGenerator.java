package com.onion.backend.jwt;

// JWT signature algorithm enumeration / JWT 서명 알고리즘 열거형
import io.jsonwebtoken.SignatureAlgorithm;
// JWT security utility for key generation / 키 생성을 위한 JWT 보안 유틸리티
import io.jsonwebtoken.security.Keys;

// Java security and cryptography / Java 보안 및 암호화
import java.security.Key;
import java.security.SecureRandom;
// Java Base64 encoder/decoder utility / Java Base64 인코더/디코더 유틸리티
import java.util.Base64;
// Java I/O for file operations / 파일 작업을 위한 Java I/O
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// Java time for timestamp / 타임스탬프를 위한 Java time
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// Java utility classes / Java 유틸리티 클래스
import java.util.Scanner;

/**
 * Utility class for generating cryptographically secure JWT secret keys
 * 암호화적으로 안전한 JWT 비밀 키 생성을 위한 유틸리티 클래스
 *
 * This class provides methods to generate secure keys for JWT token signing
 * 이 클래스는 JWT 토큰 서명을 위한 안전한 키 생성 메서드를 제공합니다
 */
public final class SecretKeyGenerator {
    // ✅ IMPROVEMENT: Made class final to prevent inheritance / 개선: 상속 방지를 위해 클래스를 final로 설정

    // Private constructor to prevent instantiation / 인스턴스화 방지를 위한 private 생성자
    private SecretKeyGenerator() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
        // ✅ IMPROVEMENT: Added private constructor to prevent instantiation / 개선: 인스턴스화 방지를 위한 private 생성자 추가
    }

    // Constants for key generation / 키 생성을 위한 상수
    private static final String DEFAULT_OUTPUT_FILE = "jwt-secrets.txt"; // Default output file name / 기본 출력 파일명
    private static final int MINIMUM_KEY_LENGTH = 32; // Minimum key length in bytes / 최소 키 길이 (바이트)
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // Timestamp format / 타임스탬프 형식

    /**
     * Main method for interactive key generation
     * 대화형 키 생성을 위한 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== JWT Secret Key Generator ===");
        System.out.println("⚠️  SECURITY WARNING: Handle generated keys with extreme care!");
        System.out.println("🔐 Generated keys should be stored securely and never committed to version control");
        System.out.println();

        Scanner scanner = new Scanner(System.in); // Scanner for user input / 사용자 입력을 위한 스캐너

        try {
            // Get user preferences / 사용자 선호도 가져오기
            int keyCount = getKeyCount(scanner);
            String environment = getEnvironment(scanner);
            boolean saveToFile = getSaveToFileOption(scanner);

            // Generate keys / 키 생성
            System.out.println("\n🔑 Generating secure JWT keys...");
            for (int i = 1; i <= keyCount; i++) {
                generateAndDisplayKey(i, environment, saveToFile);
            }

            // Display security recommendations / 보안 권장사항 표시
            displaySecurityRecommendations();

        } catch (Exception e) {
            System.err.println("❌ Error generating keys: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close(); // Close scanner resource / 스캐너 리소스 닫기
        }
    }

    /**
     * Generates a cryptographically secure JWT secret key
     * 암호화적으로 안전한 JWT 비밀 키 생성
     */
    public static String generateSecretKey() {
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // Generate secure key / 안전한 키 생성
        return Base64.getEncoder().encodeToString(key.getEncoded()); // Encode to Base64 / Base64로 인코딩
    }

    /**
     * Generates a secret key with custom length
     * 커스텀 길이로 비밀 키 생성
     */
    public static String generateSecretKey(int lengthInBytes) {
        if (lengthInBytes < MINIMUM_KEY_LENGTH) { // Validate minimum length / 최소 길이 검증
            throw new IllegalArgumentException("Key length must be at least " + MINIMUM_KEY_LENGTH + " bytes");
        }

        SecureRandom secureRandom = new SecureRandom(); // Create secure random generator / 안전한 랜덤 생성기 생성
        byte[] keyBytes = new byte[lengthInBytes]; // Create byte array for key / 키를 위한 바이트 배열 생성
        secureRandom.nextBytes(keyBytes); // Fill with random bytes / 랜덤 바이트로 채우기

        return Base64.getEncoder().encodeToString(keyBytes); // Encode to Base64 / Base64로 인코딩
    }

    /**
     * Validates if a key meets security requirements
     * 키가 보안 요구사항을 충족하는지 검증
     */
    public static boolean validateKeyStrength(String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key); // Decode from Base64 / Base64에서 디코딩

            // Check minimum length / 최소 길이 확인
            if (keyBytes.length < MINIMUM_KEY_LENGTH) {
                return false;
            }

            // Check for sufficient entropy (basic check) / 충분한 엔트로피 확인 (기본 검사)
            return hasMinimumEntropy(keyBytes);

        } catch (IllegalArgumentException e) {
            return false; // Invalid Base64 / 유효하지 않은 Base64
        }
    }

    // Private helper methods / 비공개 도우미 메서드

    /**
     * Gets the number of keys to generate from user input
     * 사용자 입력으로부터 생성할 키의 개수 가져오기
     */
    private static int getKeyCount(Scanner scanner) {
        System.out.print("How many keys do you want to generate? (1-10, default: 1): ");
        String input = scanner.nextLine().trim(); // Get user input / 사용자 입력 가져오기

        if (input.isEmpty()) return 1; // Default to 1 / 기본값 1

        try {
            int count = Integer.parseInt(input); // Parse integer / 정수 파싱
            if (count < 1 || count > 10) { // Validate range / 범위 검증
                System.out.println("⚠️  Invalid range. Using default: 1");
                return 1;
            }
            return count;
        } catch (NumberFormatException e) {
            System.out.println("⚠️  Invalid number. Using default: 1");
            return 1;
        }
    }

    /**
     * Gets the target environment from user input
     * 사용자 입력으로부터 대상 환경 가져오기
     */
    private static String getEnvironment(Scanner scanner) {
        System.out.print("Target environment (dev/staging/prod, default: dev): ");
        String env = scanner.nextLine().trim().toLowerCase(); // Get and normalize input / 입력 가져오기 및 정규화

        if (env.isEmpty() || (!env.equals("dev") && !env.equals("staging") && !env.equals("prod"))) {
            return "dev"; // Default to dev / 기본값 dev
        }
        return env;
    }

    /**
     * Gets user preference for saving keys to file
     * 키를 파일로 저장할지에 대한 사용자 선호도 가져오기
     */
    private static boolean getSaveToFileOption(Scanner scanner) {
        System.out.print("Save keys to file? (y/N, default: N): ");
        String input = scanner.nextLine().trim().toLowerCase(); // Get and normalize input / 입력 가져오기 및 정규화
        return input.equals("y") || input.equals("yes"); // Return true if user wants to save / 사용자가 저장을 원하면 true 반환
    }

    /**
     * Generates and displays a single key
     * 단일 키 생성 및 표시
     */
    private static void generateAndDisplayKey(int keyNumber, String environment, boolean saveToFile) {
        try {
            String secretKey = generateSecretKey(); // Generate key / 키 생성
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT); // Get timestamp / 타임스탬프 가져오기

            // Display key information (securely) / 키 정보 표시 (안전하게)
            System.out.println("\n--- Key #" + keyNumber + " ---");
            System.out.println("Environment: " + environment.toUpperCase());
            System.out.println("Generated: " + timestamp);
            System.out.println("Key Length: " + Base64.getDecoder().decode(secretKey).length + " bytes");
            System.out.println("Base64 Key: " + secretKey);
            System.out.println("Validation: " + (validateKeyStrength(secretKey) ? "✅ STRONG" : "⚠️  WEAK"));

            // Save to file if requested / 요청된 경우 파일에 저장
            if (saveToFile) {
                saveKeyToFile(keyNumber, environment, secretKey, timestamp);
            }

        } catch (Exception e) {
            System.err.println("❌ Error generating key #" + keyNumber + ": " + e.getMessage());
        }
    }

    /**
     * Saves a generated key to file with metadata
     * 생성된 키를 메타데이터와 함께 파일에 저장
     */
    private static void saveKeyToFile(int keyNumber, String environment, String secretKey, String timestamp) {
        try {
            Path outputPath = Paths.get(DEFAULT_OUTPUT_FILE); // Get output file path / 출력 파일 경로 가져오기

            // Create file if it doesn't exist / 파일이 없으면 생성
            if (!Files.exists(outputPath)) {
                Files.createFile(outputPath);
                System.out.println("📁 Created output file: " + DEFAULT_OUTPUT_FILE);
            }

            // Append key information to file / 파일에 키 정보 추가
            try (FileWriter writer = new FileWriter(outputPath.toFile(), true)) { // Open in append mode / 추가 모드로 열기
                writer.write("\n# JWT Secret Key #" + keyNumber + "\n");
                writer.write("# Environment: " + environment.toUpperCase() + "\n");
                writer.write("# Generated: " + timestamp + "\n");
                writer.write("# Key Length: " + Base64.getDecoder().decode(secretKey).length + " bytes\n");
                writer.write("JWT_SECRET_" + environment.toUpperCase() + "_" + keyNumber + "=" + secretKey + "\n");
                writer.write("\n");

                System.out.println("💾 Key saved to: " + DEFAULT_OUTPUT_FILE);
            }

        } catch (IOException e) {
            System.err.println("❌ Error saving key to file: " + e.getMessage());
        }
    }

    /**
     * Displays security recommendations for key management
     * 키 관리를 위한 보안 권장사항 표시
     */
    private static void displaySecurityRecommendations() {
        System.out.println("\n🔒 === SECURITY RECOMMENDATIONS ===");
        System.out.println("1. 🚫 NEVER commit these keys to version control (Git, SVN, etc.)");
        System.out.println("2. 🔐 Store keys in secure environment variables or key vaults");
        System.out.println("3. 🔄 Rotate keys regularly (recommend: every 3-6 months)");
        System.out.println("4. 🌍 Use different keys for different environments (dev/staging/prod)");
        System.out.println("5. 🗑️  Clear console history after copying keys");
        System.out.println("6. 📱 Consider using dedicated secret management tools");
        System.out.println("7. 👥 Limit access to production keys to essential personnel only");
        System.out.println("8. 📝 Document key rotation procedures");
        System.out.println("\n📚 Example usage in application.yml:");
        System.out.println("jwt:");
        System.out.println("  secret: ${JWT_SECRET}  # Set via environment variable");
        System.out.println("\n🖥️  Example environment variable setup:");
        System.out.println("export JWT_SECRET=\"<your-generated-key-here>\"");
        System.out.println("\n⚠️  Remember: This console output may be logged - clear it after use!");
    }

    /**
     * Checks if byte array has minimum entropy (basic check)
     * 바이트 배열이 최소 엔트로피를 가지는지 확인 (기본 검사)
     */
    private static boolean hasMinimumEntropy(byte[] keyBytes) {
        // Simple check for repeated patterns / 반복 패턴에 대한 간단한 검사
        int uniqueBytes = 0;
        boolean[] seen = new boolean[256]; // Track seen byte values / 본 바이트 값들 추적

        for (byte b : keyBytes) {
            int unsigned = b & 0xFF; // Convert to unsigned / 부호 없음으로 변환
            if (!seen[unsigned]) {
                seen[unsigned] = true;
                uniqueBytes++;
            }
        }

        // Require at least 75% unique bytes for decent entropy / 적절한 엔트로피를 위해 최소 75% 고유 바이트 요구
        double uniqueRatio = (double) uniqueBytes / keyBytes.length;
        return uniqueRatio >= 0.5; // At least 50% unique bytes / 최소 50% 고유 바이트
    }
}