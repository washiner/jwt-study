package com.washiner.jwt_study.security;

import com.washiner.jwt_study.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// @Component = o Spring cria essa classe automaticamente
// e a registra como um componente disponível para injeção.
@Component

// OncePerRequestFilter = classe do Spring que garante que esse filtro
// vai rodar UMA vez por requisição — nunca duas vezes.
// É a base que todo filtro de autenticação estende no mercado.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JwtService = nosso serviço que valida e lê o token.
    // O filtro depende dele para saber se o token é válido.
    private final JwtService jwtService;

    // UserRepository = precisamos buscar o usuário no banco
    // depois de extrair o email do token.
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    // doFilterInternal = o método principal do filtro.
    // O Spring chama esse método em TODA requisição que chega.
    // request  = a requisição que chegou (headers, body, url)
    // response = a resposta que vai ser enviada
    // filterChain = a fila de filtros. Chamar chain.doFilter()
    //               significa "passa pro próximo da fila".
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Pega o header "Authorization" da requisição.
        // É aqui que o cliente manda o token:
        // Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
        String authHeader = request.getHeader("Authorization");

        // Se não tem header Authorization, ou não começa com "Bearer ",
        // essa requisição não tem token — pode ser o /login ou /register.
        // Passamos pra frente sem autenticar. O Spring Security vai
        // decidir se bloqueia ou não baseado na configuração do SecurityConfig.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Remove o prefixo "Bearer " para ficar só o token.
        // "Bearer eyJhbGci..." → "eyJhbGci..."
        // O 7 é o tamanho da String "Bearer " com o espaço.
        String token = authHeader.substring(7);

        // Extrai o email de dentro do token.
        // Se o token for inválido ou expirado, o JwtService
        // vai lançar uma exceção aqui e a requisição vai retornar 401.
        String email = jwtService.extractEmail(token);

        // Verifica se conseguimos extrair o email E se o usuário
        // ainda não está autenticado nessa requisição.
        // getAuthentication() == null significa que o Spring Security
        // ainda não sabe quem é o usuário dessa requisição.
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Busca o usuário no banco pelo email extraído do token.
            // Se não encontrar, o orElseThrow lança uma exceção.
            var user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            // Valida se o token pertence a esse usuário e não expirou.
            if (jwtService.isTokenValid(token, user)) {

                // UsernamePasswordAuthenticationToken = objeto que o Spring Security
                // usa para representar um usuário autenticado.
                // Parâmetros:
                // 1. user = o objeto do usuário (principal)
                // 2. null = credenciais (senha). Null porque já validamos o token.
                // 3. user.getAuthorities() = as permissões do usuário.
                var authToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

                // Adiciona detalhes da requisição no token de autenticação.
                // IP do cliente, session id, etc. Boa prática sempre incluir.
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // LINHA MAIS IMPORTANTE DO FILTRO:
                // Registra o usuário como autenticado no contexto de segurança.
                // É aqui que o Spring Security passa a saber quem é o usuário.
                // Depois dessa linha, qualquer controller pode chamar
                // SecurityContextHolder.getContext().getAuthentication()
                // e vai encontrar o usuário autenticado.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Passa a requisição para o próximo filtro da fila.
        // Sempre precisa chamar isso no final — se não chamar,
        // a requisição trava aqui e nunca chega no controller.
        filterChain.doFilter(request, response);
    }
}