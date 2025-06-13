package com.onion.backend.entity;

import jakarta.persistence.*; // JPA 관련 어노테이션 import
import lombok.Getter;        // getter 메서드 자동 생성
import lombok.NoArgsConstructor; // 기본 생성자 자동 생성
import lombok.Setter;        // setter 메서드 자동 생성
import org.springframework.data.annotation.CreatedDate;       // 생성일 자동 관리용 어노테이션
import org.springframework.data.annotation.LastModifiedDate;  // 수정일 자동 관리용 어노테이션

import java.time.LocalDateTime; // 날짜/시간 클래스

@Entity // JPA 엔티티 클래스임을 나타냄 (DB 테이블로 매핑됨)
@Getter // Lombok: 모든 필드에 대해 getter 생성
@Setter // Lombok: 모든 필드에 대해 setter 생성
@NoArgsConstructor // Lombok: 기본 생성자 생성
public class User {

    @Id // 기본 키(PK) 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가 전략 (MySQL 등에서 AUTO_INCREMENT)
    private Long id; // 사용자 고유 ID

    @Column(nullable = false) // null 불가능한 컬럼
    private String username; // 사용자 이름

    @Column(nullable = false)
    private String password; // 비밀번호 (암호화 필요)

    @Column(nullable = false)
    private String email; // 이메일 주소

    private LocalDateTime lastLogin; // 마지막 로그인 시간

    @CreatedDate // 엔티티 생성 시 자동으로 시간 기록
    @Column(insertable = true) // insert 가능하도록 설정
    private LocalDateTime createdDate; // 생성 시간

    @LastModifiedDate // 엔티티 수정 시 자동으로 시간 갱신
    private LocalDateTime updatedDate; // 마지막 수정 시간

    @PrePersist // DB에 저장되기 전(처음 insert 전)에 실행되는 메서드
    protected void onCreate() {
        this.createdDate = LocalDateTime.now(); // 현재 시간을 생성일로 설정
    }

    @PreUpdate // DB에 업데이트되기 전(update 전)에 실행되는 메서드
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now(); // 현재 시간을 수정일로 설정
    }
}
