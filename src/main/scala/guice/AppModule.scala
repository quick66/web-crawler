package guice

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.{ActorMaterializer, Materializer}
import com.google.inject.Provides
import com.typesafe.config.{Config, ConfigFactory}
import crawler.{CrawlMaster, WorkerFactory}
import javax.inject.{Named, Singleton}
import net.codingwell.scalaguice.ScalaModule

class AppModule extends ScalaModule {

    override def configure(): Unit = {
    }

    @Provides @Singleton
    def appConfig(): Config = ConfigFactory.load()

    @Provides @Singleton
    def actorSystem(appConfig: Config): ActorSystem = ActorSystem("web-crawler", appConfig)

    @Provides @Singleton
    def materializer(implicit system: ActorSystem): Materializer = ActorMaterializer()

    @Provides @Singleton
    def httpExt(implicit system: ActorSystem): HttpExt = Http()

    @Provides @Singleton @Named("crawl-master")
    def crawlMaster(system: ActorSystem,
                    workerFactory: WorkerFactory): ActorRef = system.actorOf(Props(new CrawlMaster(workerFactory)))

    @Provides @Singleton
    def jsonStreamingSupport(): JsonEntityStreamingSupport = EntityStreamingSupport.json()

}
