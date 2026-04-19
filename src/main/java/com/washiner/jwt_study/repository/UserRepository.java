package com.washiner.jwt_study.repository;

import com.washiner.jwt_study.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// @Repository = marca essa interface como um componente de acesso a dados.
// O Spring vai criar a implementação automaticamente em tempo de execução.
// Você já conhece isso do CRUD.
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Esse método é NOVO em relação ao CRUD comum.
    // No CRUD normal você buscava por id. Aqui precisamos
    // buscar por email — porque email é o que o usuário
    // digita na tela de login, não o id.
    //
    // Optional<User> = o usuário pode ou não existir no banco.
    // Se não existir, retorna Optional.empty() em vez de null.
    // Isso evita NullPointerException e força quem chama
    // a tratar o caso de usuário não encontrado.
    //
    // O Spring Data lê o nome do método e gera o SQL sozinho:
    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);
}