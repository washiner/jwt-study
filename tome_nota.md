# Tome Nota — Guia de Estudo JWT

Esse arquivo é seu manual pessoal. Escrito do jeito que você aprendeu,
não do jeito genérico de documentação.

Quando for treinar sozinho, leia esse arquivo antes de abrir qualquer classe.

---

## A mentalidade certa

Você não vai decorar o código. Você vai decorar o **raciocínio**.

O código você consulta. O raciocínio você carrega na cabeça.

Quando você souber responder "por que essa classe existe?", você sabe
escrever ela — mesmo que precise consultar a sintaxe.

---

## Por que JWT existe?

Antes do JWT o padrão era sessão no servidor: quando você fazia login,
o servidor criava um registro "usuário X está logado" em memória ou banco.

**Problema:** se você tem 5 servidores rodando, o usuário logado no
servidor 1 não estava logado no servidor 2.

**Solução JWT:** a informação fica dentro do token. Qualquer servidor
que tem a chave secreta consegue verificar o token.
Sem banco, sem sincronização, sem estado compartilhado.

Isso se chama **stateless** — sem estado no servidor.

---

## Estrutura do token JWT

Todo JWT tem 3 partes separadas por ponto:

```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ3YXNoaW5lckBlbWFpbC5jb20ifQ.ASSINATURA
      HEADER                        PAYLOAD                      SIGNATURE
```

- **Header:** algoritmo usado para assinar (RSA, HMAC, etc)
- **Payload:** informações do usuário (email, quando expira, quem emitiu)
- **Signature:** hash que garante que ninguém alterou o conteúdo

**Importante:** Header e Payload são só Base64 — qualquer um pode ler.
O que é secreto é a Signature. Se alguém alterar qualquer coisa,
a assinatura não bate e o token é rejeitado.

---

## Fluxo completo

### Login
```
POST /auth/login (email + senha)
         ↓
AuthController    ← recebe a requisição
         ↓
AuthService       ← valida email e senha no banco
         ↓
JwtService        ← gera o token assinado
         ↓
retorna o token para o cliente
```

### Requisição protegida
```
GET /users/me + "Authorization: Bearer [token]"
         ↓
JwtAuthenticationFilter   ← intercepta TODA requisição
         ↓
JwtService                ← valida o token
         ↓
UserRepository            ← busca o usuário pelo email do token
         ↓
SecurityConfig            ← confirma que está autenticado
         ↓
UserController            ← executa e retorna a resposta
```

**Quando não tem token:**
```
Requisição sem token
         ↓
JwtAuthenticationFilter   ← passa pra frente SEM autenticar
         ↓
SecurityConfig            ← rota precisa de autenticação?
         ↓
SIM → retorna 401         NÃO → libera (/auth/login, /auth/register)
```

---

## Cada classe e sua responsabilidade

---

### User.java — model/

**Por que existe:** representa o usuário no banco de dados E
faz o contrato com o Spring Security.

**A sacada principal:** ela `implements UserDetails`.

UserDetails é uma interface do Spring Security.
É um contrato — igual ao JpaRepository, mas para segurança.

Quando você implementa UserDetails, você está dizendo pro Spring Security:
*"Pode usar minha classe como usuário. Aqui estão as respostas
para suas perguntas."*

**Perguntas que o Spring Security faz (métodos obrigatórios):**

| Método | Pergunta | Nossa resposta |
|---|---|---|
| `getUsername()` | Qual o identificador único? | Retorna o email |
| `getPassword()` | Qual a senha? | Retorna o password |
| `getAuthorities()` | Quais as permissões? | Lista vazia por enquanto |
| `isAccountNonExpired()` | Conta expirada? | true (sempre ativa) |
| `isAccountNonLocked()` | Conta bloqueada? | true (nunca bloqueada) |
| `isCredentialsNonExpired()` | Credenciais expiradas? | true |
| `isEnabled()` | Conta habilitada? | true |

**Por que `getUsername()` retorna email e não nome?**
Porque username no Spring Security significa "identificador único".
No nosso sistema o identificador único é o email.

**Por que `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`?**
O `@Data` gera equals e hashCode baseado em TODOS os campos.
Com entidades JPA isso pode causar loop infinito em relacionamentos.
Solução: comparar objetos só pelo `id` — dois usuários são o mesmo
usuário se têm o mesmo id.

```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements UserDetails {

    @EqualsAndHashCode.Include  // só o id entra na comparação
    private Long id;
```

---

### UserRepository.java — repository/

**Por que existe:** acesso ao banco. Você já conhece do CRUD.

**O que é novo:** o método `findByEmail`.

No CRUD normal você buscava por id. Aqui precisamos buscar por email
porque email é o que o usuário digita na tela de login.

```java
Optional<User> findByEmail(String email);
```

**Por que `Optional`?**
O usuário pode ou não existir no banco.
`Optional` evita NullPointerException e força quem chama
a tratar o caso de usuário não encontrado.

O Spring Data lê o nome do método e gera o SQL sozinho:
```sql
SELECT * FROM users WHERE email = ?
```

---

### JwtService.java — security/

**Por que existe:** é a máquina que faz e verifica a pulseira.

Tem 3 responsabilidades:

**1. Gerar token** (`generateToken`)
- Recebe o usuário que acabou de fazer login
- Monta os claims (informações que vão dentro do token)
- Assina com a chave privada RSA
- Devolve o token como String

**2. Extrair email do token** (`extractEmail`)
- Recebe o token como String
- Verifica a assinatura
- Devolve o email que está no campo `subject`

**3. Validar token** (`isTokenValid`)
- Verifica se o email do token bate com o usuário
- Verifica se o token não expirou

**O que são claims?**
Claims são as informações dentro do token:
- `issuer` — quem emitiu ("jwt-study")
- `issuedAt` — quando foi gerado
- `expiresAt` — quando expira
- `subject` — o email do usuário

Qualquer um pode LER os claims — não são secretos.
O que é secreto é a assinatura.

**Por que `@Lazy` no construtor?**
Problema de referência circular:
- SecurityConfig precisa do JwtAuthenticationFilter
- JwtAuthenticationFilter precisa do JwtService
- JwtService precisa do JwtEncoder/JwtDecoder
- JwtEncoder/JwtDecoder são criados pelo SecurityConfig

`@Lazy` diz pro Spring: "não injeta agora, injeta quando o método
for chamado pela primeira vez." Isso quebra o ciclo.

---

### JwtAuthenticationFilter.java — security/

**Por que existe:** é o porteiro. Roda antes de qualquer controller.
Toda requisição — com token ou sem token — passa por ele primeiro.

**Estende `OncePerRequestFilter`:** garante que o filtro roda
UMA vez por requisição, nunca duas.

**O que ele faz em ordem:**

```
1. Pega o header "Authorization" da requisição
2. Não tem header ou não começa com "Bearer "?
   → passa pra frente sem autenticar (return)
3. Remove o "Bearer " e fica só com o token
4. Extrai o email do token via JwtService
5. Usuário ainda não autenticado nessa requisição?
   → busca o usuário no banco pelo email
   → valida o token
   → registra o usuário como autenticado no SecurityContext
6. Passa pra frente (filterChain.doFilter)
```

**Ponto crítico:** o filtro NÃO bloqueia quando não tem token.
Ele só deixa de autenticar. Quem bloqueia é o SecurityConfig.

**Por que `SecurityContextHolder`?**
É onde o Spring Security guarda "quem está autenticado nessa requisição".
Depois que você registra o usuário aqui, qualquer controller pode
chamar `SecurityContextHolder.getContext().getAuthentication()`
e encontrar o usuário.

---

### SecurityConfig.java — config/

**Por que existe:** é onde tudo se conecta.
Substitui o comportamento padrão do Spring Security
(tela de login) pelo nosso (validação de JWT).

**O que configura:**

**KeyPair (par de chaves RSA)**
- Gerado uma vez quando a aplicação sobe
- Chave privada: assina os tokens
- Chave pública: verifica os tokens
- Em produção: ficam em arquivo ou vault, não em memória

**JwtEncoder**
- Usa a chave privada para assinar tokens
- Injetado no JwtService

**JwtDecoder**
- Usa a chave pública para verificar tokens
- Injetado no JwtService

**PasswordEncoder (BCrypt)**
- Criptografa senhas antes de salvar no banco
- Nunca salvamos senha em texto puro
- Dois usuários com a mesma senha têm hashes diferentes

**AuthenticationManager**
- Usado pelo AuthService para autenticar email e senha no login
- O Spring já tem implementação pronta, só pedimos ela aqui

**SecurityFilterChain — as regras:**
```java
.csrf → desabilitado (APIs REST com JWT não precisam)
.authorizeHttpRequests:
    /auth/**  → permitAll (login e register são públicos)
    qualquer outra rota → authenticated (precisa de token)
.sessionManagement → STATELESS (sem sessão no servidor)
.addFilterBefore → nosso JwtAuthenticationFilter roda primeiro
```

---

## Erros que você vai encontrar ao treinar

### Circular reference
```
The dependencies of some of the beans form a cycle
```
**Causa:** SecurityConfig depende de algo que depende dele mesmo.
**Solução:** `@Lazy` no construtor do JwtService.

### Tabela não criada
**Causa:** faltou `@Entity` na classe User.
**Solução:** adicionar `@Entity` acima do `@Table`.

### 403 em toda requisição
**Causa:** Spring Security bloqueando tudo (comportamento padrão).
**Solução:** SecurityConfig com as rotas públicas configuradas.

### Token inválido
**Causa:** aplicação reiniciou e gerou novo par de chaves RSA.
Tokens antigos foram assinados com a chave antiga.
**Solução:** em desenvolvimento, fazer login de novo.
Em produção, usar chaves persistidas em arquivo.

---

## Ordem de criação para treinar sozinho

```
1. docker-compose.yml          → sobe o banco
2. application.properties      → configura banco e JWT
3. User.java                   → entidade + UserDetails
4. UserRepository.java         → findByEmail
5. JwtService.java             → gera e valida token
6. JwtAuthenticationFilter.java → porteiro das requisições
7. SecurityConfig.java         → conecta tudo ao Spring Security
8. RegisterRequest.java        → DTO do cadastro
9. LoginRequest.java           → DTO do login
10. AuthService.java           → lógica de registro e login
11. AuthController.java        → endpoints /auth/**
12. UserController.java        → endpoint protegido /users/me
```

**Regra:** nunca pule uma etapa. Cada classe depende das anteriores.

---

## Testando no Postman

### 1. Register
```
Método: POST
URL: http://localhost:8080/auth/register
Body (JSON):
{
  "nome": "Washiner",
  "email": "washiner@email.com",
  "password": "123456"
}
```

### 2. Login
```
Método: POST
URL: http://localhost:8080/auth/login
Body (JSON):
{
  "email": "washiner@email.com",
  "password": "123456"
}
Resposta: { "token": "eyJhbGci..." }
```

### 3. Rota protegida
```
Método: GET
URL: http://localhost:8080/users/me
Headers:
  Authorization: Bearer eyJhbGci...  (token do login)
```

### 4. Testando sem token (deve retornar 401)
```
Método: GET
URL: http://localhost:8080/users/me
(sem header Authorization)
Resposta esperada: 401 Unauthorized
```

---

## Conceitos para revisar se travar

**O que é stateless?**
O servidor não guarda nenhuma informação sobre quem está logado.
Cada requisição precisa trazer o token. O token carrega tudo.

**O que é BCrypt?**
Algoritmo de hash para senhas. Transforma "123456" em
"$2a$10$X9vH8...". Irreversível — não tem como descriptografar.

**O que é RSA?**
Algoritmo de criptografia com par de chaves.
Chave privada assina. Chave pública verifica.
Mais seguro que uma chave só (HMAC).

**O que é o SecurityContext?**
Memória temporária do Spring Security durante uma requisição.
Guarda quem está autenticado. Limpo após cada requisição.

**O que é Bearer?**
Prefixo padrão do cabeçalho Authorization para tokens JWT.
"Bearer" significa "portador" — quem tem o token, tem acesso.
