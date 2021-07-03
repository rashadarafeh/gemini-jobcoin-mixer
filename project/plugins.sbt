resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += Resolver.typesafeRepo("releases")
resolvers += Resolver.typesafeIvyRepo("releases")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.19")