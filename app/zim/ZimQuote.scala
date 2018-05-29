package zim

import play.api.libs.json.{Json, Reads, Writes}

case class ZimQuote(quote: String, character: String)

object ZimQuote {
  implicit val reads: Reads[ZimQuote] = Json.reads[ZimQuote]
  implicit val writes: Writes[ZimQuote] = Json.writes[ZimQuote]
}
