package com.onion.backend.controller;

import com.onion.backend.dto.SignUpUser;
import com.onion.backend.entity.User;                  // 사용자 엔티티 클래스 import
import com.onion.backend.jwt.JwtUtil;
import com.onion.backend.service.CustomUserDetailsService;
import com.onion.backend.service.UserService;          // 사용자 서비스 클래스 import
import io.swagger.v3.oas.annotations.Parameter;        // Swagger 문서화를 위한 어노테이션

// REST API와 관련된 Spring 어노테이션
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired; // 의존성 주입을 위한 어노테이션
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;        // HTTP 응답을 감싸는 클래스
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@RestController // 해당 클래스가 REST API 컨트롤러임을 나타냄, JSON 형태로 응답이 반환
@RequestMapping("/api/users") // 헤당 컨트롤러의 기본 URL 경로는 '/api/users'
public class UserController {
    private final UserService userService; // 사용자 관련 비즈니스 로직을 담당하는 클래스
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Autowired // 생성자를 통해 userService가 의존성 주입
    public UserController(UserService userService, AuthenticationManager authenticationManager, JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;

    }

    @GetMapping("") // HTTP GET 요청을 /api/users에 보내면 모든 사용자의 리스트를 반환
    public ResponseEntity<List<User>> getUserS() {
        return ResponseEntity.ok(userService.getUsers()); // 응답은 200 OK 상태 코드와 함께 List<User> 형태로 반환
    }

    @PostMapping("/signUp") // HTTP POST 요청을 /api/users/signUp에 보내면 회원가입이 이루어짐
    public ResponseEntity<User> createUser(@RequestBody SignUpUser signUpUser) {
        User user = userService.createUser(signUpUser);
        return ResponseEntity.ok(user); // 회원가입된 사용자 객체를 응답
    }

    @DeleteMapping("/{userId}") // HTTP DELETE 요청을 /api/users/{userId}에 보내면 해당 사용자를 삭제
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to be deleted", required = true) @PathVariable Long userId) { // Swagger 문서에서 userId에 대한 설명을 추가하기 위해 @Parameter를 사용
        // 해당 userId에 해당하는 사용자 삭제
        userService.deleteUser(userId);
        // 삭제 후에는 204 No Content 상태 코드로 응답 (내용 없이 성공적으로 처리됨을 의미)
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpServletResponse response) throws AuthenticationException {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String token = jwtUtil.generateToken(userDetails.getUsername());
        Cookie cookie = new Cookie("onion_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60); // 1시간 유효

        response.addCookie(cookie);
        return token;
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("onion_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 쿠키 삭제
        response.addCookie(cookie);
    }

    @PostMapping("/token/validation")
    @ResponseStatus(HttpStatus.OK)
    public void jwtValidate(@RequestParam String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token is not validation");
        }
    }
}
