# SQL And Migrations

Authoritative schema changes live in Flyway migrations:

```text
backend/src/main/resources/db/migration
```

Batch SQL scripts must follow the project rule that every `insert ... select` output field has a per-field comment.
