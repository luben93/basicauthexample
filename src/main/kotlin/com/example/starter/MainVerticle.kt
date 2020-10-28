package com.example.starter

import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AbstractUser
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BasicAuthHandler
import io.vertx.ext.web.handler.impl.HttpStatusException
import java.lang.Exception
import java.lang.NullPointerException

fun main() {
  Vertx.vertx().deployVerticle(MainVerticle())

}

class MainVerticle : AbstractVerticle() {

  override fun start(startPromise: Promise<Void>) {
    val router = Router.router(vertx)
    router.get("/").handler(BasicAuthHandler.create(BasicAuthProvider(mapOf("foo" to "bar"))))
    router.get("/").handler {
      val resp = it.response()
      resp.statusCode = 200
      resp.end("authenticated, bye")
    }
    vertx.createHttpServer().requestHandler(router).listen(7070)
  }
}



class BasicAuthProvider(private val userPass: Map<String, String>) : AuthProvider {
  override fun authenticate(authInfo: JsonObject?, resultHandler: Handler<AsyncResult<User>>?) {
    try {
      println("got request")
      if (authInfo != null) {
        val requestUsername =
          authInfo.getString("username") ?: throw NullPointerException("no username")
        val requestPassword =
          authInfo.getString("password") ?: throw NullPointerException("no password")
        println("u $requestUsername $requestPassword")
        val isKnownUser = userPass.containsKey(requestUsername)
        println("server pass '${userPass[requestUsername]}' req: '$requestPassword'")
        val isPasswordsMatching = userPass[requestUsername] == requestPassword
        println("known $isKnownUser pass $isPasswordsMatching")
        if (isKnownUser && isPasswordsMatching) {
          val user = object : AbstractUser() {
            val username = authInfo.getString("username")
            override fun doIsPermitted(permission: String?, resultHandler: Handler<AsyncResult<Boolean>>?) {
              resultHandler?.handle(Future.succeededFuture(true))
            }

            override fun setAuthProvider(authProvider: AuthProvider?) {
            }

            override fun principal(): JsonObject {
              return JsonObject(mapOf("username" to username))
            }

          }
          resultHandler?.handle(Future.succeededFuture(user))
          return
        }
      }
      resultHandler?.handle(Future.failedFuture(HttpStatusException(401)))
    } catch (ex: Exception) {
      resultHandler?.handle(Future.failedFuture(HttpStatusException(401)))
    }
  }
}
