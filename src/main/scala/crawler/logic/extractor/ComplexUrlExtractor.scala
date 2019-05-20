package crawler.logic.extractor

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import crawler.logic.Document
import javax.inject.Singleton

import scala.concurrent.Future

//TODO недоделан
@Singleton
class ComplexUrlExtractor(implicit system: ActorSystem) extends UrlExtractor {

    private implicit val mat: Materializer = ActorMaterializer()

    override def extract(document: Document): Future[Seq[String]] = document match {
        case Document.Strict(_, content) =>
            //TODO async or blocking?
            Future.successful(parseChunk(ParseResult(), content).found)
        case Document.Streamed(_, contentStream) =>
            contentStream.runFold(ParseResult())(parseChunk).map(_.found)(mat.executionContext)
    }

    def parseChunk(parseResult: ParseResult, chunk: String): ParseResult = {

        def matchPrefix(str: String, prefix: Array[Char]): Int = {
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

case class ParseResult(found: Seq[String] = Seq.empty, state: ParserState = ATagOpen.initial)


