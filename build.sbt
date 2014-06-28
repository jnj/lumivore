name := "Lumivore"

version := "1.0"

seq(webSettings:_*)

libraryDependencies += "org.apache.tika" % "tika-core" % "1.5"

libraryDependencies += "org.apache.tika" % "tika-parsers" % "1.5"

libraryDependencies += "org.jetlang" % "jetlang" % "0.2.12"

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.7.15-M1"

libraryDependencies += "org.scalatra" %% "scalatra" % "2.3.0"

libraryDependencies += "org.scalatra.scalate" %% "scalate-web" % "1.7.0"

libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided"

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-webapp" % "9.1.0.v20131115" % "compile;container",
  "org.eclipse.jetty" % "jetty-plus"   % "9.1.0.v20131115" % "container"
)

