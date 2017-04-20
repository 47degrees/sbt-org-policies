/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbtorgpolicies.runnable

import sbt.TaskKey

import scala.language.implicitConversions

object syntax {

  implicit def runnableTaskOpsSyntax[T](taskKey: TaskKey[T]): RunnableTaskOps[T] =
    new RunnableTaskOps[T](taskKey)

  implicit def runnableSetSettingOpsSyntax[T](setSetting: SetSetting[T]): RunnableSetSettingOps[T] =
    new RunnableSetSettingOps[T](setSetting)

  final class RunnableTaskOps[T](taskKey: TaskKey[T]) {

    def asRunnableItemFull: RunnableItemConfigScope[T] =
      asRunnableItem(allModules = true, aggregated = true, crossScalaVersions = true)

    def asRunnableItem: RunnableItemConfigScope[T] =
      asRunnableItem(allModules = false, aggregated = false, crossScalaVersions = false)

    def asRunnableItem(
        allModules: Boolean,
        aggregated: Boolean,
        crossScalaVersions: Boolean): RunnableItemConfigScope[T] =
      RunnableItemConfigScope(RunnableTask(taskKey), allModules, aggregated, crossScalaVersions)

  }

  final class RunnableSetSettingOps[T](setSetting: SetSetting[T]) {

    def asRunnableItemFull: RunnableItemConfigScope[T] =
      asRunnableItem(allModules = true, aggregated = true, crossScalaVersions = true)

    def asRunnableItem: RunnableItemConfigScope[T] =
      asRunnableItem(allModules = false, aggregated = false, crossScalaVersions = false)

    def asRunnableItem(
        allModules: Boolean,
        aggregated: Boolean,
        crossScalaVersions: Boolean): RunnableItemConfigScope[T] =
      RunnableItemConfigScope(RunnableSetSetting(setSetting), allModules, aggregated, crossScalaVersions)

  }

  final class RunnableCommandOps(command: String) {

    def asRunnableItemFull: RunnableItemConfigScope[Unit] =
      asRunnableItem(allModules = true, aggregated = true, crossScalaVersions = true)

    def asRunnableItem: RunnableItemConfigScope[Unit] =
      asRunnableItem(allModules = false, aggregated = false, crossScalaVersions = false)

    def asRunnableItem(
        allModules: Boolean,
        aggregated: Boolean,
        crossScalaVersions: Boolean): RunnableItemConfigScope[Unit] =
      RunnableItemConfigScope(RunnableProcess(command), allModules, aggregated, crossScalaVersions)

  }

}
