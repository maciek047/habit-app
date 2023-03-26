package com.me.postfetcher.database

import org.flywaydb.core.Flyway
import java.net.URI

object FlywayConfig {
    fun configure(): Flyway {
        val dbUri = URI(System.getenv("DATABASE_URL"))

        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        val dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path + "?sslmode=require"

        return Flyway.configure()
            .dataSource(dbUrl, username, password)
            .locations("filesystem:./src/main/db/migration")
            .baselineVersion("20230324134810")
            .load()
    }
}