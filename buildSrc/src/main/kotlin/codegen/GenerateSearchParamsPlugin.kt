/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package codegen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

class GenerateSearchParamsPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension =
      target.extensions.create(
        "generateSearchParams",
        GenerateSearchParamsPluginExtension::class.java,
      )

    val generateSearchParamsTask =
      target.tasks.register(
        "generateSearchParamsTask",
        GenerateSearchParamsTask::class.java,
      ) {
        this.group = "Android FHIR"
        this.description = "Generate Search Params"
      }

    generateSearchParamsTask.configure {
      srcOutputDir.set(extension.srcOutputDir)
      testOutputDir.set(extension.testOutputDir)
    }
  }
}

interface GenerateSearchParamsPluginExtension {
  val srcOutputDir: DirectoryProperty
  val testOutputDir: DirectoryProperty
}
