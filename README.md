# Blindnet backend, but in Scala

Built on Scala 3 using cats-effect, http4s, doobie and circe.

## Environment variables

Required if no default value.

| Name                       | Description                  | Example                                | Default       |
|----------------------------|------------------------------|----------------------------------------|:--------------|
| BN_ENV                     | Environment                  | development, staging, production       | development   |
| BN_MIGRATE                 | Enables DB migration         | yes, no                                | env-dependant |
| BN_DB_URI                  | Database URI                 | `jdbc:postgresql://localhost/blindnet` |               |
| BN_DB_USER                 | Database username            | `blindnet`                             |               |
| BN_DB_PASSWORD             | Database password            | `blindnet`                             |               |
| BN_PORT                    | HTTP port                    | 80                                     | 8087          |
| BN_HOST                    | HTTP host                    | 0.0.0.0                                | 127.0.0.1     |
| BN_AZURE_STORAGE_ACC_NAME  | Azure storage account name   |                                        |               |
| BN_AZURE_STORAGE_ACC_KEY   | Azure storage account key    |                                        |               |
| BN_AZURE_STORAGE_CONT_NAME | Azure storage container name |                                        |               |


## Database (PostgreSQL)

The backend uses Flyway for automated migrations on startup.
testcontainers allows functional tests to be executed with a temporary database.
