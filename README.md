# Blindnet backend, but in Scala

Built on Scala 3 using cats-effect, http4s, doobie and circe.

## Environment variables

All required, no default values.

| Name           | Description       | Example                                |
|----------------|-------------------|----------------------------------------|
| BN_DB_URI      | Database URI      | `jdbc:postgresql://localhost/blindnet` |
| BN_DB_USER     | Database username | `blindnet`                             |
| BN_DB_PASSWORD | Database password | `blindnet`                             |


## Database (PostgreSQL)

The backend uses Flyway for automated migrations on startup.
testcontainers allows functional tests to be executed with a temporary database.