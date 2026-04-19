package com.washiner.jwt_study.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Record para os dados de login.
// Mais simples que o RegisterRequest — só email e senha.
// Não tem @Size aqui porque na validação de login
// não faz sentido rejeitar uma senha curta —
// se a senha está errada o AuthService já cuida disso.
public record LoginRequest(

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        String password

) {}