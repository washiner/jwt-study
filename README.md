# JWT Study — Autenticação com Spring Boot e Spring Security

Projeto de estudo de autenticação e autorização usando JWT (JSON Web Token)
com Java, Spring Boot e Spring Security.

Construído do zero, com foco em entender o **porquê** de cada decisão,
não apenas o **como**.

---

## O que é JWT?

JWT (JSON Web Token) é um token gerado pelo servidor após o login do usuário.
Em vez de guardar sessão no servidor, o token carrega as informações do usuário
dentro dele mesmo — assinado com uma chave secreta para garantir que ninguém
alterou o conteúdo.

**Analogia:** É como uma pulseira de show. O segurança verifica seu documento
uma vez (login) e te dá a pulseira (token). Depois você usa a pulseira para
entrar em qualquer área sem precisar mostrar o documento de novo.

### Por que JWT em vez de sessão?

| Sessão tradicional | JWT |
|---|---|
| Servidor guarda estado | Servidor não guarda nada |
| Problema em múltiplos servidores | Funciona em qualquer servidor |
| Banco consultado em toda requisição | Token verificado em memória |

---

## Tecnologias

- Java 25
- Spring Boot 4.0.5
- Spring Security 7
- Spring Data JPA
- PostgreSQL 15 (via Docker)
- OAuth2 Resource Server (suporte nativo a JWT)
- Lombok
- Docker Compose

---

## Pré-requisitos

- JDK 25+
- Docker Desktop
- IntelliJ IDEA (recomendado)

---

## Como rodar o projeto

**1. Sobe o banco de dados:**
```bash
docker compose up -d
```

**2. Roda a aplicação pelo IntelliJ** (botão play na classe `JwtStudyApplication`)

**3. Confirma que subiu:**
```
Tomcat started on port 8080
Started JwtStudyApplication
```

A tabela `users` é criada automaticamente pelo Hibernate na primeira execução.

---

## Estrutura do projeto

```
src/main/java/com/washiner/jwt_study/
├── config/
│   └── SecurityConfig.java         ← regras de segurança, chaves RSA, beans
├── controller/
│   ├── AuthController.java         ← endpoints /auth/register e /auth/login
│   └── UserController.java         ← endpoint protegido /users/me
├── dto/
│   ├── RegisterRequest.java        ← dados que chegam no cadastro
│   └── LoginRequest.java           ← dados que chegam no login
├── model/
│   └── User.java                   ← entidade + contrato UserDetails
├── repository/
│   └── UserRepository.java         ← acesso ao banco
├── security/
│   ├── JwtService.java             ← gera, lê e valida tokens
│   └── JwtAuthenticationFilter.java ← porteiro de todas as requisições
└── service/
    └── AuthService.java            ← lógica de registro e login
```

---

## Fluxo de autenticação

### Login
```
POST /auth/login
      ↓
AuthController → AuthService → JwtService → retorna o token
```

### Requisição protegida
```
GET /users/me + token no header Authorization
      ↓
JwtAuthenticationFilter → JwtService → UserRepository → UserController
```

---

## Endpoints

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| POST | /auth/register | Não | Cadastra novo usuário |
| POST | /auth/login | Não | Loga e retorna o JWT |
| GET | /users/me | Sim | Retorna dados do usuário logado |

### Exemplo de uso

**Register:**
```json
POST /auth/register
{
  "nome": "Washiner",
  "email": "washiner@email.com",
  "password": "123456"
}
```

**Login:**
```json
POST /auth/login
{
  "email": "washiner@email.com",
  "password": "123456"
}
```

**Resposta do login:**
```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9..."
}
```

**Usando o token:**
```
GET /users/me
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

---

## Segurança

- Senhas armazenadas com hash BCrypt — nunca em texto puro
- Token assinado com par de chaves RSA 2048 bits
- Token expira em 24 horas
- Servidor não guarda estado — arquitetura stateless

---

## Docker Compose

```yaml
# Sobe o Postgres na porta 5432
# Dados persistidos em volume — não perdem ao reiniciar
docker compose up -d   # sobe
docker compose down    # para
docker compose ps      # verifica status
```

---

## Ordem de estudo recomendada

Se você está estudando esse projeto, siga essa ordem para entender
a dependência entre as classes:

1. `User.java` — entenda o contrato `UserDetails`
2. `UserRepository.java` — por que `findByEmail` e não `findById`
3. `JwtService.java` — como o token é gerado e validado
4. `JwtAuthenticationFilter.java` — o porteiro de toda requisição
5. `SecurityConfig.java` — como tudo se conecta ao Spring Security
6. `AuthService.java` — onde o login e registro acontecem
7. `AuthController.java` — os endpoints públicos
8. `UserController.java` — endpoint protegido

Consulte o arquivo `tome_nota.md` para o raciocínio detalhado de cada classe.
