[![Build Status](https://travis-ci.org/47deg/sbt-catalysts-extras.svg?branch=master)](https://travis-ci.org/47deg/sbt-catalysts-extras)

# sbt-catalysts-extras

**sbt-catalysts-extras** is an SBT plugin, which forks and extends the [sbt-catalyst](https://github.com/typelevel/sbt-catalysts) plugin, providing more dependencies and settings outside from Typelevel org.

## Installation

Add the following line to `project/plugins.sbt`:

```scala
addSbtPlugin("com.fortysevendeg" % "sbt-catalysts-extras" % "0.1.1")
```

See some examples about how to use it in the [Typelevel sbt-catalyst](https://github.com/typelevel/sbt-catalysts#quick-example).

Additionally, this plugin provides some other plugins, settings and tasks.

## Extra-Plugins

* [sbt-header](https://github.com/sbt/sbt-header)
* [sbt-buildinfo](https://github.com/sbt/sbt-buildinfo)
* [scripted-plugin](https://github.com/sbt/sbt/tree/0.13/scripted)

## Extra-Settings

Settings ready to be used in your project:

* `pgpSettings`: https://goo.gl/OmcBfs, useful to deploy to Sonatype.
* `testScriptedSettings`: https://goo.gl/ciLZL4, some basic configuration settings to test your sbt plugins.
* `miscSettings`: https://goo.gl/bxp4EF, to enjoy with a fancy sbt shell prompt.

## Extra-Tasks

When using different versions of this plugin you might want to know quickly, without having to look at the source code, what are the libraries and versions available to include in your project. `sbt-catalysts-extras` bring these tasks:

* `allLibraries`: Task to get all the available libraries.
* `searchDep`: Task to search a dependency by name.
* `allVersions`: Task to get all the available library versions.
* `versionOf`: Task to get the version of the dependency specified as argument.
* `publishSnapshot`: Task to publish only if the version is a SNAPSHOT.

Some examples:

```bash
> allLibraries
algebra -> "org.typelevel" %% "algebra" % "0.5.1"
algebra-laws -> "org.typelevel" %% "algebra-laws" % "0.5.1"
alleycats -> "org.typelevel" %% "alleycats" % "0.1.7"
catalysts -> "org.typelevel" %% "catalysts" % "0.1.0"
catalysts-checklite -> "org.typelevel" %% "catalysts-checklite" % "0.1.0"
catalysts-lawkit -> "org.typelevel" %% "catalysts-lawkit" % "0.1.0"
catalysts-macros -> "org.typelevel" %% "catalysts-macros" % "0.1.0"
catalysts-platform -> "org.typelevel" %% "catalysts-platform" % "0.1.0"
catalysts-scalatest -> "org.typelevel" %% "catalysts-scalatest" % "0.1.0"
catalysts-specbase -> "org.typelevel" %% "catalysts-specbase" % "0.1.0"
catalysts-speclite -> "org.typelevel" %% "catalysts-speclite" % "0.1.0"
catalysts-specs2 -> "org.typelevel" %% "catalysts-specs2" % "0.1.0"
catalysts-testkit -> "org.typelevel" %% "catalysts-testkit" % "0.1.0"
cats -> "org.typelevel" %% "cats" % "0.7.2"
cats-core -> "org.typelevel" %% "cats-core" % "0.7.2"
cats-free -> "org.typelevel" %% "cats-free" % "0.7.2"
cats-kernel -> "org.typelevel" %% "cats-kernel" % "0.7.2"
...
```

```bash
> searchDep "cats"
cats-free -> "org.typelevel" %% "cats-free" % "0.7.2"
cats-kernel -> "org.typelevel" %% "cats-kernel" % "0.7.2"
cats-macros -> "org.typelevel" %% "cats-macros" % "0.7.2"
alleycats -> "org.typelevel" %% "alleycats" % "0.1.7"
cats -> "org.typelevel" %% "cats" % "0.7.2"
cats-core -> "org.typelevel" %% "cats-core" % "0.7.2"
cats-state -> "org.typelevel" %% "cats-state" % "0.7.2"
cats-laws -> "org.typelevel" %% "cats-laws" % "0.7.2"
```

```bash
> allVersions
algebra -> 0.5.1
alleycats -> 0.1.7
catalysts -> 0.1.0
cats -> 0.7.2
circe -> 0.5.4
...
slf4j -> 1.7.21
specs2 -> 3.6.4
```

```bash
>versionOf github4s
github4s = 0.8.0-SNAPSHOT
```
