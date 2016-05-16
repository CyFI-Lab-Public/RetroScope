/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package signature.io.html;

import org.antlr.stringtemplate.StringTemplate;

import signature.Version;
import signature.compare.model.IApiDelta;
import signature.compare.model.IClassDefinitionDelta;
import signature.compare.model.IDelta;
import signature.compare.model.IPackageDelta;
import signature.compare.model.impl.SigDelta;
import signature.io.IApiDeltaExternalizer;
import signature.model.IClassDefinition;
import signature.model.IPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HtmlDeltaExternalizer implements IApiDeltaExternalizer {

    private static final String OVERVIEW_PAGE_NAME = "changes.html";
    private static final String STYLE_SHEET_NAME = "styles.css";
    private static final String DELTA_FOLDER = "changes" + File.separator;

    public void externalize(String location, IApiDelta apiDelta)
            throws IOException {
        if (!location.endsWith(File.separator)) {
            location += File.separator;
        }

        File directory = new File(location);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        copyStyleSheet(location);

        Map<String, String> commonInfos = new HashMap<String, String>();
        commonInfos.put("creation_time", DateFormat.getDateTimeInstance()
                .format(new Date()));
        commonInfos.put("from_desc", apiDelta.getFrom().getName());
        commonInfos.put("to_desc", apiDelta.getTo().getName());

        // write overview page
        StringBuilder content = new StringBuilder();
        ApiOverviewPage apiOverviewPage = new ApiOverviewPage(apiDelta,
                commonInfos);
        apiOverviewPage.writeTo(content);
        writeToFile(location + OVERVIEW_PAGE_NAME, content.toString());

        // write package overview
        Set<IPackageDelta> changedPackages = SigDelta.getChanged(apiDelta
                .getPackageDeltas());
        if (!changedPackages.isEmpty()) {

            File file = new File(location + DELTA_FOLDER);
            if (!file.exists()) {
                file.mkdir();
            }

            for (IPackageDelta packageDelta : changedPackages) {
                content = new StringBuilder();
                PackageOverviewPage packagePage = new PackageOverviewPage(
                        packageDelta, commonInfos);
                packagePage.writeTo(content);
                IPackage aPackage = getAnElement(packageDelta);
                String packageOverviewFileName = location + DELTA_FOLDER
                        + "pkg_" + aPackage.getName() + ".html";
                writeToFile(packageOverviewFileName, content.toString());

                // write class overviews
                for (IClassDefinitionDelta classDelta : packageDelta
                        .getClassDeltas()) {
                    content = new StringBuilder();
                    ClassOverviewPage classPage = new ClassOverviewPage(
                            classDelta, commonInfos);
                    classPage.writeTo(content);
                    IClassDefinition aClass = getAnElement(classDelta);
                    String classOverviewFileName = location + DELTA_FOLDER
                            + aPackage.getName() + "." + aClass.getName()
                            + ".html";
                    writeToFile(classOverviewFileName, content.toString());
                }
            }
        }
        // write class overview
    }

    private static <T> T getAnElement(IDelta<T> delta) {
        if (delta.getFrom() != null) {
            return delta.getFrom();
        } else {
            return delta.getTo();
        }
    }

    private void copyStyleSheet(String directory) throws IOException {
        StringTemplate template = TemplateStore.getStringTemplate("Styles");
        template.setAttribute("version", Version.VERSION);
        writeToFile(directory + STYLE_SHEET_NAME, template.toString());
    }

    private void writeToFile(String fileName, String content)
            throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        fileOutputStream.write(content.getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
    }
}
