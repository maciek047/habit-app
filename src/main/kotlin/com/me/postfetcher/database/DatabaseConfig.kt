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

fun String.splitToIntList(): List<Int> {
    return if(this.isBlank()) emptyList() else this.split(",").map { it.toInt() }
}

fun getDateOfWeek(dayOfWeek: Int): String {
    val today = LocalDate.now()
    val currentDayOfWeek = today.dayOfWeek.value % 7 // Monday is 0, Tuesday is 1, ..., Sunday is 6
    val daysDifference = dayOfWeek - currentDayOfWeek

    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return today.plusDays(daysDifference.toLong()).format(formatter)
}