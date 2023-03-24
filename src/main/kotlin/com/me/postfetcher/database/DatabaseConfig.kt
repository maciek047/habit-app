package com.me.postfetcher.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.net.URI

object DatabaseConfig {
     fun connect() {
        val dbUri = URI(System.getenv("DATABASE_URL"))

        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        val dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path + "?sslmode=require"

        Database.connect(dbUrl, driver = "org.postgresql.Driver", user = username, password = password)

         val flyway = FlywayConfig.configure()
         flyway.migrate()
    }
}

object Habits : IntIdTable() {
    val name = varchar("name", 255)
    val description = varchar("description", 255)
    // Add other columns here
}

class Habit(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Habit>(Habits)

    var name by Habits.name
    var description by Habits.description
    // Add other properties here
}

suspend fun createHabit(name: String, description: String): Habit {
    return newSuspendedTransaction {
        Habit.new {
            this.name = name
            this.description = description
        }
    }
}
