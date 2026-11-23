# Users

Microsserviço de usuários da plataforma Corpoário, estruturado com Java 25 e
Quarkus.

## Requisitos

- JDK 25
- Maven 3.9 ou superior
- Docker com Docker Compose

## Executar em desenvolvimento

Crie um arquivo `.env` na raiz do projeto com uma senha local do PostgreSQL:

```dotenv
DB_PASSWORD=uma-senha-local
```

Carregue as variáveis no terminal e inicie o banco e a aplicação:

```bash
set -a
. ./.env
set +a

docker compose up -d
mvn quarkus:dev
```

O PostgreSQL de desenvolvimento usa a porta 5434 para não conflitar com o
catálogo. Se o container já existir, execute `docker-compose up -d` novamente
para recriá-lo com a nova porta. O volume mantém os dados e a senha inicial;
alterar `DB_PASSWORD` no `.env` não altera a senha de um banco já inicializado.

Para parar o banco sem apagar os dados, execute `docker-compose down`.
A geração de esquema pelo Hibernate está desabilitada; o Flyway executa as
migrações na inicialização.

Os valores padrão de `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` valem apenas no
modo de desenvolvimento. Em produção, as três variáveis devem ser informadas; a
aplicação não inicia sem elas.

No modo de desenvolvimento, a interface Swagger UI fica disponível em
`http://localhost:8081/q/swagger-ui`. Os endpoints de health check e métricas
Prometheus ficam em `/q/health` e `/q/metrics`.

## Cadastro de clientes

`POST /customers` cria uma conta de cliente a partir de um corpo JSON com
`email` e `password`:

```json
{"email": "cliente@example.com", "password": "pelo-menos-8-caracteres"}
```

O email é aparado (trim) e convertido para minúsculas antes da validação e da
persistência, e deve ter um domínio com ponto. A senha deve ter pelo menos 8
caracteres e no máximo 72 bytes em UTF-8 (o BCrypt ignora o que passar disso),
e é armazenada apenas como hash BCrypt. O servidor sempre atribui o papel
`CUSTOMER`; `role`, `status` ou qualquer outro campo extra no payload é
ignorado. A resposta nunca inclui a senha nem o seu hash.

| Status | Significado |
| --- | --- |
| `201 Created` | Conta criada; o corpo tem `id`, `email`, `role`, `status` e `createdAt`. |
| `400 Bad Request` | Corpo ausente, JSON malformado, ou email ou senha inválidos. |
| `409 Conflict` | Já existe uma conta com este email, ignorando maiúsculas e espaços nas extremidades. |

Os erros usam o corpo `{"status": 409, "error": "...", "message": "..."}`.

## Executar os testes

```bash
mvn test
```

Os testes usam um container PostgreSQL descartável, iniciado automaticamente
pelo Quarkus Dev Services; por isso, o Docker precisa estar em execução. Eles
nunca usam o banco de desenvolvimento nem `DB_URL`. Se o socket do Docker não
estiver no local padrão, configure-o antes de executar os testes. Para o
perfil padrão do Colima:

```bash
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

Exporte essas variáveis no mesmo terminal antes de `mvn quarkus:dev` para os
testes contínuos também encontrarem o Docker. Para outro perfil ou runtime,
use o endpoint informado por `docker context inspect`.
