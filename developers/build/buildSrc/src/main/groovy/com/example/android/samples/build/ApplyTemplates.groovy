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

import freemarker.cache.FileTemplateLoader
import freemarker.cache.MultiTemplateLoader
import freemarker.cache.TemplateLoader
import freemarker.template.Configuration
import freemarker.template.DefaultObjectWrapper
import freemarker.template.Template
import org.gradle.api.GradleException
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction


class ApplyTemplates extends SourceTask {
    /**
     * Freemarker context object
     */
    def Configuration cfg = new freemarker.template.Configuration()

    /**
     * The root directory for output files. All output file paths
     * are assumed to be relative to this root.
     */
    @OutputDirectory
    public outputDir = project.projectDir

    /**
     * Include directory. The templates in this directory will not be
     * processed directly, but will be accessible to other templates
     * via the <#include> directive.
     */
    def include = project.file("$project.projectDir/templates/include")

    /**
     * List of file extensions that indicate a file to be processed, rather
     * than simply copied.
     */
    def extensionsToProcess = ['ftl']

    /**
     * List of file extensions that should be completely ignored by this
     * task. File extensions that appear in neither this list nor the list
     * specified by {@link #extensionsToProcess} are copied into the destination
     * without processing.
     */
    def extensionsToIgnore = ['ftli']

    /**
     * A String -> String closure that transforms a (relative) input path into a
     * (relative) output path. This closure is responsible for any alterations to
     * the output path, including pathname substitution and extension removal.
     */
    Closure<String> filenameTransform

    /**
     * The hash which will be passed to the freemarker template engine. This hash
     * is used by the freemarker script as input data.
     * The hash should contain a key named "meta". The template processor will add
     * processing data to this key.
     */
    def parameters

    /**
     * The main action for this task. Visits each file in the source directories and
     * either processes, copies, or ignores it. The action taken for each file depends
     * on the contents of {@link #extensionsToProcess} and {@link #extensionsToIgnore}.
     */
    @TaskAction
    def applyTemplate() {
        // Create a list of Freemarker template loaders based on the
        // source tree(s) of this task. The loader list establishes a virtual
        // file system for freemarker templates; the template language can
        // load files, and each load request will have its path resolved
        // against this set of loaders.
        println "Gathering template load locations:"
        def List loaders = []
        source.asFileTrees.each {
            src ->
                println "    ${src.dir}"
                loaders.add(0, new FileTemplateLoader(project.file(src.dir)))
        }

        // Add the include path(s) to the list of loaders.
        println "Gathering template include locations:"
        include = project.fileTree(include)
        include.asFileTrees.each {
            inc ->
                println "    ${inc.dir}"
                loaders.add(0, new FileTemplateLoader(project.file(inc.dir)))
        }
        // Add the loaders to the freemarker config
        cfg.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[1])))

        // Set the wrapper that will be used to convert the template parameters hash into
        // the internal freemarker data model. The default wrapper is capable of handling a
        // mix of POJOs/POGOs and XML nodes, so we'll use that.
        cfg.setObjectWrapper(new DefaultObjectWrapper())

        // This is very much like setting the target SDK level in Android.
        cfg.setIncompatibleEnhancements("2.3.20")

        // Add an implicit <#include 'common.ftl' to the top of every file.
        // TODO: should probably be a parameter instead of hardcoded like this.
        cfg.addAutoInclude('common.ftl')

        // Visit every file in the source tree(s)
        def processTree = source.getAsFileTree()
        processTree.visit {
            FileVisitDetails input ->
                def inputFile = input.getRelativePath().toString()
                def outputFile = input.getRelativePath().getFile(project.file(outputDir))
                // Get the input and output files, and make sure the output path exists
                def renamedOutput = filenameTransform(outputFile.toString())
                outputFile = project.file(renamedOutput)

                if (input.directory){
                    // create the output directory. This probably will have already been
                    // created as part of processing the files *in* the directory, but
                    // do it here anyway to support empty directories.
                    outputFile.mkdirs()
                } else {
                    // We may or may not see the directory before we see the files
                    // in that directory, so create it here
                    outputFile.parentFile.mkdirs()

                    // Check the input file extension against the process/ignore list
                    def extension = "NONE"
                    def extensionPattern = ~/.*\.(\w*)$/
                    def extensionMatch = extensionPattern.matcher(inputFile)
                    if (extensionMatch.matches()) {
                        extension = extensionMatch[0][1]
                    }
                    // If the extension is in the process list, put the input through freemarker
                    if (extensionsToProcess.contains(extension)){
                        print '[freemarker] PROCESS: '
                        println "$inputFile -> $outputFile"

                        try {
                            def Template tpl = this.cfg.getTemplate(inputFile)
                            def FileWriter out = new FileWriter(outputFile)

                            // Add the output file path to parameters.meta so that the freemarker
                            // script can access it.
                            parameters.meta.put("outputFile", "${outputFile}")
                            tpl.process(parameters, out)
                        } catch (e) {
                            println e.message
                            throw new GradleException("Error processing ${inputFile}: ${e.message}")
                        }
                    } else if (!extensionsToIgnore.contains(extension)) {
                        // if it's not processed and not ignored, then it must be copied.
                        print '[freemarker] COPY: '
                        println "$inputFile -> $outputFile"
                        input.copyTo(outputFile);
                    }
                }
        }
    }
}
