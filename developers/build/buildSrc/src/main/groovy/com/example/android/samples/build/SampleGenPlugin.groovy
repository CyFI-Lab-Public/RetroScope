/*
* Copyright 2013 The Android Open Source Project
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
package com.example.android.samples.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.GradleBuild
/**
 * Plugin to expose build rules for sample generation and packaging.
 */
class SampleGenPlugin implements Plugin {

    /**
     * Creates a new sample generator task based on the supplied sources.
     *
     * @param name Name of the new task
     * @param sources Source tree that this task should process
     */
    void createTask(
            Project project,
            String name,
            SampleGenProperties props,
            def sources,
            def destination) {
        project.task ([type:ApplyTemplates], name,  {
            sources.each { tree ->
                source += tree
            }
            outputDir = destination
            include = props.templatesInclude()
            filenameTransform = {s -> props.getOutputForInput(s)}
            parameters = props.templateParams()
        })
    }


    @Override
    void apply(project) {
        project.extensions.create("samplegen", SampleGenProperties)
        project.samplegen.project = project
            SampleGenProperties samplegen = project.samplegen
            project.task('create') {
                if (project.gradle.startParameter.taskNames.contains('create')) {
                    samplegen.getCreationProperties()
                }

            }

            project.task('refresh') {
                samplegen.getRefreshProperties()
            }

        project.afterEvaluate({
            createTask(project,
                    'processTemplates',
                    samplegen,
                    samplegen.templates(),
                    samplegen.targetProjectPath)
            createTask(project,
                    'processCommon',
                    samplegen,
                    samplegen.common(),
                    samplegen.targetCommonPath())


            project.task([type: GradleBuild], 'bootstrap', {
                buildFile = "${samplegen.targetProjectPath}/build.gradle"
                dir = samplegen.targetProjectPath
                tasks = ["refresh"]
            })
            project.bootstrap.dependsOn(project.processTemplates)
            project.bootstrap.dependsOn(project.processCommon)
            project.create.dependsOn(project.bootstrap)

            project.refresh.dependsOn(project.processTemplates)
            project.refresh.dependsOn(project.processCommon)

            // People get nervous when they see a task with no actions, so...
            project.create << {println "Project creation finished."}
            project.refresh << {println "Project refresh finished."}

        })
    }


}
