scalaVersion := "2.12.0"

tutSettings

TaskKey[Unit]("check") <<= (crossTarget) map { (crossTarget) =>
  if (catext.Dependencies.versions.size <= 0) error("not libraries found")
  ()
}
