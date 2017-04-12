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

package sbtorgpolicies.templates

import sbtorgpolicies.templates.utils.sectionSep

object sectionTemplates {

  val changelogSectionTemplate: String =
    """
      |## {{date}} - Version {{version}}
      |
      |Release changes:
      |
      |{{changes}}
      |""".stripMargin

  val copyrightSectionTitle: String = "Copyright"

  val copyrightSectionTemplate: String =
    s"""${sectionSep(copyrightSectionTitle)}
       |# Copyright
       |
       |{{name}} is designed and developed by {{organizationName}}
       |
       |Copyright (C) {{year}} {{organizationName}}. <{{organizationHomePage}}>
       |
       |${sectionSep(copyrightSectionTitle, start = false)}""".stripMargin

  val badgesSectionTitle: String = "Badges"

  val badgesSectionTemplate: String =
    s"""${sectionSep(badgesSectionTitle)}
       |
       |{{badges}}
       |
       |${sectionSep(badgesSectionTitle, start = false)}""".stripMargin

}
