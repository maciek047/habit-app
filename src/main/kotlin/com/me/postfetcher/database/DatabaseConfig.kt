package com.me.postfetcher.database

import org.jetbrains.exposed.sql.Database
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DatabaseConfig {
     fun connect() {
        val dbUri = URI(System.getenv("DATABASE_URL"))
//        val dbUri = URI("postgres://tufgrkxzpkzfwy:9c5936436e74c5b7135ef99dc40888ee686e33cecb4a18760aaca968cadac0dc@ec2-3-234-204-26.compute-1.amazonaws.com:5432/dd25fhordv76lb")

        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        val dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path + "?sslmode=require"

        Database.connect(dbUrl, driver = "org.postgresql.Driver", user = username, password = password)

         val flyway = FlywayConfig.configure()
         flyway.migrate()
    }
}

fun getDateOfWeek(dayOfWeek: Int): LocalDate {
    val today = LocalDate.now()
    val currentDayOfWeek = today.dayOfWeek.value % 7
    val daysDifference = dayOfWeek - currentDayOfWeek

    return today.plusDays(daysDifference.toLong())
}

fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return date.format(formatter)
}
