package org.joshjoyce.lumivore.web

import org.json4s._
import org.scalatra._
import org.scalatra.scalate.ScalateSupport
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.util.LumivoreLogging
import JsonDSL._
import scala.concurrent.ExecutionContext.Implicits.global

class Lumivore extends ScalatraServlet
  with ScalateSupport
  with JValueResult
  with JacksonJsonSupport
  with SessionSupport
  with AtmosphereSupport
  with LumivoreLogging {

  val database = new SqliteDatabase
  database.connect()

  implicit protected val jsonFormats: Formats = DefaultFormats

  get("/") {
    contentType = "text/html"
    val records = database.queryPhotos().map(_.toString)
    val dupes = database.getDuplicates()
    scaml("/index.scaml", "records" -> records, "duplicates" -> dupes)
  }

  get("/index") {
    contentType = "text/html"
    scaml("/indexer.scaml")
  }

  atmosphere("/index-request") {
    new AtmosphereClient {
      def receive = {
        case Connected => log.info("Connection from client")
        case Disconnected(disconnector, Some(error)) => log.info("Disconnect from client")
        case Error(Some(error)) => log.error("web socket error", error)
        case TextMessage(text) => log.info("Received txt msg: " + text)
        case JsonMessage(json) => {
          log.info("Got index request: " + json)
          send(("message" -> "hello") ~ ("author" -> "josh"))
        }
      }
    }
  }

  error {
    case t: Throwable => t.printStackTrace()
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map {
      path =>
        contentType = "text/html"
        layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }

}
