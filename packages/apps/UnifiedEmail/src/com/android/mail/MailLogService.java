/*******************************************************************************
 *      Copyright (C) 2013 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail;

import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Pair;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * A write-only device for sensitive logs. Turned on only during debugging.
 *
 * Dump valuable system state by sending a local broadcast to the associated activity.
 * Broadcast receivers are responsible for dumping state as they see fit.
 * This service is only started when the log level is high, so there is no risk of user
 * data being logged by mistake.
 *
 * To add logging to this service, call {@link #log(String, String, Object...)} with a tag name,
 * which is a class name, like "AbstractActivityController", which is a unique ID. Then, add to the
 * resulting buffer any information of interest at logging time. This is kept in a ring buffer,
 * which is overwritten with new information.
 */
public class MailLogService extends Service {
    /**
     * This is the top level flag that enables this service.
     */
    public static boolean DEBUG_ENABLED = false;

    /** The tag which needs to be turned to DEBUG to get logging going. */
    protected static final String LOG_TAG = LogTag.getLogTag();

    /**
     * A circular buffer of {@value #SIZE} lines.  To  insert into this buffer,
     * call the {@link #put(String)} method.  To retrieve the most recent logs,
     * call the {@link #toString()} method.
     */
    private static class CircularBuffer {
        // We accept fifty lines of input.
        public static final int SIZE = 50;
        /** The actual list of strings to be printed. */
        final Queue<Pair<Long, String>> mList = new LinkedList<Pair<Long, String>>();
        /** The current size of the buffer */
        int mCurrentSize = 0;

        /** Create an empty log buffer. */
        private CircularBuffer() {
            // Do nothing
        }

        /** Get the current timestamp */
        private static String dateToString(long timestamp) {
            final Date d = new Date(timestamp);
            return String.format("%d-%d %d:%d:%d: ", d.getDay(), d.getMonth(), d.getHours(),
                    d.getMinutes(), d.getSeconds());
        }

        /**
         * Insert a log message into the buffer. This might evict the oldest message if the log
         * is at capacity.
         * @param message a log message for this buffer.
         */
        private synchronized void put(String message) {
            if (mCurrentSize == SIZE) {
                // At capacity, we'll remove the head, and add to the tail. Size is unchanged.
                mList.remove();
            } else {
                // Less than capacity. Adding a new element at the end.
                mCurrentSize++;
            }
            // Add the current timestamp along with the message.
            mList.add(new Pair<Long, String>(System.currentTimeMillis(), message));
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            for (final Pair<Long, String> s : mList) {
                // Print the timestamp as an actual date, and then the message.
                builder.append(dateToString(s.first));
                builder.append(s.second);
                // Put a newline at the end of each log line.
                builder.append("\n");
            }
            return builder.toString();
        }
    }

    /** Header printed at the start of the dump. */
    private static final String HEADER = "**** MailLogService ***\n";
    /** Map of current tag -> log. */
    private static final Map<String, CircularBuffer> sLogs = new HashMap<String, CircularBuffer>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Return the circular buffer associated with this tag, or create a new buffer if none is
     * currently associated.
     * @param tag a string to identify a unique tag.
     * @return a circular buffer associated with a string tag.
     */
    private static CircularBuffer getOrCreate(String tag) {
        if (sLogs.containsKey(tag)) {
            return sLogs.get(tag);
        }
        // Create a new CircularBuffer with this tag
        final CircularBuffer buffer = new CircularBuffer();
        sLogs.put(tag, buffer);
        return buffer;
    }

    /**
     * Return true if the logging level is high enough for this service to function.
     * @return true if this service is functioning at the current log level. False otherwise.
     */
    public static boolean isLoggingLevelHighEnough() {
        return LogUtils.isLoggable(LOG_TAG, LogUtils.DEBUG);
    }

    /**
     * Add to the log for the tag given.
     * @param tag a unique tag to add the message to
     * @param format a string format for the message
     * @param args optional list of arguments for the format.
     */
    public static void log(String tag, String format, Object... args) {
        if (!DEBUG_ENABLED || !isLoggingLevelHighEnough()) {
            return;
        }
        // The message we are printing.
        final String logMessage = String.format(format, args);
        // Find the circular buffer to go with this tag, or create a new one.
        getOrCreate(tag).put(logMessage);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (!DEBUG_ENABLED) {
            return;
        }
        writer.print(HEADER);
        // Go through all the tags, and write them all out sequentially.
        for (final String tag : sLogs.keySet()) {
            // Write out a sub-header: Logging for tag "MyModuleName"
            writer.append("Logging for tag: \"");
            writer.append(tag);
            writer.append("\"\n");

            writer.append(sLogs.get(tag).toString());
        }
        // Go through all the buffers.
        super.dump(fd, writer,args);
    }
}
