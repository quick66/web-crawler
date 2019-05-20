package util

import java.net.{MalformedURLException, URL}

import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, deserializationError}

trait URLFormatExtension {
    self: DefaultJsonProtocol =>

    implicit object URLFormat extends JsonFormat[URL] {

        def write(url: URL) = JsString(url.toString)

        def read(value: JsValue): URL = value match {
            case JsString(str) =>
                try {
                    new URL(str)
                } catch {
                    case e: MalformedURLException =>
                        deserializationError("Invalid URL format", e)
                }
            case x =>
                deserializationError("Expected URL as JsString, but got " + x)
        }

    }

}
