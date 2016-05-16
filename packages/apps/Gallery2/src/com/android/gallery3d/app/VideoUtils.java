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

// Modified example based on mp4parser google code open source project.
// http://code.google.com/p/mp4parser/source/browse/trunk/examples/src/main/java/com/googlecode/mp4parser/ShortenExample.java

package com.android.gallery3d.app;

import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.util.SaveVideoFileInfo;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class VideoUtils {
    private static final String LOGTAG = "VideoUtils";
    private static final int DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024;

    /**
     * Remove the sound track.
     */
    public static void startMute(String filePath, SaveVideoFileInfo dstFileInfo)
            throws IOException {
        if (ApiHelper.HAS_MEDIA_MUXER) {
            genVideoUsingMuxer(filePath, dstFileInfo.mFile.getPath(), -1, -1,
                    false, true);
        } else {
            startMuteUsingMp4Parser(filePath, dstFileInfo);
        }
    }

    /**
     * Shortens/Crops tracks
     */
    public static void startTrim(File src, File dst, int startMs, int endMs)
            throws IOException {
        if (ApiHelper.HAS_MEDIA_MUXER) {
            genVideoUsingMuxer(src.getPath(), dst.getPath(), startMs, endMs,
                    true, true);
        } else {
            trimUsingMp4Parser(src, dst, startMs, endMs);
        }
    }

    private static void startMuteUsingMp4Parser(String filePath,
            SaveVideoFileInfo dstFileInfo) throws FileNotFoundException, IOException {
        File dst = dstFileInfo.mFile;
        File src = new File(filePath);
        RandomAccessFile randomAccessFile = new RandomAccessFile(src, "r");
        Movie movie = MovieCreator.build(randomAccessFile.getChannel());

        // remove all tracks we will create new tracks from the old
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());

        for (Track track : tracks) {
            if (track.getHandler().equals("vide")) {
                movie.addTrack(track);
            }
        }
        writeMovieIntoFile(dst, movie);
        randomAccessFile.close();
    }

    private static void writeMovieIntoFile(File dst, Movie movie)
            throws IOException {
        if (!dst.exists()) {
            dst.createNewFile();
        }

        IsoFile out = new DefaultMp4Builder().build(movie);
        FileOutputStream fos = new FileOutputStream(dst);
        FileChannel fc = fos.getChannel();
        out.getBox(fc); // This one build up the memory.

        fc.close();
        fos.close();
    }

    /**
     * @param srcPath the path of source video file.
     * @param dstPath the path of destination video file.
     * @param startMs starting time in milliseconds for trimming. Set to
     *            negative if starting from beginning.
     * @param endMs end time for trimming in milliseconds. Set to negative if
     *            no trimming at the end.
     * @param useAudio true if keep the audio track from the source.
     * @param useVideo true if keep the video track from the source.
     * @throws IOException
     */
    private static void genVideoUsingMuxer(String srcPath, String dstPath,
            int startMs, int endMs, boolean useAudio, boolean useVideo)
            throws IOException {
        // Set up MediaExtractor to read from the source.
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(srcPath);

        int trackCount = extractor.getTrackCount();

        // Set up MediaMuxer for the destination.
        MediaMuxer muxer;
        muxer = new MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        // Set up the tracks and retrieve the max buffer size for selected
        // tracks.
        HashMap<Integer, Integer> indexMap = new HashMap<Integer,
                Integer>(trackCount);
        int bufferSize = -1;
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            boolean selectCurrentTrack = false;

            if (mime.startsWith("audio/") && useAudio) {
                selectCurrentTrack = true;
            } else if (mime.startsWith("video/") && useVideo) {
                selectCurrentTrack = true;
            }

            if (selectCurrentTrack) {
                extractor.selectTrack(i);
                int dstIndex = muxer.addTrack(format);
                indexMap.put(i, dstIndex);
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    int newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    bufferSize = newSize > bufferSize ? newSize : bufferSize;
                }
            }
        }

        if (bufferSize < 0) {
            bufferSize = DEFAULT_BUFFER_SIZE;
        }

        // Set up the orientation and starting time for extractor.
        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
        retrieverSrc.setDataSource(srcPath);
        String degreesString = retrieverSrc.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (degreesString != null) {
            int degrees = Integer.parseInt(degreesString);
            if (degrees >= 0) {
                muxer.setOrientationHint(degrees);
            }
        }

        if (startMs > 0) {
            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        }

        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
        // for copying each sample and stop when we get to the end of the source
        // file or exceed the end time of the trimming.
        int offset = 0;
        int trackIndex = -1;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        BufferInfo bufferInfo = new BufferInfo();
        try {
            muxer.start();
            while (true) {
                bufferInfo.offset = offset;
                bufferInfo.size = extractor.readSampleData(dstBuf, offset);
                if (bufferInfo.size < 0) {
                    Log.d(LOGTAG, "Saw input EOS.");
                    bufferInfo.size = 0;
                    break;
                } else {
                    bufferInfo.presentationTimeUs = extractor.getSampleTime();
                    if (endMs > 0 && bufferInfo.presentationTimeUs > (endMs * 1000)) {
                        Log.d(LOGTAG, "The current sample is over the trim end time.");
                        break;
                    } else {
                        bufferInfo.flags = extractor.getSampleFlags();
                        trackIndex = extractor.getSampleTrackIndex();

                        muxer.writeSampleData(indexMap.get(trackIndex), dstBuf,
                                bufferInfo);
                        extractor.advance();
                    }
                }
            }

            muxer.stop();
        } catch (IllegalStateException e) {
            // Swallow the exception due to malformed source.
            Log.w(LOGTAG, "The source video file is malformed");
        } finally {
            muxer.release();
        }
        return;
    }

    private static void trimUsingMp4Parser(File src, File dst, int startMs, int endMs)
            throws FileNotFoundException, IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(src, "r");
        Movie movie = MovieCreator.build(randomAccessFile.getChannel());

        // remove all tracks we will create new tracks from the old
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());

        double startTime = startMs / 1000;
        double endTime = endMs / 1000;

        boolean timeCorrected = false;

        // Here we try to find a track that has sync samples. Since we can only
        // start decoding at such a sample we SHOULD make sure that the start of
        // the new fragment is exactly such a frame.
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (timeCorrected) {
                    // This exception here could be a false positive in case we
                    // have multiple tracks with sync samples at exactly the
                    // same positions. E.g. a single movie containing multiple
                    // qualities of the same video (Microsoft Smooth Streaming
                    // file)
                    throw new RuntimeException(
                            "The startTime has already been corrected by" +
                            " another track with SyncSample. Not Supported.");
                }
                startTime = correctTimeToSyncSample(track, startTime, false);
                endTime = correctTimeToSyncSample(track, endTime, true);
                timeCorrected = true;
            }
        }

        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            long startSample = -1;
            long endSample = -1;

            for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
                TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
                for (int j = 0; j < entry.getCount(); j++) {
                    // entry.getDelta() is the amount of time the current sample
                    // covers.

                    if (currentTime <= startTime) {
                        // current sample is still before the new starttime
                        startSample = currentSample;
                    }
                    if (currentTime <= endTime) {
                        // current sample is after the new start time and still
                        // before the new endtime
                        endSample = currentSample;
                    } else {
                        // current sample is after the end of the cropped video
                        break;
                    }
                    currentTime += (double) entry.getDelta()
                            / (double) track.getTrackMetaData().getTimescale();
                    currentSample++;
                }
            }
            movie.addTrack(new CroppedTrack(track, startSample, endSample));
        }
        writeMovieIntoFile(dst, movie);
        randomAccessFile.close();
    }

    private static double correctTimeToSyncSample(Track track, double cutHere,
            boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
            TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
            for (int j = 0; j < entry.getCount(); j++) {
                if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                    // samples always start with 1 but we start with zero
                    // therefore +1
                    timeOfSyncSamples[Arrays.binarySearch(
                            track.getSyncSamples(), currentSample + 1)] = currentTime;
                }
                currentTime += (double) entry.getDelta()
                        / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

}
