## Requerimentos

- Java JDK 21

## Executar a aplicação em ambiente local

- Faça o clone deste repositório

- Para executar a aplicação, execute o método main da classe
  src/main/java/com/tproject/workshop/WorkshopApplication.java
  pelo seu IDE.
- Subir o banco de dados local, executar o comando: `docker-compose -f docker-compose-local.yml up -d`. Você também pode
  utilizar o docker-compose padrão, que é o utilizado pelo teste. Popule o banco com alguns dos scripts de teste (abra
  alguma classe \*ControllerIT, pegue os arquivos que estão na anotação `@Sql` e os execute no banco que o docker subiu)
- Para executar testes unitários e de integração
  ./gradlew clean build integrationTests

Caso seja necessário apagar e subir novamente o banco de dados, execute os comandos abaixo:

```
docker-compose -f docker-compose-local.yml down --volumes 
docker-compose -f docker-compose-local.yml up -d
```
