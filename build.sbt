
name := "PlaySample"
version := "1.0"
scalaVersion := "2.12.2"

enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "com.auth0" % "java-jwt" % "3.4.0",
  "com.h2database" % "h2" % "1.4.192",
  "org.postgresql" % "postgresql" % "42.2.2",
  jdbc,
  ehcache,
  ws,
  specs2 % Test,
  guice,
  evolutions
)

assemblyJarName in assembly := "app.jar"
mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)
assemblyMergeStrategy in assembly := {
  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need manifest files since sbt-assembly will create
    // one with the given settings
    MergeStrategy.discard
  case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
    // Keep the content for all reference-overrides.conf files
    MergeStrategy.concat
  case x =>
    // For all the other files, use the default sbt-assembly merge strategy
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}