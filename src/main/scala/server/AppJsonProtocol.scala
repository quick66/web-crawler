package server

import crawler.{AddUrl, AllowedDomains, CrawlingStatus, SetAllowedDomains}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import util.URLFormatExtension

object AppJsonProtocol
    extends DefaultJsonProtocol
    with URLFormatExtension {

    implicit val AddUrlFormat: RootJsonFormat[AddUrl] = jsonFormat1(AddUrl)
    implicit val CrawlingStatusFormat: RootJsonFormat[CrawlingStatus] = jsonFormat3(CrawlingStatus)

    implicit val SetAllowedDomainsFormat: RootJsonFormat[SetAllowedDomains] = jsonFormat1(SetAllowedDomains)
    implicit val AllowedDomainsFormat: RootJsonFormat[AllowedDomains] = jsonFormat1(AllowedDomains)

}
