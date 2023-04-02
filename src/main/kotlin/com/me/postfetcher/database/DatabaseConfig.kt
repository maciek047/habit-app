package com.me.postfetcher.database

import com.me.postfetcher.route.dto.HabitDayDto
import com.me.postfetcher.route.dto.HabitForTodayDto
import com.me.postfetcher.route.dto.WeeklyHabitDto
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
//        val dbUri = URI("postgres://tufgrkxzpkzfwy:9c5936436e74c5b7135ef99dc40888ee686e33cecb4a18760aaca968cadac0dc@ec2-3-234-204-26.compute-1.amazonaws.com:5432/dd25fhordv76lb")

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
    val completedDays = varchar("completed_days", 255)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    // Add other columns here
}


fun Habit.toWeeklyHabitDto(): WeeklyHabitDto {
    val completedDaysArr = this.completedDays.splitToIntList()
    val habitDays  = this.days.splitToIntList().map { HabitDayDto(it, completedDaysArr.contains(it)) }
    return WeeklyHabitDto(
        id = this.id.value.toString(),
        habitName = this.name,
        days = habitDays
    )
}

fun Habit.toHabitForTodayDto(): HabitForTodayDto {
    val completedDaysArr = this.completedDays.splitToIntList()
    return HabitForTodayDto(
        id = this.id.value.toString(),
        name = this.name,
        completed = completedDaysArr.contains(LocalDateTime.now().dayOfWeek.value)
    )
}

fun String.splitToIntList(): List<Int> {
    return if(this.isBlank()) emptyList() else this.split(",").map { it.toInt() }
}

class Habit(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Habit>(Habits)

    var name by Habits.name
    var description by Habits.description
    var days by Habits.days
    var completedDays by Habits.completedDays
    var createdAt by Habits.createdAt
}

suspend fun createHabit(name: String, days: List<Int>, description: String = ""): Habit {
    return newSuspendedTransaction {
        Habit.new {
            this.name = name
            this.description = description
            this.days = days.joinToString(",")
            this.completedDays = ""
        }
    }
}

const val ALL_WEEKDAYS = "1,2,3,4,5,6,7"

suspend fun fetchHabits(): List<Habit> {
    return newSuspendedTransaction {
        Habit.all().toList()
    }
}

suspend fun fetchTodayHabits(): List<Habit> {
    return newSuspendedTransaction {
        val today = LocalDateTime.now().dayOfWeek.value - 1
        //todo fix this for better performance
        Habit.find { Habits.days like "%$today%" }.toList()
    }
}

suspend fun editHabit(id: String, name: String, days: List<Int>, completedDays: List<Int>, description: String = ""): Habit {
    return newSuspendedTransaction {
        Habit.findById(UUID.fromString(id))?.apply {
            this.name = name
            this.description = description
            this.days = days.joinToString(",")
            this.completedDays = completedDays.joinToString(",")
        } ?: throw Exception("Habit not found")
    }
}

suspend fun deleteHabit(id: String): Habit {
    return newSuspendedTransaction {
        Habit.findById(UUID.fromString(id))?.apply {
            this.delete()
        } ?: throw Exception("Habit not found. Nothing to delete.")
    }
}