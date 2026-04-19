package com.washiner.jwt_study.security;

import com.washiner.jwt_study.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

// @Service = marca essa classe como um serviço.
// O Spring cria ela automaticamente e injeta onde for necessário.
// Você já conhece isso do CRUD.
@Service
public class JwtService {

    // JwtEncoder = responsável por GERAR tokens.
    // Ele pega as informações do usuário, assina com a chave
    // secreta e devolve o token em formato String.
    // O Spring vai criar e injetar essa implementação automaticamente
    // quando a gente configurar o SecurityConfig mais adiante.
    private final JwtEncoder jwtEncoder;

    // JwtDecoder = responsável por LER e VALIDAR tokens.
    // Ele recebe o token em String, verifica a assinatura
    // e devolve as informações que estão dentro dele.
    // Também será configurado e injetado pelo SecurityConfig.
    private final JwtDecoder jwtDecoder;

    // @Value = lê o valor do application.properties.
    // O Spring pega o valor de jwt.expiration e injeta aqui.
    // Assim não precisamos colocar o número hardcoded no código.
    @Value("${jwt.expiration}")
    private long expiration;

    // Construtor com injeção de dependência.
    // @Lazy nos parâmetros diz pro Spring:
    // "não injeta esses dois na hora de criar o contexto.
    // injeta só quando o método for chamado pela primeira vez."
    // Isso quebra o ciclo porque o SecurityConfig termina de ser
    // criado antes de o JwtService precisar do JwtEncoder/JwtDecoder.
    public JwtService(
            @Lazy JwtEncoder jwtEncoder,
            @Lazy JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

    // ==========================================
    // GERAR TOKEN
    // ==========================================
    // Recebe o usuário que acabou de fazer login
    // e devolve o token JWT como String.
    public String generateToken(User user) {

        // Instant.now() = momento exato em que o token está sendo gerado.
        Instant now = Instant.now();

        // JwtClaimsSet = o "conteúdo" do token, chamado de claims.
        // Claims são as informações que ficam dentro do token.
        // Qualquer um pode LER essas informações — elas não são secretas.
        // O que é secreto é a ASSINATURA, que garante que ninguém
        // alterou o conteúdo depois que o servidor gerou.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                // issuer = quem emitiu o token. Identifica seu sistema.
                .issuer("jwt-study")
                // issuedAt = quando o token foi gerado.
                .issuedAt(now)
                // expiresAt = quando o token expira.
                // now + expiration (em segundos) = tempo de validade.
                .expiresAt(now.plusSeconds(expiration / 1000))
                // subject = o identificador do usuário dentro do token.
                // Usamos o email porque é o username no nosso sistema.
                .subject(user.getUsername())
                // build() = monta o objeto com tudo configurado acima.
                .build();

        // JwtEncoderParameters = empacota os claims para o encoder processar.
        // O encoder pega isso, assina com a chave secreta e gera a String do token.
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    // ==========================================
    // EXTRAIR EMAIL DO TOKEN
    // ==========================================
    // Recebe o token como String e devolve o email
    // do usuário que está dentro dele.
    public String extractEmail(String token) {

        // jwtDecoder.decode() = abre o token e verifica a assinatura.
        // Se alguém alterou o token, aqui vai lançar uma exceção.
        // Se o token expirou, aqui também vai lançar uma exceção.
        // getSubject() = pega o campo "subject" que a gente definiu
        // no generateToken como o email do usuário.
        return jwtDecoder.decode(token).getSubject();
    }

    // ==========================================
    // VALIDAR TOKEN
    // ==========================================
    // Recebe o token e o usuário e verifica se
    // o token pertence a esse usuário e está válido.
    public boolean isTokenValid(String token, User user) {

        // Extrai o email de dentro do token.
        String email = extractEmail(token);

        // Verifica duas coisas:
        // 1. O email dentro do token é o mesmo do usuário?
        // 2. O token não está expirado?
        // Se as duas forem verdade, o token é válido.
        return email.equals(user.getUsername()) && !isTokenExpired(token);
    }

    // ==========================================
    // VERIFICAR SE O TOKEN EXPIROU
    // ==========================================
    // Método privado — só usado internamente nessa classe.
    private boolean isTokenExpired(String token) {

        // Abre o token, pega a data de expiração
        // e verifica se ela já passou do momento atual.
        return jwtDecoder.decode(token)
                .getExpiresAt()
                .isBefore(Instant.now());
    }
}