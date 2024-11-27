# Kotlin Exposed Table Generator Gradle Plugin

## Overview

The **Kotlin Exposed Table Generator** is a Gradle plugin designed to generate
Kotlin [Exposed](https://github.com/JetBrains/Exposed) table definitions directly from a PostgreSQL database schema.
Simplify your workflow by automating the creation of table mappings and keeping them in sync with your database.

## Features

- Generate Kotlin Exposed DSL table definitions.
- Filter tables by schema for precise control.
- Keep your code synchronized with database schema changes effortlessly.

## Installation

Add the plugin and library to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.klahap.pgen") version "$VERSION"
}

dependencies {
    implementation("io.github.klahap:pgenlib:$VERSION")
}
```

## Configuration

To configure the plugin, add the `pgen` block to your `build.gradle.kts`:

```kotlin
pgen {
    dbConnectionConfig(
        url = System.getenv("DB_URL"),          // Database URL
        user = System.getenv("DB_USER"),        // Database username
        password = System.getenv("DB_PASSWORD") // Database password
    )
    packageName("io.example.db")                // Target package for generated tables
    tableFilter {
        addSchemas("public")                    // Include only specific schemas (e.g., "public")
    }
    outputPath("./output")                      // Output directory for generated files
}
```

### Environment Variables

Make sure to set the following environment variables:

- `DB_URL`: The connection URL for your PostgreSQL database.
- `DB_USER`: Your database username.
- `DB_PASSWORD`: Your database password.

## Running the Plugin

Once configured, generate your Kotlin Exposed table definitions by running:

```bash
./gradlew pgen
```

This will create Kotlin files in the specified `outputPath`.

## Example

### Input: Database Schema

Assume a PostgreSQL schema with the following table:

```sql
CREATE TYPE status AS ENUM ('ACTIVE', 'INACTIVE');

CREATE TABLE users
(
    id     SERIAL PRIMARY KEY,
    name   TEXT NOT NULL,
    status status
);
```

### Output: Generated Kotlin File

The plugin will generate a Kotlin Exposed DSL table:

```kotlin
package io.example.db

import org.jetbrains.exposed.sql.Table

enum class Status(
    override val pgEnumLabel: String,
) : PgEnum {
    ACTIVE(pgEnumLabel = "ACTIVE"),
    INACTIVE(pgEnumLabel = "INACTIVE");

    override val pgEnumTypeName: String = "public.status"
}

object Users : Table("users") {
    val id: Column<Int> = integer(name = "id")
    val name: Column<String> = text(name = "name")
    val status: Column<Status> = customEnumeration(
        name = "status",
        sql = "status",
        fromDb = { getPgEnumByLabel(it as String) },
        toDb = { it.toPgObject() },
    )

    override val primaryKey: Table.PrimaryKey = PrimaryKey(id, name = "users_pkey")
}
```

## Contributing

Contributions are welcome! Feel free to open issues or submit pull requests to improve the plugin.

## License

This project is licensed under the [MIT License](LICENSE).
