package com.me.postfetcher.database

import org.flywaydb.core.Flyway
import java.net.URI

object FlywayConfig {
    fun configure(): Flyway {
        val dbUri = URI(System.getenv("DATABASE_URL"))
//        val dbUri = URI("postgres://tufgrkxzpkzfwy:9c5936436e74c5b7135ef99dc40888ee686e33cecb4a18760aaca968cadac0dc@ec2-3-234-204-26.compute-1.amazonaws.com:5432/dd25fhordv76lb")

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
