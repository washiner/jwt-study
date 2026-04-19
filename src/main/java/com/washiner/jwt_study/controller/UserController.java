package com.washiner.jwt_study.controller;

import com.washiner.jwt_study.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Esse controller tem as rotas protegidas.
// Lembra do SecurityConfig? anyRequest().authenticated()
// Qualquer rota que não seja /auth/** cai aqui
// e precisa de token válido.
@RestController
@RequestMapping("/users")
public class UserController {

    // ==========================================
    // ME
    // ==========================================
    // GET /users/me
    // Retorna os dados do usuário autenticado.
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(

            // @AuthenticationPrincipal = pega o usuário autenticado
            // direto do SecurityContext.
            // Lembra do JwtAuthenticationFilter? Ele registrou o usuário
            // no SecurityContext depois de validar o token.
            // Aqui a gente só pega o que já está lá.
            // O Spring injeta o User automaticamente — você não
            // precisa buscar no banco de novo.
            @AuthenticationPrincipal User user) {

        // Retorna um JSON com os dados do usuário logado.
        return ResponseEntity.ok(Map.of(
                "id", user.getId().toString(),
                "nome", user.getNome(),
                "email", user.getEmail()
        ));
    }
}
