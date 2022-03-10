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

[db.sql](db.sql) contains a dump of the current schema of the database, for documentation and importing purposes until
we implement database migration.