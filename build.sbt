name := "PlaySample"
 
version := "1.0"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.192",
  jdbc,
  ehcache,
  ws,
  specs2 % Test,
  guice,
  evolutions
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

lazy val playProject = (project in file(".")).enablePlugins(PlayScala)