package com.washiner.jwt_study.controller;

import com.washiner.jwt_study.dto.LoginRequest;
import com.washiner.jwt_study.dto.RegisterRequest;
import com.washiner.jwt_study.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// @RestController = esse controlador responde JSON.
// Você já conhece do CRUD.
@RestController

// @RequestMapping = prefixo de todas as rotas desse controller.
// Tudo aqui começa com /auth
// Lembra do SecurityConfig? /auth/** está no permitAll.
// Ou seja, essas rotas são públicas — sem token.
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ==========================================
    // REGISTER
    // ==========================================
    // POST /auth/register
    // Recebe os dados do usuário e retorna o token.
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            // @Valid = ativa as validações do RegisterRequest
            // (@NotBlank, @Email, @Size).
            // Se algum campo falhar, o Spring rejeita a requisição
            // automaticamente com 400 Bad Request.
            // @RequestBody = lê o JSON do corpo da requisição
            // e converte para RegisterRequest.
            @Valid @RequestBody RegisterRequest request) {

        String token = authService.register(request);

        // Map.of = cria um JSON simples: { "token": "eyJhbGci..." }
        // ResponseEntity.ok = retorna 200 com o corpo.
        return ResponseEntity.ok(Map.of("token", token));
    }

    // ==========================================
    // LOGIN
    // ==========================================
    // POST /auth/login
    // Recebe email e senha e retorna o token.
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request) {

        String token = authService.login(request);

        return ResponseEntity.ok(Map.of("token", token));
    }
}