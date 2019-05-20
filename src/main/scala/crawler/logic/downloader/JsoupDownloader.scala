package crawler.logic.downloader

import crawler.logic.Document
import org.jsoup.Jsoup

class JsoupDownloader extends DocumentDownloader {

    override def getContent(url: String): Document = {
        //TODO можно и тут постримить, но для примера пусть так будет
        Document.Strict(url, Jsoup.connect(url).response().body())
    }

}
