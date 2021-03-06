name := "FunctionalFlow"

version := "0.1"

scalaVersion := "2.12.6"
val akkaVersion = "2.5.14"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"   % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"  % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion  % Test,
  "org.scalatest"     %% "scalatest"    % "3.0.5"      % Test
)