/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.cts.tradefed.command;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.tradefed.build.CtsBuildProvider;
import com.android.cts.tradefed.result.ITestResultRepo;
import com.android.cts.tradefed.result.ITestSummary;
import com.android.cts.tradefed.result.PlanCreator;
import com.android.cts.tradefed.result.TestResultRepo;
import com.android.cts.tradefed.testtype.ITestPackageRepo;
import com.android.cts.tradefed.testtype.TestPackageRepo;
import com.android.tradefed.command.Console;
import com.android.tradefed.config.ArgsOptionParser;
import com.android.tradefed.config.ConfigurationException;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.RegexTrie;
import com.android.tradefed.util.TableFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Specialization of trade federation console that adds CTS commands to list plans and packages.
 */
public class CtsConsole extends Console {

    protected static final String ADD_PATTERN = "a(?:dd)?";

    private CtsBuildHelper mCtsBuild = null;

    CtsConsole() {
        super();
    }

    @Override
    public void run() {
        printLine(String.format("Android CTS %s", CtsBuildProvider.CTS_BUILD_VERSION));
        super.run();
    }

    /**
     * Adds the 'list packages' and 'list plans' commands
     */
    @Override
    protected void setCustomCommands(RegexTrie<Runnable> trie, List<String> genericHelp,
            Map<String, String> commandHelp) {
        trie.put(new Runnable() {
            @Override
            public void run() {
                CtsBuildHelper ctsBuild = getCtsBuild();
                if (ctsBuild != null) {
                    listPlans(ctsBuild);
                }
            }
        }, LIST_PATTERN, "p(?:lans)?");
        trie.put(new Runnable() {
            @Override
            public void run() {
                CtsBuildHelper ctsBuild = getCtsBuild();
                if (ctsBuild != null) {
                    listPackages(ctsBuild);
                }
            }
        }, LIST_PATTERN, "packages");
        trie.put(new Runnable() {
            @Override
            public void run() {
                CtsBuildHelper ctsBuild = getCtsBuild();
                if (ctsBuild != null) {
                    listResults(ctsBuild);
                }
            }
        }, LIST_PATTERN, "r(?:esults)?");

        // find existing help for 'LIST_PATTERN' commands, and append these commands help
        String listHelp = commandHelp.get(LIST_PATTERN);
        if (listHelp == null) {
            // no help? Unexpected, but soldier on
            listHelp = new String();
        }
        String combinedHelp = listHelp +
                "\tp[lans]\t\tList all CTS test plans" + LINE_SEPARATOR +
                "\tpackages\tList all CTS packages" + LINE_SEPARATOR +
                "\tr[esults]\tList all CTS results" + LINE_SEPARATOR;
        commandHelp.put(LIST_PATTERN, combinedHelp);

        ArgRunnable<CaptureList> addDerivedCommand = new ArgRunnable<CaptureList>() {
            @Override
            public void run(CaptureList args) {
                // Skip 2 tokens to get past addPattern and "derivedplan"
                String[] flatArgs = new String[args.size() - 2];
                for (int i = 2; i < args.size(); i++) {
                    flatArgs[i - 2] = args.get(i).get(0);
                }
                CtsBuildHelper ctsBuild = getCtsBuild();
                if (ctsBuild != null) {
                    addDerivedPlan(ctsBuild, flatArgs);
                }
            }
        };
        trie.put(addDerivedCommand, ADD_PATTERN, "d(?:erivedplan?)", null);
        commandHelp.put(ADD_PATTERN, String.format(
                "%s help:" + LINE_SEPARATOR +
                "\tderivedplan      Add a derived plan" + LINE_SEPARATOR,
                ADD_PATTERN));
    }

    @Override
    protected String getConsolePrompt() {
        return "cts-tf > ";
    }

    @Override
    protected String getGenericHelpString(List<String> genericHelp) {
        StringBuilder helpBuilder = new StringBuilder();
        helpBuilder.append("CTS-tradefed host version ");
        helpBuilder.append(CtsBuildProvider.CTS_BUILD_VERSION);
        helpBuilder.append("\n\n");
        helpBuilder.append("CTS-tradefed is the test harness for running the Android ");
        helpBuilder.append("Compatibility Suite, built on top of the tradefed framework.\n\n");
        helpBuilder.append("Available commands and options\n");
        helpBuilder.append("Host:\n");
        helpBuilder.append("  help: show this message\n");
        helpBuilder.append("  help all: show the complete tradefed help\n");
        helpBuilder.append("  exit: gracefully exit the cts console, waiting till all ");
        helpBuilder.append("invocations are complete\n");
        helpBuilder.append("Run:\n");
        helpBuilder.append("  run cts --plan test_plan_name: run a test plan\n");
        helpBuilder.append("  run cts --package/-p : run a CTS test package\n");
        helpBuilder.append("  run cts --class/-c [--method/-m] : run a specific test class and/or");
        helpBuilder.append("method\n");
        helpBuilder.append("  run cts --continue-session session_ID: run all not executed ");
        helpBuilder.append("tests from a previous CTS session\n");
        helpBuilder.append("  run cts [options] --serial/s device_ID: run CTS on specified ");
        helpBuilder.append("device\n");
        helpBuilder.append("  run cts [options] --shards number_of_shards: shard a CTS run into ");
        helpBuilder.append("given number of independent chunks, to run on multiple devices in");
        helpBuilder.append("parallel\n");
        helpBuilder.append("  run cts --help/--help-all: get more help on running CTS\n");
        helpBuilder.append("List:\n");
        helpBuilder.append("  l/list d/devices: list connected devices and their state\n");
        helpBuilder.append("  l/list packages: list CTS test packages\n");
        helpBuilder.append("  l/list p/plans: list CTS test plans\n");
        helpBuilder.append("  l/list i/invocations: list invocations aka CTS test runs currently");
        helpBuilder.append("in progress\n");
        helpBuilder.append("  l/list c/commands: list commands: aka CTS test run commands ");
        helpBuilder.append("currently in the queue waiting to be allocated devices\n");
        helpBuilder.append("  l/list r/results: list CTS results currently present in the ");
        helpBuilder.append("repository\n");
        helpBuilder.append("Add:\n");
        helpBuilder.append("  add derivedplan --plan plane_name --session/-s session_id -r ");
        helpBuilder.append("[pass/fail/notExecuted/timeout]: derive a plan from the given ");
        helpBuilder.append("session\n");
        helpBuilder.append("Dump:\n");
        helpBuilder.append("  d/dump l/logs: dump the tradefed logs for all running invocations\n");
        helpBuilder.append("Options:\n");
        helpBuilder.append("  --disable-reboot : Do not reboot device after running some amount of tests.\n");
        return helpBuilder.toString();
    }

    private void listPlans(CtsBuildHelper ctsBuild) {
        FilenameFilter xmlFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        };
        for (File planFile : ctsBuild.getTestPlansDir().listFiles(xmlFilter)) {
            printLine(FileUtil.getBaseName(planFile.getName()));
        }
    }

    private void listPackages(CtsBuildHelper ctsBuild) {
        ITestPackageRepo testCaseRepo = new TestPackageRepo(ctsBuild.getTestCasesDir(), false);
        for (String packageUri : testCaseRepo.getPackageNames()) {
            printLine(packageUri);
        }
    }

    private void listResults(CtsBuildHelper ctsBuild) {
        TableFormatter tableFormatter = new TableFormatter();
        List<List<String>> table = new ArrayList<List<String>>();
        table.add(Arrays.asList("Session","Pass", "Fail","Not Executed","Start time","Plan name",
                "Device serial(s)"));
        ITestResultRepo testResultRepo = new TestResultRepo(ctsBuild.getResultsDir());
        for (ITestSummary result : testResultRepo.getSummaries()) {
            table.add(Arrays.asList(Integer.toString(result.getId()),
                    Integer.toString(result.getNumPassed()),
                    Integer.toString(result.getNumFailed()),
                    Integer.toString(result.getNumIncomplete()),
                    result.getTimestamp(),
                    result.getTestPlan(),
                    result.getDeviceSerials()));
        }
        tableFormatter.displayTable(table, new PrintWriter(System.out, true));
    }

    private void addDerivedPlan(CtsBuildHelper ctsBuild, String[] flatArgs) {
        PlanCreator creator = new PlanCreator();
        try {
            ArgsOptionParser optionParser = new ArgsOptionParser(creator);
            optionParser.parse(Arrays.asList(flatArgs));
            creator.createAndSerializeDerivedPlan(ctsBuild);
        } catch (ConfigurationException e) {
            printLine("Error: " + e.getMessage());
            printLine(ArgsOptionParser.getOptionHelp(false, creator));
        }
    }

    private CtsBuildHelper getCtsBuild() {
        if (mCtsBuild == null) {
            String ctsInstallPath = System.getProperty("CTS_ROOT");
            if (ctsInstallPath != null) {
                mCtsBuild = new CtsBuildHelper(new File(ctsInstallPath));
                try {
                    mCtsBuild.validateStructure();
                } catch (FileNotFoundException e) {
                    printLine(String.format("Invalid cts install: %s", e.getMessage()));
                    mCtsBuild = null;
                }
            } else {
                printLine("Could not find CTS install location: CTS_ROOT env variable not set");
            }
        }
        return mCtsBuild;
    }

    public static void main(String[] args) throws InterruptedException, ConfigurationException {
        Console console = new CtsConsole();
        Console.startConsole(console, args);
    }
}
