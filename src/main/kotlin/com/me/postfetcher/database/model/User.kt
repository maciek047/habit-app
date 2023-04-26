package com.me.postfetcher.database.model

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.UUID

object Users : UUIDTable() {
    val email = varchar("name", 255)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var email by Users.email
    var createdAt by Users.createdAt
}

suspend fun createUserIfNotExists(email: String): User {
    return findUserByEmail(email) ?: createUser(email)
}

suspend fun createUser(email: String): User {
    return newSuspendedTransaction {
        User.new {
            this.email = email
            this.createdAt = LocalDateTime.now()
        }
    }
}

suspend fun findUserByEmail(email: String): User? {
    return newSuspendedTransaction {
        User.find { Users.email eq email }.firstOrNull()
    }
}
