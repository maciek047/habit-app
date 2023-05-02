package com.me.postfetcher.database.model

import com.me.postfetcher.route.UserAuthProfile
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.UUID

object Users : UUIDTable() {
    val sub = varchar("sub", 255)
    val name = varchar("name", 255).nullable()
    val email = varchar("email", 255).nullable()

    //todo add more fields
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var sub by Users.sub
    var name by Users.name
    var email by Users.email
    var createdAt by Users.createdAt
}

suspend fun createUser(userAuthProfile: UserAuthProfile): User {
    return newSuspendedTransaction {
        User.new {
            this.sub = userAuthProfile.sub
            this.email = userAuthProfile.email
            this.name = userAuthProfile.email
            this.createdAt = LocalDateTime.now()
        }
    }
}

suspend fun findUserByEmail(email: String): User? {
    return newSuspendedTransaction {
        User.find { Users.email eq email }.firstOrNull()
    }
}


suspend fun findUserBySub(sub: String): User? {
    return newSuspendedTransaction {
        User.find { Users.email eq sub }.firstOrNull()
    }
}