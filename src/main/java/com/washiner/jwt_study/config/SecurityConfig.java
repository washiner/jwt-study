package com.washiner.jwt_study.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.washiner.jwt_study.repository.UserRepository;
import com.washiner.jwt_study.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

// @Configuration = diz pro Spring que essa classe tem configurações.
// O Spring lê ela na inicialização e registra tudo que tiver @Bean.
@Configuration

// @EnableWebSecurity = ativa o sistema de segurança do Spring.
// Sem isso o Spring Security não funciona mesmo que esteja no pom.xml.
@EnableWebSecurity
public class SecurityConfig {

    // O filtro que criamos — o Spring vai injetá-lo aqui.
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // ==========================================
    // PAR DE CHAVES RSA
    // ==========================================
    // @Bean = o Spring cria esse objeto uma vez e guarda.
    // Qualquer classe que precisar pode pedir injeção.
    // KeyPair = um par de chaves RSA: uma pública e uma privada.
    // Gerado uma vez quando a aplicação sobe.
    // ATENÇÃO: em produção as chaves ficam em arquivo ou vault,
    // não geradas em memória — porque se o servidor reiniciar,
    // todos os tokens anteriores ficam inválidos.
    // Para estudo, gerar em memória é suficiente.
    @Bean
    public KeyPair keyPair() {
        try {
            // KeyPairGenerator = gerador de par de chaves do Java.
            // RSA = o algoritmo. 2048 = tamanho em bits da chave.
            // Quanto maior o número, mais seguro e mais lento.
            // 2048 é o mínimo aceito pelo mercado hoje.
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar par de chaves RSA", e);
        }
    }

    // ==========================================
    // JWT ENCODER — GERA TOKENS
    // ==========================================
    // Recebe o KeyPair que criamos acima (injeção automática pelo Spring).
    // Constrói o JwtEncoder que o JwtService vai usar para assinar tokens.
    @Bean
    public JwtEncoder jwtEncoder(KeyPair keyPair) {

        // RSAKey = representa o par de chaves no formato que o
        // Nimbus (biblioteca JWT) entende.
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();

        // JWKSet = conjunto de chaves. Pode ter várias chaves,
        // mas aqui usamos só uma.
        // ImmutableJWKSet = versão imutável do conjunto — boa prática.
        var jwkSet = new ImmutableJWKSet<>(new JWKSet(rsaKey));

        // NimbusJwtEncoder = implementação do JwtEncoder que usa
        // a biblioteca Nimbus por baixo. É o padrão do Spring.
        return new NimbusJwtEncoder(jwkSet);
    }

    // ==========================================
    // JWT DECODER — LÊ E VALIDA TOKENS
    // ==========================================
    // Recebe o KeyPair e constrói o JwtDecoder.
    // O decoder só precisa da chave PÚBLICA para verificar tokens —
    // ele não precisa da privada porque não assina, só verifica.
    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair) {
        return NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) keyPair.getPublic())
                .build();
    }

    // ==========================================
    // PASSWORD ENCODER
    // ==========================================
    // BCrypt = algoritmo de hash para senhas.
    // Quando o usuário cadastra a senha "123456", salvamos
    // o hash dela no banco: "$2a$10$X9vH8..."
    // Nunca a senha em texto puro.
    // O BCrypt é inteligente: mesmo que dois usuários tenham
    // a mesma senha, os hashes são diferentes.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==========================================
    // AUTHENTICATION MANAGER
    // ==========================================
    // AuthenticationManager = o cara que o Spring Security usa
    // para autenticar usuário e senha na hora do login.
    // Precisamos dele no AuthService para processar o login.
    // O Spring já tem uma implementação pronta — só pedimos ela aqui.
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // UserDetailsService = diz pro Spring Security como buscar
// o usuário pelo username (email no nosso caso).
// O AuthenticationManager usa isso por baixo dos panos
// quando você chama authenticationManager.authenticate().
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    // ==========================================
    // SECURITY FILTER CHAIN
    // ==========================================
    // Essa é a configuração principal do Spring Security.
    // Aqui a gente define as regras: quais rotas são públicas,
    // quais precisam de autenticação, e como funciona a sessão.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desabilita o CSRF — Cross Site Request Forgery.
                // CSRF é uma proteção para aplicações com formulários HTML.
                // Em APIs REST com JWT não precisamos porque o token
                // já protege contra esse tipo de ataque.
                .csrf(AbstractHttpConfigurer::disable)

                // Define as regras de autorização por rota.
                .authorizeHttpRequests(auth -> auth

                        // Rotas públicas — qualquer um pode acessar sem token.
                        // /auth/** = tudo dentro de /auth: /auth/login, /auth/register
                        .requestMatchers("/auth/**").permitAll()

                        // Qualquer outra rota que não seja /auth/**
                        // exige autenticação — precisa de token válido.
                        .anyRequest().authenticated()
                )

                // Define que a sessão é STATELESS — sem estado.
                // O servidor não guarda sessão de ninguém.
                // Cada requisição precisa trazer o token.
                // É isso que torna o JWT stateless.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Registra nosso filtro JWT na cadeia de filtros do Spring.
                // addFilterBefore = adiciona o nosso filtro ANTES do filtro
                // padrão de autenticação por usuário e senha.
                // Assim o nosso filtro roda primeiro em toda requisição.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
