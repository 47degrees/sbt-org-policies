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

import sbt._

case class RunnableItemConfigScope[T](
    item: RunnableItem[T],
    allModules: Boolean = false,
    aggregated: Boolean = false,
    crossScalaVersions: Boolean = false)

case class SetSetting[T](setting: SettingKey[T], value: T)

sealed trait RunnableItem[T]

case class RunnableTask[T](task: TaskKey[T]) extends RunnableItem[T]

case class RunnableSetSetting[T](setSetting: SetSetting[T]) extends RunnableItem[T]

case class RunnableProcess(process: String) extends RunnableItem[Unit]
