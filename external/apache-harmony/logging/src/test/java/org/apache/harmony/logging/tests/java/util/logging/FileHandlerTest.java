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

package org.apache.harmony.logging.tests.java.util.logging;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.security.Permission;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.LoggingPermission;
import java.util.logging.XMLFormatter;

import junit.framework.TestCase;

import org.apache.harmony.logging.tests.java.util.logging.HandlerTest.NullOutputStream;
import org.apache.harmony.logging.tests.java.util.logging.util.EnvironmentHelper;

/**
 */
public class FileHandlerTest extends TestCase {

    static LogManager manager = LogManager.getLogManager();

    final static Properties props = new Properties();

    final static String className = FileHandlerTest.class.getName();

    final static StringWriter writer = new StringWriter();

    final static String USR_HOME_KEY = "user.home";

    final static String TMP_DIR_KEY = "java.io.tmpdir";

    final static String HOMEPATH = System.getProperty(USR_HOME_KEY);

    final static String TEMPPATH = System.getProperty(TMP_DIR_KEY);

    final static String SEP = File.separator;

    private final PrintStream err = System.err;

    private OutputStream errSubstituteStream = null;

    FileHandler handler;

    LogRecord r;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        manager.reset();
        initProps();
        File file = new File(TEMPPATH + SEP + "log");
        file.mkdir();
        manager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        handler = new FileHandler();
        r = new LogRecord(Level.CONFIG, "msg");
        errSubstituteStream = new NullOutputStream();
        System.setErr(new PrintStream(errSubstituteStream));
    }

    /**
     *
     */
    private void initProps() {
        props.clear();
        props.put("java.util.logging.FileHandler.level", "FINE");
        props.put("java.util.logging.FileHandler.filter", className
                + "$MockFilter");
        props.put("java.util.logging.FileHandler.formatter", className
                + "$MockFormatter");
        props.put("java.util.logging.FileHandler.encoding", "iso-8859-1");
        // limit to only two message
        props.put("java.util.logging.FileHandler.limit", "1000");
        // rotation count is 2
        props.put("java.util.logging.FileHandler.count", "2");
        // using append mode
        props.put("java.util.logging.FileHandler.append", "true");
        props
                .put("java.util.logging.FileHandler.pattern",
                        "%t/log/java%u.test");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        if (null != handler) {
            handler.close();
        }
        reset(TEMPPATH + SEP + "log", "");
        System.setErr(err);
        super.tearDown();
    }

    public void testConstructor_NoUsrHome() throws IOException {
        System.clearProperty(USR_HOME_KEY);

        try {
            new FileHandler("%h/log_NoUsrHome.log");
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        } finally {
            if (HOMEPATH != null) {
                System.setProperty(USR_HOME_KEY, HOMEPATH);
            }
        }
    }

    public void testConstructor_NoTmpDir() throws IOException {
        System.clearProperty(TMP_DIR_KEY);

        try {
            new FileHandler("%t/log_NoTmpDir.log");
        } finally {
            if (TEMPPATH != null) {
                System.setProperty(TMP_DIR_KEY, TEMPPATH);
            }
        }
        assertFalse(new File(TEMPPATH, "log_NoTmpDir.log").exists());
        assertTrue(new File(HOMEPATH, "log_NoTmpDir.log").exists());
        new File(HOMEPATH, "log_NoTmpDir.log").delete();
    }

    public void testConstructor_NoTmpDir_NoUsrHome() throws IOException {
        System.clearProperty(TMP_DIR_KEY);
        System.clearProperty(USR_HOME_KEY);

        try {
            new FileHandler("%t/log_NoTmpDir_NoUsrHome.log");
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        } finally {
            if (TEMPPATH != null) {
                System.setProperty(TMP_DIR_KEY, TEMPPATH);
            }
            if (HOMEPATH != null) {
                System.setProperty(USR_HOME_KEY, HOMEPATH);
            }
        }
    }

    public void testLock() throws Exception {
        FileOutputStream output = new FileOutputStream(TEMPPATH + SEP + "log"
                + SEP + "java1.test.0");
        FileHandler h = new FileHandler();
        h.publish(r);
        h.close();
        assertFileContent(TEMPPATH + SEP + "log", "java1.test.0", h
                .getFormatter(), "UTF-8");
        output.close();
    }

    /*
     * Class under test for void FileHandler()
     */
    public void testFileHandler() throws Exception {
        assertEquals(handler.getEncoding(), "iso-8859-1");
        assertTrue(handler.getFilter() instanceof MockFilter);
        assertTrue(handler.getFormatter() instanceof MockFormatter);
        assertEquals(handler.getLevel(), Level.FINE);
        assertNotNull(handler.getErrorManager());
        handler.publish(r);
        handler.close();
        // output 3 times, and all records left
        // append mode is true
        for (int i = 0; i < 3; i++) {
            handler = new FileHandler();
            handler.publish(r);
            handler.close();
        }
        assertFileContent(TEMPPATH + SEP + "log", "java0.test.0",
                new LogRecord[] { r, null, r, null, r, null, r },
                new MockFormatter(), "UTF-8");
    }

    public void testDefaultValue() throws Exception {
        handler.publish(r);
        handler.close();
        props.clear();
        manager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        handler = new FileHandler();
        assertNull(handler.getEncoding());
        assertNull(handler.getFilter());
        assertTrue(handler.getFormatter() instanceof XMLFormatter);
        assertEquals(handler.getLevel(), Level.ALL);
        assertNotNull(handler.getErrorManager());
        handler.publish(r);
        handler.close();
        // output 3 times, and only one record left
        // default append mode is false
        for (int i = 0; i < 3; i++) {
            handler = new FileHandler();
            handler.publish(r);
            handler.close();
        }
        assertFileContent(HOMEPATH, "java0.log", new XMLFormatter(), null);
    }

    private void assertFileContent(String homepath, String filename,
            Formatter formatter, String encoding) throws Exception {
        assertFileContent(homepath, filename, new LogRecord[] { r }, formatter, encoding);
    }

    private void assertFileContent(String homepath, String filename,
            LogRecord[] lr, Formatter formatter, String encoding) throws Exception {
        handler.close();
        String msg = "";
        // if formatter is null, the file content should be empty
        // else the message should be formatted given records
        if (null != formatter) {
            StringBuffer sb = new StringBuffer();
            sb.append(formatter.getHead(handler));
            for (int i = 0; i < lr.length; i++) {
                if (null == lr[i] && i < lr.length - 1) {
                    // if one record is null and is not the last record, means
                    // here is
                    // output completion point, should output tail, then output
                    // head
                    // (ready for next output)
                    sb.append(formatter.getTail(handler));
                    sb.append(formatter.getHead(handler));
                } else {
                    sb.append(formatter.format(lr[i]));
                }
            }
            sb.append(formatter.getTail(handler));
            msg = sb.toString();
        }
        byte[] bytes = new byte[msg.length()];
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(homepath
                    + SEP + filename));
            inputStream.read(bytes);
            if (encoding == null) {
                assertEquals(msg, new String(bytes));
            } else {
                assertEquals(msg, new String(bytes, encoding));
            }
            // assert has reached the end of the file
            assertEquals(-1, inputStream.read());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                // ignored
            }
            reset(homepath, filename);
        }
    }

    /**
     * Does a cleanup of given file
     *
     * @param homepath
     * @param filename
     */
    private void reset(String homepath, String filename) {
        File file = null;
        try {
            file = new File(homepath + SEP + filename);
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            file = new File(homepath + SEP + filename + ".lck");
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testLimitAndCount() throws Exception {
        handler.close();
        // very small limit value, count=2
        // output, rename current output file to the second generation file
        // close it and open a new file as rotation output
        handler = new FileHandler("%t/testLimitCount%g", 1, 2, false);
        handler.publish(r);
        handler.close();
        assertFileContent(TEMPPATH, "testLimitCount1", handler.getFormatter(), "UTF-8");

        // very small limit value, count=1
        // output once, rotate(equals to nothing output)
        handler = new FileHandler("%t/testLimitCount%g", 1, 1, false);
        handler.publish(r);
        handler.close();
        assertFileContent(TEMPPATH, "testLimitCount0", new LogRecord[0],
                handler.getFormatter(), "UTF-8");

        // normal case, limit is 60(>2*msg length <3*msg length), append is
        // false
        handler = new FileHandler("%t/testLimitCount%u", 60, 3, false);
        LogRecord[] rs = new LogRecord[10];
        // batch output twice to test the append mode
        for (int i = 0; i < 5; i++) {
            rs[i] = new LogRecord(Level.SEVERE, "msg" + i);
            handler.publish(rs[i]);
        }
        handler.close();
        handler = new FileHandler("%t/testLimitCount%u", 60, 3, false);
        for (int i = 5; i < 10; i++) {
            rs[i] = new LogRecord(Level.SEVERE, "msg" + i);
            handler.publish(rs[i]);
        }

        assertFileContent(TEMPPATH, "testLimitCount0.1", new LogRecord[] {
                rs[5], rs[6], rs[7] }, handler.getFormatter(), "UTF-8");
        assertFileContent(TEMPPATH, "testLimitCount0.0", new LogRecord[] {
                rs[8], rs[9] }, handler.getFormatter(), "UTF-8");

        // normal case, limit is 60(>2*msg length <3*msg length), append is true
        handler = new FileHandler("%t/testLimitCount%u", 60, 3, false);
        // batch output twice to test the append mode
        for (int i = 0; i < 5; i++) {
            rs[i] = new LogRecord(Level.SEVERE, "msg" + i);
            handler.publish(rs[i]);
        }
        handler.close();
        handler = new FileHandler("%t/testLimitCount%u", 60, 3, true);
        for (int i = 5; i < 10; i++) {
            rs[i] = new LogRecord(Level.SEVERE, "msg" + i);
            handler.publish(rs[i]);
        }
        handler.close();
        assertFileContent(TEMPPATH, "testLimitCount0.2", new LogRecord[] {
                rs[3], rs[4], null, rs[5] }, handler.getFormatter(), "UTF-8");
        assertFileContent(TEMPPATH, "testLimitCount0.1", new LogRecord[] {
                rs[6], rs[7], rs[8] }, handler.getFormatter(), "UTF-8");
        assertFileContent(TEMPPATH, "testLimitCount0.0",
                new LogRecord[] { rs[9] }, handler.getFormatter(), "UTF-8");

        FileHandler h1 = null;
        FileHandler h2 = null;
        try {
            File logDir = new File("log");
            reset("log", "");
            logDir.mkdir();
            h1 = new FileHandler("log/a", 0, 1);
            assertNotNull(h1);
            h2 = new FileHandler("log/a", 0, 1, false);
            assertNotNull(h2);
        } finally {
            try {
                h1.close();
            } catch (Exception e) {
            }
            try {
                h2.close();
            } catch (Exception e) {
            }
            reset("log", "");
        }
    }

    public void testInvalidProperty() throws Exception {
        props.put("java.util.logging.FileHandler.level", "null");
        props.put("java.util.logging.FileHandler.filter", className
                + "$MockFilte");
        props.put("java.util.logging.FileHandler.formatter", className
                + "$MockFormatte");
        props.put("java.util.logging.FileHandler.encoding", "ut");
        // limit to only two message
        props.put("java.util.logging.FileHandler.limit", "-1");
        // rotation count is 2
        props.put("java.util.logging.FileHandler.count", "-1");
        // using append mode
        props.put("java.util.logging.FileHandler.append", "bad");

        handler.close();

        manager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        handler = new FileHandler();
        assertEquals(Level.ALL, handler.getLevel());
        assertNull(handler.getFilter());
        assertTrue(handler.getFormatter() instanceof XMLFormatter);
        assertNull(handler.getEncoding());
        handler.close();

        props.put("java.util.logging.FileHandler.pattern", "");
        manager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        try {
            handler = new FileHandler();
            fail("shouldn't open file with empty name");
        } catch (NullPointerException e) {
        }
    }

    public void testInvalidParams() throws IOException {

        // %t and %p parsing can add file separator automatically
        FileHandler h1 = new FileHandler("%taaa");
        h1.close();
        File file = new File(TEMPPATH + SEP + "aaa");
        assertTrue(file.exists());
        reset(TEMPPATH, "aaa");

        // always parse special pattern
        try {
            h1 = new FileHandler("%t/%h");
            fail("should throw null exception");
        } catch (FileNotFoundException e) {
        }
        h1 = new FileHandler("%t%g");
        h1.close();
        file = new File(TEMPPATH + SEP + "0");
        assertTrue(file.exists());
        reset(TEMPPATH, "0");
        h1 = new FileHandler("%t%u%g");
        h1.close();
        file = new File(TEMPPATH + SEP + "00");
        assertTrue(file.exists());
        reset(TEMPPATH, "00");

        // this is normal case
        h1 = new FileHandler("%t/%u%g%%g");
        h1.close();
        file = new File(TEMPPATH + SEP + "00%g");
        assertTrue(file.exists());
        reset(TEMPPATH, "00%g");

        // multi separator has no effect
        h1 = new FileHandler("//%t//multi%g");
        h1.close();
        file = new File(TEMPPATH + SEP + "multi0");
        assertTrue(file.exists());
        reset(TEMPPATH, "multi0");

        // bad directory, IOException
        try {
            h1 = new FileHandler("%t/baddir/multi%g");
            fail("should throw IO exception");
        } catch (IOException e) {
        }
        file = new File(TEMPPATH + SEP + "baddir" + SEP + "multi0");
        assertFalse(file.exists());

        try {
            new FileHandler(null);
            fail("should throw null exception");
        } catch (NullPointerException e) {
        }
        try {
            handler.publish(null);
        } catch (NullPointerException e) {
            fail("should not throw NPE");
        }
        try {
            new FileHandler(null, false);
            fail("should throw null exception");
        } catch (NullPointerException e) {
        }
        try {
            // regression test for Harmony-1299
            new FileHandler("");
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new FileHandler("%t/java%u", 0, 0);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            new FileHandler("%t/java%u", -1, 1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    // set output stream still works, just like super StreamHandler
    public void testSetOutputStream() throws Exception {
        MockFileHandler handler = new MockFileHandler("%h/setoutput.log");
        handler.setFormatter(new MockFormatter());
        handler.publish(r);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        handler.publicSetOutputStream(out);
        handler.publish(r);
        handler.close();
        String msg = new String(out.toByteArray());
        Formatter f = handler.getFormatter();
        assertEquals(msg, f.getHead(handler) + f.format(r) + f.getTail(handler));
        assertFileContent(HOMEPATH, "setoutput.log", handler.getFormatter(), null);
    }

    /*
     * Class under test for void FileHandler(String)
     */
    public void testFileHandlerString() throws Exception {
        // test if unique ids not specified, it will append at the end
        // no generation number is used
        FileHandler h = new FileHandler("%t/log/string");
        FileHandler h2 = new FileHandler("%t/log/string");
        FileHandler h3 = new FileHandler("%t/log/string");
        FileHandler h4 = new FileHandler("%t/log/string");
        h.publish(r);
        h2.publish(r);
        h3.publish(r);
        h4.publish(r);
        h.close();
        h2.close();
        h3.close();
        h4.close();
        assertFileContent(TEMPPATH + SEP + "log", "string", h.getFormatter(), "UTF-8");
        assertFileContent(TEMPPATH + SEP + "log", "string.1", h.getFormatter(), "UTF-8");
        assertFileContent(TEMPPATH + SEP + "log", "string.2", h.getFormatter(), "UTF-8");
        assertFileContent(TEMPPATH + SEP + "log", "string.3", h.getFormatter(), "UTF-8");

        // default is append mode
        FileHandler h6 = new FileHandler("%t/log/string%u.log");
        h6.publish(r);
        h6.close();
        FileHandler h7 = new FileHandler("%t/log/string%u.log");
        h7.publish(r);
        h7.close();
        try {
            assertFileContent(TEMPPATH + SEP + "log", "string0.log", h
                    .getFormatter(), "UTF-8");
            fail("should assertion failed");
        } catch (Error e) {
        }
        File file = new File(TEMPPATH + SEP + "log");
        assertTrue(file.list().length <= 2);

        // test unique ids
        FileHandler h8 = new FileHandler("%t/log/%ustring%u.log");
        h8.publish(r);
        FileHandler h9 = new FileHandler("%t/log/%ustring%u.log");
        h9.publish(r);
        h9.close();
        h8.close();
        assertFileContent(TEMPPATH + SEP + "log", "0string0.log", h
                .getFormatter(), "UTF-8");
        assertFileContent(TEMPPATH + SEP + "log", "1string1.log", h
                .getFormatter(), "UTF-8");
        file = new File(TEMPPATH + SEP + "log");
        assertTrue(file.list().length <= 2);
    }

    public void testEmptyPattern_3params() throws SecurityException,
            IOException {
        // regression HARMONY-2421
        try {
            new FileHandler(new String(), 1, 1);
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testEmptyPattern_2params() throws SecurityException,
            IOException {
        // regression HARMONY-2421
        try {
            new FileHandler(new String(), true);
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testEmptyPattern_4params() throws SecurityException,
            IOException {
        // regression HARMONY-2421
        try {
            new FileHandler(new String(), 1, 1, true);
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    /*
     * mock classes
     */
    public static class MockFilter implements Filter {
        public boolean isLoggable(LogRecord record) {
            return !record.getMessage().equals("false");
        }
    }

    public static class MockFormatter extends Formatter {
        public String format(LogRecord r) {
            if (null == r) {
                return "";
            }
            return r.getMessage() + " by MockFormatter\n";
        }

        public String getTail(Handler h) {
            return "tail\n";
        }

        public String getHead(Handler h) {
            return "head\n";
        }
    }

    public static class MockFileHandler extends FileHandler {
        public MockFileHandler() throws IOException {
            super();
        }

        public MockFileHandler(String pattern) throws IOException {
            super(pattern);
        }

        public void publicSetOutputStream(OutputStream stream) {
            super.setOutputStream(stream);
        }
    }
}
