package com.washiner.jwt_study.model;

import jakarta.persistence.*;
import lombok.*;

// NOVO: essas duas são do Spring Security
// UserDetails = o contrato que o Spring Security exige
// GrantedAuthority = representa uma permissão/papel do usuário (ex: ROLE_ADMIN)
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


// @Data = gera getters, setters, equals, hashCode e toString
// @Builder = permite criar objetos assim: User.builder().name("João").build()
// @NoArgsConstructor = construtor vazio, obrigatório para o JPA
// @AllArgsConstructor = construtor com todos os campos, necessário para o @Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
// implements UserDetails = aqui está o que você viu no primeiro projeto
// e não entendeu. Agora você sabe: é o contrato com o Spring Security.
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    // A senha vai chegar aqui já criptografada.
    // Nunca salvamos senha em texto puro no banco.
    // Quem vai criptografar é o BCryptPasswordEncoder — veremos isso no service.
    @Column(nullable = false)
    private String password;


    // ==========================================
    // MÉTODOS DO CONTRATO UserDetails
    // ==========================================
    // O Spring Security exige que você responda a essas perguntas.
    // São os métodos que a interface UserDetails obriga a implementar.

    // "Quais são as permissões desse usuário?"
    // Por enquanto retornamos uma lista vazia — não vamos trabalhar
    // com roles (ADMIN, USER) nesse projeto de estudo.
    // Em projetos reais aqui viria: List.of(new SimpleGrantedAuthority(role))
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of();
    }

    // "Qual é a senha desse usuário?"
    // O Spring Security usa esse método para verificar a senha.
    // Já temos o campo password acima, então só retornamos ele.
    @Override
    public String getPassword(){
        return password;
    }

    // "Qual é o username desse usuário?"
    // No Spring Security "username" é o identificador único.
    // No nosso sistema o identificador único é o email.
    // Por isso retornamos o email aqui — não o nome.
    @Override
    public String getUsername(){
        return email;
    }

    // Os três métodos abaixo controlam o status da conta.
    // Todos retornam true = conta sempre ativa e válida.
    // Em sistemas reais você implementaria lógica aqui:
    // ex: conta bloqueada após 5 tentativas de login erradas.

    // "A conta está expirada?"
    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    // "A conta está bloqueada?"
    @Override
    public boolean isAccountNonLocked(){
        return true;
    }

    // "As credenciais estão expiradas?"
    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    // "A conta está habilitada?"
    @Override
    public boolean isEnabled(){
        return true;
    }
}
