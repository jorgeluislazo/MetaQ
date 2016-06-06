name := "frontend"

version := "1.0"

lazy val `frontend` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc , cache  , ws   , specs2 % Test )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

libraryDependencies += "org.apache.solr" % "solr-solrj" % "4.8.0"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "amateras-repo" at "http://amateras.sourceforge.jp/mvn/"  
