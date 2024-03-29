package com.me.habitapp.database.model

import com.me.habitapp.route.UserAuthProfile
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.UUID

object Users : UUIDTable() {
    val sub = varchar("sub", 255).uniqueIndex()
    val name = varchar("name", 255).nullable()
    val email = varchar("email", 255).nullable()
    val emailVerified = bool("email_verified").default(false)
    val locale = varchar("locale", 255).nullable()
    val givenName = varchar("given_name", 255).nullable()
    val familyName = varchar("family_name", 255).nullable()
    val nickname = varchar("nickname", 255).nullable()
    val picture = varchar("picture", 1000).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var sub by Users.sub
    var name by Users.name
    var email by Users.email
    var emailVerified by Users.emailVerified
    var locale by Users.locale
    var givenName by Users.givenName
    var familyName by Users.familyName
    var nickname by Users.nickname
    var picture by Users.picture
    var createdAt by Users.createdAt
}

suspend fun createUser(userAuthProfile: UserAuthProfile): User {
    val user = newSuspendedTransaction {
        User.new {
            this.sub = userAuthProfile.sub
            this.email = userAuthProfile.email
            this.name = userAuthProfile.name
            this.emailVerified = userAuthProfile.emailVerified
            this.locale = userAuthProfile.locale
            this.givenName = userAuthProfile.givenName
            this.familyName = userAuthProfile.familyName
            this.nickname = userAuthProfile.nickname
            this.picture = userAuthProfile.picture
            this.createdAt = LocalDateTime.now()
        }
    }
    createFirstHabit(user.id.value)
    return user
}

suspend fun findUserBySub(sub: String): User? {
    return newSuspendedTransaction {
        User.find { Users.sub eq sub }.firstOrNull()
    }
}
