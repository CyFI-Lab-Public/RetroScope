/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.filesystemperf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Random;

import com.android.cts.util.MeasureRun;
import com.android.cts.util.MeasureTime;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;
import com.android.cts.util.ReportLog;
import com.android.cts.util.Stat;
import android.cts.util.SystemUtil;

import android.content.Context;
import android.util.Log;

public class FileUtil {
    private static final String TAG = "FileUtil";
    private static final Random mRandom = new Random(0);
    private static long mFileId = 0;
    /**
     * create array with different data per each call
     *
     * @param length
     * @param randomSeed
     * @return
     */
    public static byte[] generateRandomData(int length) {
        byte[] buffer = new byte[length];
        int val = mRandom.nextInt();
        for (int i = 0; i < length / 4; i++) {
            // in little-endian
            buffer[i * 4] = (byte)(val & 0x000000ff);
            buffer[i * 4 + 1] = (byte)((val & 0x0000ff00) >> 8);
            buffer[i * 4 + 2] = (byte)((val & 0x00ff0000) >> 16);
            buffer[i * 4 + 3] = (byte)((val & 0xff000000) >> 24);
            val++;
        }
        for (int i = (length / 4) * 4; i < length; i++) {
            buffer[i] = 0;
        }
        return buffer;
    }

    /**
     * create a new file under the given dirName.
     * Existing files will not be affected.
     * @param context
     * @param dirName
     * @return
     */
    public static File createNewFile(Context context, String dirName) {
        File topDir = new File(context.getFilesDir(), dirName);
        topDir.mkdir();
        String[] list = topDir.list();

        String newFileName;
        while (true) {
            newFileName = Long.toString(mFileId);
            boolean fileExist = false;
            for (String child : list) {
                if (child.equals(newFileName)) {
                    fileExist = true;
                    break;
                }
            }
            if (!fileExist) {
                break;
            }
            mFileId++;
        }
        mFileId++;
        //Log.i(TAG, "filename" + Long.toString(mFileId));
        return new File(topDir, newFileName);
    }

    /**
     * create multiple new files
     * @param context
     * @param dirName
     * @param count number of files to create
     * @return
     */
    public static File[] createNewFiles(Context context, String dirName, int count) {
        File[] files = new File[count];
        for (int i = 0; i < count; i++) {
            files[i] = createNewFile(context, dirName);
        }
        return files;
    }

    /**
     * write file with given byte array
     * @param file
     * @param data
     * @param append will append if set true. Otherwise, write from beginning
     * @throws IOException
     */
    public static void writeFile(File file, byte[] data, boolean append) throws IOException {
        final RandomAccessFile randomFile = new RandomAccessFile(file, "rwd"); // force O_SYNC
        if (append) {
            randomFile.seek(randomFile.length());
        } else {
            randomFile.seek(0L);
        }
        randomFile.write(data);
        randomFile.close();
    }

    /**
     * create a new file with given length.
     * @param context
     * @param dirName
     * @param length
     * @return
     * @throws IOException
     */
    public static File createNewFilledFile(Context context, String dirName, long length)
            throws IOException {
        final int BUFFER_SIZE = 10 * 1024 * 1024;
        File file = createNewFile(context, dirName);
        FileOutputStream out = new FileOutputStream(file);
        byte[] data = generateRandomData(BUFFER_SIZE);
        long written = 0;
        while (written < length) {
            out.write(data);
            written += BUFFER_SIZE;
        }
        out.flush();
        out.close();
        return file;
    }

    /**
     * remove given file or directory under the current app's files dir.
     * @param context
     * @param name
     */
    public static void removeFileOrDir(Context context, String name) {
        File entry = new File(context.getFilesDir(), name);
        if (entry.exists()) {
            removeEntry(entry);
        }
    }

    private static void removeEntry(File entry) {
        if (entry.isDirectory()) {
            String[] children = entry.list();
            for (String child : children) {
                removeEntry(new File(entry, child));
            }
        }
        Log.i(TAG, "delete file " + entry.getAbsolutePath());
        entry.delete();
    }

    /**
     * measure time taken for each IO run with amount R/W
     * @param count
     * @param run
     * @param readAmount returns amount of read in bytes for each interval.
     *        Value will not be written if /proc/self/io does not exist.
     * @param writeAmount returns amount of write in bytes for each interval.
     * @return time per each interval
     * @throws IOException
     */
    public static double[] measureIO(int count, double[] readAmount, double[] writeAmount,
            MeasureRun run)  throws Exception {
        double[] result = new double[count];
        File procIo = new File("/proc/self/io");
        boolean measureIo = procIo.exists() && procIo.canRead();
        long prev = System.currentTimeMillis();
        RWAmount prevAmount = new RWAmount();
        if (measureIo) {
            prevAmount = getRWAmount(procIo);
        }
        for (int i = 0; i < count; i++) {
            run.run(i);
            long current =  System.currentTimeMillis();
            result[i] = current - prev;
            prev = current;
            if (measureIo) {
                RWAmount currentAmount = getRWAmount(procIo);
                readAmount[i] = currentAmount.mRd - prevAmount.mRd;
                writeAmount[i] = currentAmount.mWr - prevAmount.mWr;
                prevAmount = currentAmount;
            }
        }
        return result;
    }

    private static class RWAmount {
        public double mRd = 0.0;
        public double mWr = 0.0;
    };

    private static RWAmount getRWAmount(File file) throws IOException {
        RWAmount amount = new RWAmount();

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        while((line = br.readLine())!= null) {
            if (line.startsWith("read_bytes")) {
                amount.mRd = Double.parseDouble(line.split(" ")[1]);
            } else if (line.startsWith("write_bytes")) {
                amount.mWr = Double.parseDouble(line.split(" ")[1]);
            }
        }
        br.close();
        return amount;
    }

    /**
     * get file size exceeding total memory size ( 2x total memory).
     * The size is rounded in bufferSize. And the size will be bigger than 400MB.
     * @param context
     * @param bufferSize
     * @return file size or 0 if there is not enough space.
     */
    public static long getFileSizeExceedingMemory(Context context, int bufferSize) {
        long freeDisk = SystemUtil.getFreeDiskSize(context);
        long memSize = SystemUtil.getTotalMemory(context);
        long diskSizeTarget = (2 * memSize / bufferSize) * bufferSize;
        final long minimumDiskSize = (512L * 1024L * 1024L / bufferSize) * bufferSize;
        if ( diskSizeTarget < minimumDiskSize ) {
            diskSizeTarget = minimumDiskSize;
        }
        if (diskSizeTarget > freeDisk) {
            Log.i(TAG, "Free disk size " + freeDisk + " too small");
            return 0;
        }
        return diskSizeTarget;
    }

    /**
     *
     * @param context
     * @param dirName
     * @param report
     * @param fileSize
     * @param bufferSize should be power of two
     * @throws IOException
     */
    public static void doRandomReadTest(Context context, String dirName, ReportLog report,
            long fileSize, int bufferSize) throws Exception {
        File file = FileUtil.createNewFilledFile(context,
                dirName, fileSize);

        final byte[] data = FileUtil.generateRandomData(bufferSize);
        Random random = new Random(0);
        final int totalReadCount = (int)(fileSize / bufferSize);
        final int[] readOffsets = new int[totalReadCount];
        for (int i = 0; i < totalReadCount; i++) {
            // align in buffer size
            readOffsets[i] = (int)(random.nextFloat() * (fileSize - bufferSize)) &
                    ~(bufferSize - 1);
        }
        final int runsInOneGo = 16;
        final int readsInOneMeasure = totalReadCount / runsInOneGo;

        final RandomAccessFile randomFile = new RandomAccessFile(file, "rw"); // do not need O_SYNC
        double[] rdAmount = new double[runsInOneGo];
        double[] wrAmount = new double[runsInOneGo];
        double[] times = FileUtil.measureIO(runsInOneGo, rdAmount, wrAmount, new MeasureRun() {

            @Override
            public void run(int i) throws IOException {
                Log.i(TAG, "starting " + i + " -th round");
                int start = i * readsInOneMeasure;
                int end = (i + 1) * readsInOneMeasure;
                for (int j = start; j < end; j++) {
                    randomFile.seek(readOffsets[j]);
                    randomFile.read(data);
                }
            }
        });
        randomFile.close();
        double[] mbps = ReportLog.calcRatePerSecArray((double)fileSize / runsInOneGo / 1024 / 1024,
                times);
        report.printArray("read throughput",
                mbps, ResultType.HIGHER_BETTER, ResultUnit.MBPS);
        // This is just the amount of IO returned from kernel. So this is performance neutral.
        report.printArray("read amount", rdAmount, ResultType.NEUTRAL, ResultUnit.BYTE);
        Stat.StatResult stat = Stat.getStat(mbps);

        report.printSummary("read throughput", stat.mAverage, ResultType.HIGHER_BETTER,
                ResultUnit.MBPS);
    }

    /**
     *
     * @param context
     * @param dirName
     * @param report
     * @param fileSize
     * @param bufferSize should be power of two
     * @throws IOException
     */
    public static void doRandomWriteTest(Context context, String dirName, ReportLog report,
            long fileSize, int bufferSize) throws Exception {
        File file = FileUtil.createNewFilledFile(context,
                dirName, fileSize);
        final byte[] data = FileUtil.generateRandomData(bufferSize);
        Random random = new Random(0);
        final int totalWriteCount = (int)(fileSize / bufferSize);
        final int[] writeOffsets = new int[totalWriteCount];
        for (int i = 0; i < totalWriteCount; i++) {
            writeOffsets[i] = (int)(random.nextFloat() * (fileSize - bufferSize)) &
                    ~(bufferSize - 1);
        }
        final int runsInOneGo = 16;
        final int writesInOneMeasure = totalWriteCount / runsInOneGo;

        final RandomAccessFile randomFile = new RandomAccessFile(file, "rwd"); // force O_SYNC
        double[] rdAmount = new double[runsInOneGo];
        double[] wrAmount = new double[runsInOneGo];
        double[] times = FileUtil.measureIO(runsInOneGo, rdAmount, wrAmount, new MeasureRun() {

            @Override
            public void run(int i) throws IOException {
                Log.i(TAG, "starting " + i + " -th round");
                int start = i * writesInOneMeasure;
                int end = (i + 1) * writesInOneMeasure;
                for (int j = start; j < end; j++) {
                    randomFile.seek(writeOffsets[j]);
                    randomFile.write(data);
                }
            }
        });
        randomFile.close();
        double[] mbps = ReportLog.calcRatePerSecArray((double)fileSize / runsInOneGo / 1024 / 1024,
                times);
        report.printArray("write throughput",
                mbps, ResultType.HIGHER_BETTER, ResultUnit.MBPS);
        report.printArray("write amount", wrAmount, ResultType.NEUTRAL,
                ResultUnit.BYTE);
        Stat.StatResult stat = Stat.getStat(mbps);

        report.printSummary("write throughput", stat.mAverage, ResultType.HIGHER_BETTER,
                ResultUnit.MBPS);
    }

    /**
     *
     * @param context
     * @param dirName
     * @param report
     * @param fileSize fileSize should be multiple of bufferSize.
     * @param bufferSize
     * @param numberRepetition
     * @throws IOException
     */
    public static void doSequentialUpdateTest(Context context, String dirName, ReportLog report,
            long fileSize, int bufferSize, int numberRepetition) throws Exception {
        File file = FileUtil.createNewFilledFile(context,
                dirName, fileSize);
        final byte[] data = FileUtil.generateRandomData(bufferSize);
        int numberRepeatInOneRun = (int)(fileSize / bufferSize);
        double[] mbpsAll = new double[numberRepetition * numberRepeatInOneRun];
        for (int i = 0; i < numberRepetition; i++) {
            Log.i(TAG, "starting " + i + " -th round");
            final RandomAccessFile randomFile = new RandomAccessFile(file, "rwd");  // force O_SYNC
            randomFile.seek(0L);
            double[] times = MeasureTime.measure(numberRepeatInOneRun, new MeasureRun() {

                @Override
                public void run(int i) throws IOException {
                    randomFile.write(data);
                }
            });
            randomFile.close();
            double[] mbps = ReportLog.calcRatePerSecArray((double)bufferSize / 1024 / 1024,
                    times);
            report.printArray(i + "-th round throughput",
                    mbps, ResultType.HIGHER_BETTER, ResultUnit.MBPS);
            ReportLog.copyArray(mbps, mbpsAll, i * numberRepeatInOneRun);
        }
        Stat.StatResult stat = Stat.getStat(mbpsAll);
        report.printSummary("update throughput", stat.mAverage, ResultType.HIGHER_BETTER,
                ResultUnit.MBPS);
    }
}
