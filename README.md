# 💸 PicPay Simplificado

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://docs.docker.com/compose/)

API REST de uma plataforma de pagamentos simplificada, inspirada no [desafio técnico do PicPay](https://github.com/PicPay/picpay-desafio-backend).

## 📋 Sobre o Projeto

O objetivo é criar uma API que permita depositar e transferir dinheiro entre usuários, aplicando conceitos avançados de backend como transações ACID, integrações externas, mensageria assíncrona e tratamento de concorrência.

### Regras de Negócio

- **Dois tipos de usuários**: Comuns e Lojistas
- Ambos possuem carteira com saldo
- **Lojistas só recebem** transferências — não podem enviar
- Validação de **saldo suficiente** antes da transferência
- Operação de transferência é uma **transação ACID** (rollback automático em caso de falha)
- **Autorização externa** obrigatória antes de completar a transferência
- **Notificação assíncrona** via RabbitMQ após transferência bem-sucedida

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────┐
│                    Spring Boot App                   │
│                                                      │
│  ┌──────────┐  ┌───────────────┐  ┌───────────────┐ │
│  │Controller│──│   Service     │──│  Repository   │ │
│  │  Layer   │  │   Layer       │  │   Layer       │ │
│  └──────────┘  └───┬───────┬───┘  └───────┬───────┘ │
│                    │       │              │          │
│           ┌────────┘       └────────┐     │          │
│           ▼                         ▼     ▼          │
│  ┌─────────────────┐  ┌──────────────────────────┐  │
│  │  Authorization   │  │      RabbitMQ Publisher  │  │
│  │  Service (HTTP)  │  │                          │  │
│  └────────┬─────────┘  └────────────┬─────────────┘  │
└───────────┼─────────────────────────┼────────────────┘
            │                         │
            ▼                         ▼
   ┌─────────────────┐     ┌──────────────────┐
   │  Autorizador     │     │    RabbitMQ       │
   │  Externo (Mock)  │     │  ┌────────────┐  │
   │                  │     │  │ Notification│  │
   └──────────────────┘     │  │   Consumer  │──┼──► Notification
                            │  └────────────┘  │     Service (Mock)
                            └──────────────────┘
                                    │
                            ┌───────▼────────┐
                            │  PostgreSQL    │
                            │  (Docker)      │
                            └────────────────┘
```

## 🛠️ Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.5.16 | Framework web |
| Spring Data JPA | — | ORM / Persistência |
| Spring AMQP | — | Integração com RabbitMQ |
| Spring Validation | — | Validação de DTOs |
| PostgreSQL | 16 | Banco de dados relacional |
| RabbitMQ | 3.13 | Mensageria assíncrona |
| SpringDoc OpenAPI | 2.8 | Documentação Swagger UI |
| Lombok | — | Redução de boilerplate |
| Docker Compose | — | Orquestração de containers |

## 🚀 Como Executar

### Pré-requisitos

- Java 21+
- Docker e Docker Compose

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/picpay-simplificado.git
cd picpay-simplificado
```

### 2. Suba os containers (PostgreSQL + RabbitMQ)

```bash
docker-compose up -d
```

### 3. Execute a aplicação

```bash
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`

### 4. Acesse a documentação

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **RabbitMQ Dashboard**: http://localhost:15672 (user: `picpay` / pass: `picpay123`)

## 📡 Endpoints da API

### Usuários

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/users` | Criar usuário |
| `GET` | `/users` | Listar todos os usuários |
| `GET` | `/users/{id}` | Buscar usuário por ID |

### Transferências

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/transfer` | Realizar transferência |
| `GET` | `/transactions` | Listar todas as transações |

## 📝 Exemplos de Uso

### Criar usuário comum

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "João",
    "lastName": "Silva",
    "document": "12345678901",
    "email": "joao@email.com",
    "password": "senha123",
    "balance": 1000.00,
    "userType": "COMMON"
  }'
```

### Criar lojista

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Loja",
    "lastName": "ABC",
    "document": "12345678000190",
    "email": "loja@email.com",
    "password": "senha123",
    "balance": 0.00,
    "userType": "MERCHANT"
  }'
```

### Realizar transferência

```bash
curl -X POST http://localhost:8080/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "value": 100.00,
    "payer": 1,
    "payee": 2
  }'
```

### Resposta de sucesso

```json
{
  "transactionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "amount": 100.00,
  "payerId": 1,
  "payerName": "João Silva",
  "payeeId": 2,
  "payeeName": "Loja ABC",
  "status": "COMPLETED",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Resposta de erro (saldo insuficiente)

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Saldo insuficiente. Saldo atual: R$ 50.00, Valor da transferência: R$ 100.00"
}
```

## 🗂️ Estrutura do Projeto

```
src/main/java/com/picpaysimplificado/
├── PicpaySimplificadoApplication.java
├── controller/
│   ├── TransactionController.java
│   └── UserController.java
├── domain/
│   ├── transaction/
│   │   ├── Transaction.java
│   │   ├── TransactionRepository.java
│   │   └── TransactionStatus.java
│   └── user/
│       ├── User.java
│       ├── UserRepository.java
│       └── UserType.java
├── dto/
│   ├── NotificationPayload.java
│   ├── TransferRequest.java
│   ├── TransferResponse.java
│   ├── UserCreateRequest.java
│   └── UserResponse.java
├── infra/
│   ├── config/
│   │   └── RabbitMQConfig.java
│   ├── consumer/
│   │   └── NotificationConsumer.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── InsufficientBalanceException.java
│       ├── TransactionNotAllowedException.java
│       └── UnauthorizedTransactionException.java
└── service/
    ├── AuthorizationService.java
    ├── NotificationService.java
    ├── TransactionService.java
    └── UserService.java
```

## 📌 Conceitos Aplicados

- **Transações ACID** com `@Transactional` do Spring
- **Mensageria assíncrona** com RabbitMQ (produtor/consumidor)
- **Dead Letter Queue** para mensagens que falharam
- **Integração HTTP** com serviços externos (RestClient)
- **Tratamento centralizado de exceções** com `@ControllerAdvice`
- **Bean Validation** com Jakarta Validation
- **Records** do Java (DTOs imutáveis)
- **Builder Pattern** via Lombok
- **Arquitetura em camadas** (Controller → Service → Repository)
- **Docker Compose** para infraestrutura local

## 📄 Licença

Este projeto é apenas para fins de estudo e portfolio.
