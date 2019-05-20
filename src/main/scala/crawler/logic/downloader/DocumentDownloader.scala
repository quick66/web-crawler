package crawler.logic.downloader

import java.net.URL

import crawler.logic.Document

import scala.concurrent.ExecutionContext

trait DocumentDownloader {

    def getContent(url: URL)(implicit ec: ExecutionContext): Document

}
