# Users

Projeto inicial Maven com Java 25 e Quarkus para o microsserviço de usuários de uma plataforma de ateliê digital. Contém apenas dependências e configuração: não há entidades, endpoints de negócio, autenticação funcional, regras de negócio, telas, envio de e-mails nem fluxos de verificação ou recuperação.

## Tecnologias

O BOM do Quarkus gerencia REST com JSON via Jackson, Hibernate Validator, Hibernate ORM com Panache, JDBC PostgreSQL, Flyway, SmallRye JWT, Elytron Security Common (incluindo `BcryptUtil`), SmallRye OpenAPI, SmallRye Health e registro Prometheus do Micrometer.

## Execução

Requisitos: JDK 25, Maven 3.9+ e um banco PostgreSQL acessível. Configure `JAVA_HOME` para o JDK 25. No Bash, forneça a conexão por variáveis de ambiente, sem incluí-las no repositório:

```sh
export DB_URL='jdbc:postgresql://<host>:<porta>/<banco>'
export DB_USERNAME='<usuario-do-banco>'
read -rs QUARKUS_DATASOURCE_PASSWORD; export QUARKUS_DATASOURCE_PASSWORD; echo
mvn quarkus:dev
```

Digite a senha do banco no prompt. A geração de esquema pelo Hibernate está desabilitada; o Flyway executa as migrações na inicialização. Adicione migrações futuras em `src/main/resources/db/migration`, seguindo a convenção de nomes do Flyway. Ainda não há migração nem esquema.

Para compilar e executar a aplicação empacotada:

```sh
mvn verify
java -jar target/quarkus-app/quarkus-run.jar
```

As mesmas variáveis de ambiente são necessárias para executar a aplicação empacotada. Os endpoints operacionais padrão são `/q/openapi`, `/q/health` e `/q/metrics`. Proteja-os adequadamente na infraestrutura de implantação; este projeto inicial não os protege. As chaves de verificação JWT, o emissor, as políticas de autorização e a emissão de tokens devem ser configurados e implementados antes de usar autenticação.

## Requisitos e invariantes para implementação futura

- Visitantes sem conta podem consultar publicamente o catálogo e o perfil da artista.
- Clientes cadastrados podem realizar compras e acompanhar somente os próprios pedidos.
- A artista pode gerenciar obras, perfil e pedidos.
- O cadastro público cria exclusivamente contas de cliente. O serviço atribui o papel; ele não deve ser aceito nem definido a partir do corpo da requisição.
- A conta da artista deve ser provisionada somente por um fluxo administrativo controlado, nunca pelo cadastro público.
- O e-mail deve ser único.
- Senhas devem ser armazenadas somente como hashes fortes, nunca em texto puro. Use um algoritmo específico para senhas, como BCrypt, com fator de trabalho adequado; não registre nem retorne senhas.
- Verificação de e-mail e recuperação de senha devem ser consideradas no desenho futuro; nenhum desses fluxos está implementado aqui.
