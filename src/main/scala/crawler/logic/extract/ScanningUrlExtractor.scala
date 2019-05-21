package crawler.logic.extract

import java.net.URL

import akka.stream.Materializer
import akka.util.ByteString
import crawler.logic.Document
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

//TODO недоделан
@Singleton
class ScanningUrlExtractor @Inject()(implicit materializer: Materializer) extends UrlExtractor {

    override def extract(document: Document)(implicit ec: ExecutionContext): Future[Seq[URL]] = document match {
        case Document.Strict(_, _, _, content) =>
            //TODO async or blocking?
            Future.successful(parseChunk(ParseResult(), content).found)
        case Document.Streamed(_, _, _, contentStream) =>
            contentStream.runFold(ParseResult())(parseChunk).map(_.found)
    }

    def parseChunk(parseResult: ParseResult, chunk: ByteString): ParseResult = {

        def matchPrefix(str: ByteString, prefix: Array[Char]): Int = {
            (str zip prefix).reverse.dropWhile(p => p._1 == p._2).size
        }

        if (chunk.nonEmpty) {
            parseResult.state match {
                case ATagOpen(prefix) =>
                    val notMatchedCount = matchPrefix(chunk, prefix)

                    if (notMatchedCount == 0) { // if all matched
                        if (prefix.length > chunk.length) { // if prefix longer than chunk
                            parseResult.copy(state = ATagOpen(prefix.drop(chunk.length))) // leave prefix remaining for the next chunk
                        } else { // else we found '<a ', and should next find 'href'
                            parseChunk(parseResult.copy(state = HrefAttr.initial), chunk.drop(prefix.length))
                        }
                    } else { // else try to match
                        parseChunk(parseResult.copy(state = ATagOpen.initial), chunk.drop(prefix.length - notMatchedCount))
                    }
            }
        } else {
            parseResult
        }
    }

}

sealed trait ParserState
case class ATagOpen(next: Array[Char]) extends ParserState
object ATagOpen {
    def initial = ATagOpen("<a ".toCharArray)
}
case class HrefAttr(next: Array[Char]) extends ParserState
object HrefAttr {
    def initial = HrefAttr("href".toCharArray)
}

case class ParseResult(found: Seq[URL] = Seq.empty, state: ParserState = ATagOpen.initial)


