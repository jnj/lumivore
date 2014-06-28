package org.joshjoyce.lumivore

import java.net.InetSocketAddress
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet

import org.scalatra.servlet.ScalatraListener
import org.eclipse.jetty.webapp.WebAppContext

object Main {
  def main(args: Array[String]) {
    val socketAddress = new InetSocketAddress(8080)
    val server = new Server(socketAddress)
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    server.setHandler(context)
    server.start()
    server.join()
  }
}


////    val dir = args(0)
////    val paths = new DirectoryPathStream(new File(dir))
////    val outputChannel = new MemoryChannel[IndexRecord]
//    val database = new SqliteDatabase
//    database.connect()
////    database.createTables()
////
////    val fiber = new ThreadFiber()
////    fiber.start()
////
////    outputChannel.subscribe(fiber) {
////      r => {
////        database.insert(r)
////        println(r)
////      }
////    }
////
////    val indexer = new Indexer(paths, outputChannel)
//
//    database.queryPhotos().foreach(println)
//
//
//  }
//}
