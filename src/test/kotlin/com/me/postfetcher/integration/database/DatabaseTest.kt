package com.me.postfetcher.integration.database

import com.me.postfetcher.database.Habits
import com.me.postfetcher.database.createHabit
import com.me.postfetcher.database.fetchHabits
import com.me.postfetcher.database.toDto
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseTest : StringSpec({
    val testDb = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

    transaction(testDb) {
        SchemaUtils.create(Habits)
    }

    afterEach {
        transaction(testDb) {
            SchemaUtils.drop(Habits)
            SchemaUtils.create(Habits)
        }
    }

    "should create a habit" {
        val habit = newSuspendedTransaction(db = testDb) { createHabit("Test habit", listOf(1,3,5)) }
        habit.shouldNotBeNull()
        habit.name shouldBe "Test habit"
    }

    "should fetch all habits" {
        newSuspendedTransaction(db = testDb) {
            createHabit("Test habit", listOf(1,3,5))
            createHabit("Test habit2", listOf(2,4,6))

        }

        val habits = newSuspendedTransaction(db = testDb) { fetchHabits() }

        habits.size shouldBe 2
        habits[0].toDto().habitName shouldBe "Test habit"
        habits[1].toDto().habitName shouldBe "Test habit2"
        habits[0].toDto().days shouldBe listOf(1,3,5)
        habits[1].toDto().days shouldBe listOf(2,4,6)
    }
})
