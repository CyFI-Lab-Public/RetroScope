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

package com.android.cts.verifier.streamquality;

import com.android.cts.verifier.ArrayTestListAdapter;
import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestListAdapter;
import com.android.cts.verifier.TestListAdapter.TestListItem;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.widget.TextView;

import java.io.Serializable;

/**
 * Tests for verifying the quality of streaming videos.  Plays streams of different formats over
 * different protocols for a short amount of time, after which users can mark Pass/Fail depending
 * on the smoothness and subjective quality of the video.
 */
public class StreamingVideoActivity extends PassFailButtons.TestListActivity {
    /**
     * Simple storage class for stream information.
     */
    static class Stream implements Serializable {
        /**
         * Human-readable name for the stream.
         */
        public final String name;

        /**
         * Code name to append to the class name to identify this test.
         */
        public final String code;

        /**
         * URI of the stream
         */
        public final String uri;

        public Stream(String name, String code, String uri) {
            this.name = name;
            this.code = code;
            this.uri = uri;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || !(o instanceof Stream)) {
                return false;
            } else {
                Stream stream = (Stream) o;
                return name.equals(stream.name)
                        && code.equals(stream.code)
                        && uri.equals(stream.uri);
            }
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ uri.hashCode() ^ code.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Stream[] RTSP_STREAMS = {
        new Stream("H263 Video, AMR Audio", "rtsp_h263_amr",
                "rtsp://v2.cache7.c.youtube.com/video.3gp?"
               + "cid=0x271de9756065677e&fmt=13&user=android-device-test"),
        new Stream("MPEG4 SP Video, AAC Audio", "rtsp_mpeg4_aac",
                "rtsp://v2.cache7.c.youtube.com/video.3gp?"
                + "cid=0x271de9756065677e&fmt=13&user=android-device-test"),
        new Stream("H264 Base Video, AAC Audio", "rtsp_h264_aac",
                "rtsp://v2.cache7.c.youtube.com/video.3gp?"
                + "cid=0x271de9756065677e&fmt=13&user=android-device-test"),
    };

    private static final Stream[] HTTP_STREAMS = {
        new Stream("H263 Video, AMR Audio", "http_h263_amr", "http://v20.lscache8.c.youtube.com/"
                + "videoplayback?id=271de9756065677e"
                + "&itag=13&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&sparams=ip,ipbits,expire,ip,ipbits,expire,id,itag"
                + "&signature=372FA4C532AA49D14EAF049BCDA66460EEE161E9"
                + ".6D8BF096B73B7A68A7032CA8685053CFB498D30A"
                + "&key=test_key1&user=android-device-test"),
        new Stream("MPEG4 SP Video, AAC Audio", "http_mpeg4_aac",
                "http://v20.lscache8.c.youtube.com/"
                + "videoplayback?id=271de9756065677e"
                + "&itag=17&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&sparams=ip,ipbits,expire,ip,ipbits,expire,id,itag"
                + "&signature=3DCD3F79E045F95B6AF661765F046FB0440FF016"
                + ".06A42661B3AF6BAF046F012549CC9BA34EBC80A9"
                + "&key=test_key1&user=android-device-test"),
        new Stream("H264 Base Video, AAC Audio", "http_h264_aac",
                "http://v20.lscache8.c.youtube.com/"
                + "videoplayback?id=271de9756065677e"
                + "&itag=18&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&sparams=ip,ipbits,expire,ip,ipbits,expire,id,itag"
                + "&signature=1219C2B07AF0638C27916307A6093C0E43CB894E"
                + ".126B6B916BD57157782738AA7C03E59F21DBC168"
                + "&key=test_key1&user=android-device-test"),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_list);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.streaming_video, R.string.streaming_video_info, -1);

        TextView empty = (TextView) findViewById(android.R.id.empty);
        empty.setText(R.string.sv_no_data);

        getPassButton().setEnabled(false);
        setTestListAdapter(getStreamAdapter());
    }

    private TestListAdapter getStreamAdapter() {
        ArrayTestListAdapter adapter = new ArrayTestListAdapter(this);

        adapter.add(TestListItem.newCategory("RTSP"));
        for (Stream stream : RTSP_STREAMS) {
            addStreamToTests(adapter, stream);
        }

        adapter.add(TestListItem.newCategory("HTTP Progressive"));
        for (Stream stream : HTTP_STREAMS) {
            addStreamToTests(adapter, stream);
        }

        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updatePassButton();
            }
        });

        return adapter;
    }

    private void addStreamToTests(ArrayTestListAdapter streams, Stream stream) {
        Intent i = new Intent(StreamingVideoActivity.this, PlayVideoActivity.class);
        i.putExtra(PlayVideoActivity.EXTRA_STREAM, stream);
        streams.add(TestListItem.newTest(stream.name, PlayVideoActivity.getTestId(stream.code),
                i, null));
    }

    private void updatePassButton() {
        getPassButton().setEnabled(mAdapter.allTestsPassed());
    }
}
