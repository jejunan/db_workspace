package com.onion.backend.repository;

import com.onion.backend.entity.User; // User 엔티티 import
import org.springframework.data.jpa.repository.JpaRepository; // JPA 기능을 제공하는 인터페이스
import org.springframework.stereotype.Repository; // 이 인터페이스가 Repository임을 나타냄

import java.util.Optional; // null이 될 수 있는 값을 감싸는 래퍼 클래스

@Repository // Spring 컴포넌트 스캔에 의해 자동 등록되는 Repository 클래스임을 명시
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository<엔티티 타입, PK 타입>을 상속함
    // 기본적인 CRUD 메서드 자동 제공 (save, findById, delete 등)

    Optional<User> findByUsername(String username); // username으로 사용자 찾기 (Optional: null 안전하게 처리)

    Optional<User> findByEmail(String email); // email로 사용자 찾기
}
