name := "sbt-org-policies"

scalaVersion := "2.12.1"

orgGithubTokenSetting := Option(System.getenv().get("GITHUB_TOKEN_REPO"))

orgAfterCISuccessCheckSetting := true

orgAfterCISuccessTaskListSetting := List(orgCreateFiles)
