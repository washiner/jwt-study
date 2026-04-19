package com.washiner.jwt_study.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Record = tipo especial do Java para objetos imutáveis de dados.
// Perfeito para DTOs porque:
// - gera construtor, getters, equals, hashCode e toString automaticamente
// - imutável por padrão — ninguém altera os dados depois de criado
// - muito menos código que uma classe normal
//
// Os campos declarados aqui (nome, email, password) viram
// automaticamente private final — sem setter, sem mutação.
public record RegisterRequest(

        // @NotBlank = não aceita null, vazio ou só espaços
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        // @Email = valida se o formato é um email válido
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        // @Size = define tamanho mínimo
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String password

) {}
