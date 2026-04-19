package com.washiner.jwt_study.service;

import com.washiner.jwt_study.dto.LoginRequest;
import com.washiner.jwt_study.dto.RegisterRequest;
import com.washiner.jwt_study.model.User;
import com.washiner.jwt_study.repository.UserRepository;
import com.washiner.jwt_study.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// @Service = o Spring cria essa classe e injeta onde for necessário.
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    // ==========================================
    // REGISTER
    // ==========================================
    // Recebe os dados do RegisterRequest, cria o usuário
    // no banco e já retorna o token JWT.
    // Assim o usuário não precisa fazer login após o cadastro.
    public String register(RegisterRequest request) {

        // Constrói o objeto User usando o builder do Lombok.
        // Note que a senha passa pelo passwordEncoder antes de salvar.
        // passwordEncoder.encode() = transforma "123456" em "$2a$10$X9..."
        // Nunca salvamos senha em texto puro no banco.
        User user = User.builder()
                .nome(request.nome())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        // Salva o usuário no banco.
        // O Hibernate gera o INSERT automaticamente.
        userRepository.save(user);

        // Gera e retorna o token JWT para o usuário recém cadastrado.
        return jwtService.generateToken(user);
    }

    // ==========================================
    // LOGIN
    // ==========================================
    // Recebe email e senha, autentica o usuário
    // e retorna o token JWT.
    public String login(LoginRequest request) {

        // AuthenticationManager = o Spring Security autentica
        // email e senha aqui. Por baixo dos panos ele:
        // 1. Busca o usuário pelo email (via UserDetails)
        // 2. Compara a senha enviada com o hash no banco (BCrypt)
        // 3. Se errado, lança BadCredentialsException automaticamente
        // 4. Se certo, retorna o objeto Authentication
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Se chegou até aqui, as credenciais são válidas.
        // Busca o usuário completo no banco pelo email.
        // orElseThrow = se não encontrar, lança exceção.
        // Na prática isso não acontece porque o authenticate
        // acima já verificou que o usuário existe.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Gera e retorna o token JWT.
        return jwtService.generateToken(user);
    }
}