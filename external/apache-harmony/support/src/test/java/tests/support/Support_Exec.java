/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import junit.framework.TestCase;

public class Support_Exec extends TestCase {

    /**
     * Exec java returns the exitCode, and stdOut and stdErr as strings
     */
    public static Object[] runJava(List<String> args, String[] envp,
                                   boolean displayOutput)
            throws IOException, InterruptedException {
        String executable = System.getProperty("java.home");
        if (!executable.endsWith(File.separator)) {
            executable += File.separator;
        }
        executable += "bin" + File.separator + "java";

        // parse hy.test.vmargs if was given
        String testVMArgs = System.getProperty("hy.test.vmargs");
        if (testVMArgs != null) {
            StringTokenizer st = new StringTokenizer(testVMArgs, " ");
            int i = 0; // add at the beginning but maintain order
            while (st.hasMoreTokens()) {
                args.add(i++, st.nextToken());
            }
        }

        return run(executable, args, envp, displayOutput);
    }

    /**
     * Exec command returns the exitCode, and stdOut and stdErr as strings
     */
    public static Object[] run(String command, List<String> args, String[] envp,
                               boolean displayOutput)
            throws IOException, InterruptedException {
        Object[] arr = exec(command, args, envp, displayOutput);

        Process proc = (Process) arr[0];
        StringBuilder output = new StringBuilder();
        InputStream in = proc.getInputStream();
        int result;
        byte[] bytes = new byte[1024];

        while ((result = in.read(bytes)) != -1) {
            output.append(new String(bytes, 0, result));
            if (displayOutput) {
                System.out.write(bytes, 0, result);
            }
        }

        in.close();
        proc.waitFor();
        int exitCode = proc.exitValue();
        proc.destroy();
        return new Object[] {
            Integer.valueOf(exitCode),
            output.toString(),
            ((StringBuilder)arr[1]).toString()
        };
    }
        
    /**
     *  This function returns the output of the process as a string
     */
    public static String execJava(String[] args, String[] classpath,
                                  boolean displayOutput)
        throws IOException, InterruptedException {
        Object[] arr =
            execJavaCommon(args, classpath, null, displayOutput, true);

        return getProcessOutput(arr, displayOutput, true);
    }

    /**
     * This function returns the output of the process as a string
     */
    public static String execJava(String[] args, String[] classpath,
                                  String[] envp, boolean displayOutput)
            throws IOException, InterruptedException {
        Object[] arr =
            execJavaCommon(args, classpath, envp, displayOutput, false);

        return getProcessOutput(arr, displayOutput, true);
    }

    private static String getProcessOutput(Object[] arr, boolean displayOutput,
                                           boolean checkStderr)
            throws IOException, InterruptedException {
        Process proc = (Process) arr[0];
        StringBuilder output = new StringBuilder();
        InputStream in = proc.getInputStream();
        int result;
        byte[] bytes = new byte[1024];

        while ((result = in.read(bytes)) != -1) {
            output.append(new String(bytes, 0, result));
            if (displayOutput) {
                System.out.write(bytes, 0, result);
            }
        }

        in.close();
        proc.waitFor();
        if (checkStderr) {
            checkStderr(arr);
        }
        proc.destroy();

        return output.toString();
    }

    public static void checkStderr(Object[] execArgs) {
            StringBuilder errBuf = (StringBuilder) execArgs[1];

            synchronized (errBuf) {
                if (errBuf.length() > 0) {
                    fail(errBuf.toString());
                }
            }
    }

    public static Object[] execJava2(String[] args, String[] classpath,
                                     boolean displayOutput)
            throws IOException, InterruptedException {
        return execJavaCommon(args, classpath, null, displayOutput, true);
    }

    private static Object[] execJavaCommon(String[] args, String[] classpath,
                                           String[] envp,
                                           boolean displayOutput,
                                           boolean appendToSystemClassPath)
            throws IOException, InterruptedException {
        // this function returns the resulting process from the exec
        ArrayList<String> execArgs = null;
        StringBuilder classPathString = new StringBuilder();
        StringBuilder command;

        execArgs = new ArrayList<String>(3 + args.length);

        // construct the name of executable file
        String executable = System.getProperty("java.home");
        if (!executable.endsWith(File.separator)) {
            executable += File.separator;
        }
        executable += "bin" + File.separator + "java";

        // add classpath string
        if (classpath != null) {
            for (String element : classpath) {
                classPathString.append(File.pathSeparator);
                classPathString.append(element);
            }
        }
        if (appendToSystemClassPath) {
            execArgs.add("-cp");
            execArgs.add(System.getProperty("java.class.path") +
                         classPathString);
        } else {
            if (classpath != null) {
                execArgs.add("-cp");
                execArgs.add(classPathString.toString());
            }
        }

        // parse hy.test.vmargs if was given
        String testVMArgs = System.getProperty("hy.test.vmargs");
        if (testVMArgs != null) {
            StringTokenizer st = new StringTokenizer(testVMArgs, " ");

            while (st.hasMoreTokens()) {
                execArgs.add(st.nextToken());
            }
        }

        // add custom args given as parameter
        for (String arg : args) {
            execArgs.add(arg);
        }
        return exec(executable, execArgs, envp, displayOutput);
    }

    private static Object[] exec(String command, List<String> args,
                                 String[] envp,
                                 boolean displayOutput)
            throws IOException, InterruptedException {
        // this function returns the resulting process from the exec
        args.add(0, command);

        if (displayOutput) {
            StringBuilder commandLine;
            // construct command line string and print it to stdout
            commandLine = new StringBuilder(args.get(0));
            for (int i = 1; i < args.size(); i++) {
                commandLine.append(" ");
                commandLine.append(args.get(i));
            }
            System.out.println("Exec: " + commandLine.toString());
            System.out.println();
        }

        // execute java process
        final Process proc =
            Runtime.getRuntime().exec(args.toArray(new String[args.size()]),
                                      envp);

        final StringBuilder errBuf = new StringBuilder();
        Thread errThread = new Thread(new Runnable() {
                public void run() {
                    synchronized (errBuf) {
                        InputStream err;
                        int result;
                        byte[] bytes = new byte[1024];

                        synchronized (proc) {
                            proc.notifyAll();
                        }

                        err = proc.getErrorStream();
                        try {
                            while ((result = err.read(bytes)) != -1) {
                                System.err.write(bytes, 0, result);
                                errBuf.append(new String(bytes));
                            }
                            err.close();
                        } catch (IOException e) {
                            ByteArrayOutputStream out =
                                new ByteArrayOutputStream();
                            PrintStream printer = new PrintStream(out);
                            
                            e.printStackTrace();
                            e.printStackTrace(printer);
                            printer.close();
                            errBuf.append(new String(out.toByteArray()));
                        }
                    }
                }
            });

        synchronized (proc) {
            errThread.start();
            // wait for errThread to start
            int count = 0;
            boolean isFinished = false;
            while(!isFinished) {
                try {
                    proc.wait();
                    isFinished = true;
                } catch (InterruptedException e) {
                    if(++count == 2) {
                        throw e;
                    }
                }
            }
            if(count > 0) {
                Thread.currentThread().interrupt();
            }
        }
        return new Object[] { proc, errBuf };
    }
}
