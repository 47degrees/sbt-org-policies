scalaVersion := "2.11.8"

TaskKey[Unit]("check") <<= (crossTarget) map { (crossTarget) =>
  if (catext.Dependencies.versions.size <= 0) error("not libraries found")
  ()
}
