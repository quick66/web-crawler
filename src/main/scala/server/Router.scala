package server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import crawler._
import javax.inject.{Inject, Named, Singleton}
import server.AppJsonProtocol._
import util.DirectivesExtension

@Singleton
class Router @Inject()(@Named("crawl-master") master: ActorRef, config: Config)
                      (implicit system: ActorSystem)
    extends Directives
    with SprayJsonSupport
    with DirectivesExtension {

    private implicit val defaultTimeout: Timeout = Timeout.create(config.getDuration("app.request-state-timeout"))

    def routes: Route = {
        path("status") {
            get {
                complete((master ? GetCrawlStatus).mapTo[CrawlingStatus])
            }
        } ~ path("page") {
            post {
                entity(as[AddUrl]) { addUrl =>
                    master ! addUrl
                    completeWithJsonOk
                }
            }
        } ~ path("domains") {
            get {
                complete((master ? ListAllowedDomains).mapTo[AllowedDomains])
            } ~ post {
                entity(as[SetAllowedDomains]) { setAllowedDomains =>
                    master ! setAllowedDomains
                    completeWithJsonOk
                }
            }
        } ~ path("pause") {
            post {
                master ! PauseCrawling
                completeWithJsonOk
            }
        } ~ path("resume") {
            post {
                master ! ResumeCrawling
                completeWithJsonOk
            }
        }
    }

}
