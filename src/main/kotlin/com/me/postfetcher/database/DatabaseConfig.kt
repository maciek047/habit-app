package com.me.postfetcher.database

import com.me.postfetcher.route.dto.HabitDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

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




object Habits : UUIDTable() {
    val name = varchar("name", 255)
    val description = varchar("description", 255)
    val days = varchar("days", 255)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    // Add other columns here
}


fun Habit.toDto(): HabitDto {
    return HabitDto(
        id = this.id.value,
        habitName = this.name,
        days = this.days.split(",").map { it.toInt() },
    )
}

class Habit(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Habit>(Habits)

    var name by Habits.name
    var description by Habits.description
    var days by Habits.days
    var createdAt by Habits.createdAt
}

suspend fun createHabit(name: String, days: List<Int>, description: String = ""): Habit {
    return newSuspendedTransaction {
        Habit.new {
            this.name = name
            this.description = description
            this.days = days.joinToString(",")
        }
    }
}

const val ALL_WEEKDAYS = "1,2,3,4,5,6,7"

suspend fun fetchHabits(): List<Habit> {
    return newSuspendedTransaction {
        Habit.all().toList()
    }
}