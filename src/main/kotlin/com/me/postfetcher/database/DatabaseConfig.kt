package com.me.postfetcher.database

import com.me.postfetcher.database.Habits.days
import com.me.postfetcher.database.Habits.description
import com.me.postfetcher.database.Habits.name
import com.me.postfetcher.route.dto.HabitDto
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.postgresql.jdbc.PgArray
import org.postgresql.util.PGobject
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

private fun ResultRow.getDays(): IntArray {
    return this[Habits.days]
}


object Habits : IntIdTable() {
    val name = varchar("name", 255)
    val description = varchar("description", 255)
    val days: Column<IntArray> = registerColumn("days", IntArrayColumnType())
    // Add other columns here
}

fun Habit.toDto(): HabitDto {
    return HabitDto(
        id = this.id.value,
        habitName = this.name,
        days = this.days.toList()
    )
}

class Habit(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Habit>(Habits)

    var name by Habits.name
    var description by Habits.description
    var days by Habits.days
}

suspend fun createHabit(name: String, description: String): Habit {
    return newSuspendedTransaction {
        Habit.new {
            this.name = name
            this.description = description
        }
    }
}

suspend fun fetchHabits(): List<Habit> = newSuspendedTransaction {

    Habits.selectAll().toList().map { Habit(
        id = it[Habits.id]
    ).apply {
            this.name = it[Habits.name]
            this.description = it[Habits.description]
            this.days = it[Habits.days]
        }
    }.toList()




    }



// Custom column type for integer arrays
class IntArrayColumnType : VarCharColumnType() {
    override fun sqlType() = "integer[]"

    override fun valueFromDB(value: Any): Any {
        return when(value) {
            is PGobject -> (value.value as String).replace("[{}]".toRegex(), "").split(",").map { it.trim().toInt() }.toIntArray()
            else -> super.valueFromDB(value)
        }
    }

    override fun notNullValueToDB(value: Any): Any {
        return when (value) {
            is IntArray -> PGobject().apply {
                this.type = "integer[]"
                this.value = value.joinToString(prefix = "{", postfix = "}")
            }
            else -> super.notNullValueToDB(value)
        }
    }
}