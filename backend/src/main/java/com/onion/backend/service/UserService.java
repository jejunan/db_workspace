package com.onion.backend.service;

import com.onion.backend.dto.SignUpUser;
import com.onion.backend.entity.User; // 사용자 엔티티 클래스 import
import com.onion.backend.repository.UserRepository; // 사용자 리포지토리 인터페이스 import
import org.springframework.beans.factory.annotation.Autowired; // 의존성 주입을 위한 어노테이션
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service; // 서비스 컴포넌트임을 나타냄

import java.time.LocalDateTime; // 날짜/시간 처리 클래스
import java.util.List; // 리스트 자료형 import

@Service // 해당 클래스가 서비스 계층의 컴포넌트임을 명시 (스프링 빈으로 등록됨)
public class UserService {

    private final UserRepository userRepository; // 사용자 DB 작업을 위한 Repository
    private final PasswordEncoder passwordEncoder;

    @Autowired // 생성자 기반 의존성 주입
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 사용자 생성 로직 (회원가입 등에서 사용)
    public User createUser(SignUpUser signUpUser) {
        User user = new User(); // 새 사용자 객체 생성
        user.setUsername(signUpUser.getUsername()); // 사용자 이름 설정
        user.setPassword(passwordEncoder.encode(signUpUser.getPassword())); // 비밀번호 설정
        user.setEmail(signUpUser.getEmail());       // 이메일 설정
        return userRepository.save(user); // 사용자 정보를 DB에 저장 후 반환
    }

    // 사용자 삭제 로직
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId); // 주어진 ID의 사용자 삭제
    }

    // 모든 사용자 목록 조회
    public List<User> getUsers() {
        return userRepository.findAll(); // 전체 사용자 리스트 반환
    }
}
