package com.emirhanemmez.plugins

import at.favre.lib.crypto.bcrypt.BCrypt
import com.emirhanemmez.db.data.User
import com.emirhanemmez.db.table.UserTable
import com.emirhanemmez.utils.TokenManager
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello world")
        }

        route("/user") {
            authenticate {
                get {
                    call.respond(UserTable.getUserList())
                }
            }

            get("/{index}") {
                val index = call.parameters["index"]?.toInt()
                index?.let {
                    call.respond(UserTable.getUserById(it))
                }
            }

            post {
                val userBody = call.receive<User>()
                userBody.password = userBody.hashedPassword()
                UserTable.addUser(userBody)
                call.respondText("User successfully created!")
            }

            put("/{index}") {
                val index = call.parameters["index"]?.toInt()
                val userBody = call.receive<User>()
                index?.let {
                    UserTable.updateUser(it, userBody)
                    call.respond("Successfully updated!")
                }
            }

            delete("/{index}") {
                val index = call.parameters["index"]?.toInt()
                index?.let {
                    UserTable.deleteUser(it)
                    call.respondText { "Successfully deleted" }
                }
            }
        }

        post("/login") {
            val userBody = call.receive<User>()
            val userInDb = UserTable.getUserByUsername(userBody.username)

            val tokenManager by call.application.inject<TokenManager>()

            val verifyResult =
                BCrypt.verifyer().verify(userBody.password.toCharArray(), userInDb.password.toCharArray())

            if (verifyResult.verified) {
                call.respond(hashMapOf("Token" to tokenManager.generateToken(userBody.username)))
            } else {
                call.respond(status = HttpStatusCode.Unauthorized, "Login failed!")
            }
        }
    }
}
