package guice

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.{ActorMaterializer, Materializer}
import com.google.inject.Provides
import com.typesafe.config.{Config, ConfigFactory}
import crawler.logic.download.{DocumentDownloader, JsoupDownloader}
import crawler.logic.extract.{JsoupUrlExtractor, UrlExtractor}
import crawler.logic.storage.{SimpleFileSystemStorage, Storage}
import crawler.{CrawlMaster, WorkerFactory}
import javax.inject.{Named, Singleton}
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.duration._
import scala.util.Try

class AppModule extends ScalaModule {

    @Provides @Singleton
    def appConfig(): Config = ConfigFactory.load()

    @Provides @Singleton
    def actorSystem(appConfig: Config): ActorSystem = ActorSystem("web-crawler", appConfig)

    @Provides @Singleton @Named("crawl-master")
    def crawlMaster(appConfig: Config,
                    system: ActorSystem,
                    workerFactory: WorkerFactory): ActorRef = {
        val dequeueNextUrlInterval = Try {
            appConfig.getDuration("crawler.master.dequeue-next-url-interval").toNanos.nanos
        } getOrElse(100 milliseconds)
        system.actorOf(Props(new CrawlMaster(workerFactory, dequeueNextUrlInterval)))
    }

    override def configure(): Unit = {
        bind[UrlExtractor].to[JsoupUrlExtractor]
        bind[DocumentDownloader].to[JsoupDownloader]
        bind[Storage].to[SimpleFileSystemStorage]
        bind[JsonEntityStreamingSupport].toInstance(EntityStreamingSupport.json())
    }

}
