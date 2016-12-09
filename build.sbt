organization  := "ruc.irm"
version       := "2.8"
scalaVersion  := "2.11.8"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies += "com.github.haifengl" % "smile-scala_2.11" % "1.2.0" //simile machine learning
libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0" //command line parser

//wiki libs
libraryDependencies += "info.bliki.wiki" % "bliki-core" % "3.0.19"
libraryDependencies += "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.io.jwpl-asl" % "1.5.0"

//add jars for old java code
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.1"

libraryDependencies += "commons-cli" % "commons-cli" % "1.2"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.3.1"
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.4"
libraryDependencies += "org.apache.commons" % "commons-collections4" % "4.0"
libraryDependencies += "com.google.guava" % "guava" % "18.0"

libraryDependencies += "org.jgrapht" % "jgrapht-core" % "0.9.0"

//NLP libraries
libraryDependencies += "com.hankcs" % "hanlp" % "portable-1.2.11"
libraryDependencies += "org.nlpcn" % "nlp-lang" % "1.6"
libraryDependencies += "org.ansj" % "ansj_seg" % "5.0.1"
libraryDependencies += "org.ahocorasick" % "ahocorasick" % "0.3.0"

libraryDependencies += "edu.ucla.sspace" % "sspace" % "2.0.4"
libraryDependencies += "com.fasterxml.util" % "java-merge-sort" % "1.0.0"

libraryDependencies += "net.sf.trove4j" % "trove4j" % "3.0.3"

//HTML Process
libraryDependencies += "org.jsoup" % "jsoup" % "1.9.2"

// Mallet used libraries
libraryDependencies += "org.beanshell" % "bsh" % "2.0b4"
libraryDependencies += "com.googlecode.matrix-toolkits-java" % "mtj" % "0.9.14"
libraryDependencies += "org.jdom" % "jdom" % "1.1"
libraryDependencies += "net.sf.jwordnet" % "jwnl" % "1.4_rc3"

libraryDependencies += "junit" % "junit" % "4.12"



scalacOptions in Test ++= Seq("-Yrangepos")

resolvers ++= Seq(
  // other resolvers here
  // if you want to use snapshot builds (currently 0.12-SNAPSHOT), use this.
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "erichseifert.de" at "http://mvn.erichseifert.de/maven2",
  "nlpcn" at "http://maven.nlpcn.org/"
)

assemblyJarName in assembly := "zhinang-nlp.jar"
test in assembly := {}
mainClass in assembly := Some("HTTP")

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "logback.xml"                                 => MergeStrategy.last
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
