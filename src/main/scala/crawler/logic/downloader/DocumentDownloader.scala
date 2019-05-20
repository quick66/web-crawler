package crawler.logic.downloader

import crawler.logic.Document

trait DocumentDownloader {

    def getContent(url: String): Document

}
