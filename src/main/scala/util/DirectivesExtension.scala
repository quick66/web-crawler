package util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, StandardRoute}
import spray.json.{JsObject, JsTrue}

trait DirectivesExtension {
    self: Directives with SprayJsonSupport =>

    val completeWithJsonOk: StandardRoute = complete(JsObject("ok" -> JsTrue))

}
