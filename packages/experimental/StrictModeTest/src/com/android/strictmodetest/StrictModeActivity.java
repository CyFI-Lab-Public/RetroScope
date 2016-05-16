/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.strictmodetest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AndroidException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import dalvik.system.BlockGuard;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

public class StrictModeActivity extends Activity {

    private static final String TAG = "StrictModeActivity";
    private static final Uri SYSTEM_SETTINGS_URI = Uri.parse("content://settings/system");

    private ContentResolver cr;

    private final static class SimpleConnection implements ServiceConnection {
        public IService stub = null;
        public void onServiceConnected(ComponentName name, IBinder service) {
            stub = IService.Stub.asInterface(service);
            Log.v(TAG, "Service connected: " + name);
        }
        public void onServiceDisconnected(ComponentName name) {
            stub = null;
            Log.v(TAG, "Service disconnected: " + name);
        }
    }

    private final SimpleConnection mLocalServiceConn = new SimpleConnection();
    private final SimpleConnection mRemoteServiceConn = new SimpleConnection();

    private SQLiteDatabase mDb;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        cr = getContentResolver();
        mDb = openOrCreateDatabase("foo.db", MODE_PRIVATE, null);

        final Button readButton = (Button) findViewById(R.id.read_button);
        readButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    SharedPreferences prefs = getSharedPreferences("foo", 0);
                    try {
                        Cursor c = null;
                        try {
                            c = mDb.rawQuery("SELECT * FROM foo", null);
                        } finally {
                            if (c != null) c.close();
                        }
                    } catch (android.database.sqlite.SQLiteException e) {
                        Log.e(TAG, "SQLiteException: " + e);
                    }
                }
            });

        final Button writeButton = (Button) findViewById(R.id.write_button);
        writeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mDb.execSQL("CREATE TABLE IF NOT EXISTS FOO (a INT)");
                    SharedPreferences prefs = getSharedPreferences("foo", 0);
                    prefs.edit().putLong("time", System.currentTimeMillis()).commit();
                }
            });

        final Button writeLoopButton = (Button) findViewById(R.id.write_loop_button);
        writeLoopButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    long startTime = SystemClock.uptimeMillis();
                    int iters = 1000;
                    BlockGuard.Policy policy = BlockGuard.getThreadPolicy();
                    for (int i = 0; i < iters; ++i) {
                        policy.onWriteToDisk();
                    }
                    long endTime = SystemClock.uptimeMillis();
                    Log.d(TAG, "Time for " + iters + ": " + (endTime - startTime) + ", avg=" +
                          (endTime - startTime) / (double) iters);
                }
            });

        final Button dnsButton = (Button) findViewById(R.id.dns_button);
        dnsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "Doing DNS lookup for www.l.google.com... "
                          + "(may be cached by InetAddress)");
                    try {
                        InetAddress[] addrs = InetAddress.getAllByName("www.l.google.com");
                        for (int i = 0; i < addrs.length; ++i) {
                            Log.d(TAG, "got: " + addrs[i]);
                        }
                    } catch (java.net.UnknownHostException e) {
                        Log.d(TAG, "DNS error: " + e);
                    }

                    // Now try a random hostname to evade libcore's
                    // DNS caching.
                    try {
                        String random = "" + Math.random();
                        random = random.substring(random.indexOf(".") + 1);
                        String domain = random + ".livejournal.com";
                        InetAddress addr = InetAddress.getByName(domain);
                        Log.d(TAG, "for random domain " + domain + ": " + addr);
                    } catch (java.net.UnknownHostException e) {
                    }
                }
            });

        final Button httpButton = (Button) findViewById(R.id.http_button);
        httpButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        // Note: not using AndroidHttpClient, as that comes with its
                        // own pre-StrictMode network-on-Looper thread check.  The
                        // intent of this test is that we test the network stack's
                        // instrumentation for StrictMode instead.
                        DefaultHttpClient httpClient = new DefaultHttpClient();
                        HttpResponse res = httpClient.execute(
                            new HttpGet("http://www.android.com/favicon.ico"));
                        Log.d(TAG, "Fetched http response: " + res);
                    } catch (IOException e) {
                        Log.d(TAG, "HTTP fetch error: " + e);
                    }
                }
            });

        final Button http2Button = (Button) findViewById(R.id.http2_button);
        http2Button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        // Usually this ends up tripping in DNS resolution,
                        // so see http3Button below, which connects directly to an IP
                        InputStream is = new URL("http://www.android.com/")
                                .openConnection()
                                .getInputStream();
                        Log.d(TAG, "Got input stream: " + is);
                    } catch (IOException e) {
                        Log.d(TAG, "HTTP fetch error: " + e);
                    }
                }
            });

        final Button http3Button = (Button) findViewById(R.id.http3_button);
        http3Button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        // One of Google's web IPs, as of 2010-06-16....
                        InputStream is = new URL("http://74.125.19.14/")
                                .openConnection()
                                .getInputStream();
                        Log.d(TAG, "Got input stream: " + is);
                    } catch (IOException e) {
                        Log.d(TAG, "HTTP fetch error: " + e);
                    }
                }
            });

        final Button binderLocalButton = (Button) findViewById(R.id.binder_local_button);
        binderLocalButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        boolean value = mLocalServiceConn.stub.doDiskWrite(123 /* dummy */);
                        Log.d(TAG, "local writeToDisk returned: " + value);
                    } catch (RemoteException e) {
                        Log.d(TAG, "local binderButton error: " + e);
                    }
                }
            });

        final Button binderRemoteButton = (Button) findViewById(R.id.binder_remote_button);
        binderRemoteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        boolean value = mRemoteServiceConn.stub.doDiskWrite(1);
                        Log.d(TAG, "remote writeToDisk #1 returned: " + value);
                        value = mRemoteServiceConn.stub.doDiskWrite(2);
                        Log.d(TAG, "remote writeToDisk #2 returned: " + value);
                    } catch (RemoteException e) {
                        Log.d(TAG, "remote binderButton error: " + e);
                    }
                }
            });

        final Button binderOneWayButton = (Button) findViewById(R.id.binder_oneway_button);
        binderOneWayButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        Log.d(TAG, "doing oneway disk write over Binder.");
                        mRemoteServiceConn.stub.doDiskOneWay();
                    } catch (RemoteException e) {
                        Log.d(TAG, "remote binderButton error: " + e);
                    }
                }
            });

        final Button binderCheckButton = (Button) findViewById(R.id.binder_check_button);
        binderCheckButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int policy;
                    try {
                        policy = mLocalServiceConn.stub.getThreadPolicy();
                        Log.d(TAG, "local service policy: " + policy);
                        policy = mRemoteServiceConn.stub.getThreadPolicy();
                        Log.d(TAG, "remote service policy: " + policy);
                    } catch (RemoteException e) {
                        Log.d(TAG, "binderCheckButton error: " + e);
                    }
                }
            });

        final Button serviceDumpButton = (Button) findViewById(R.id.service_dump);
        serviceDumpButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "About to do a service dump...");
                    File file = new File("/sdcard/strictmode-service-dump.txt");
                    FileOutputStream output = null;
                    final StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
                    try {
                        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
                        output = new FileOutputStream(file);
                        StrictMode.setThreadPolicy(oldPolicy);
                        boolean dumped = Debug.dumpService("cpuinfo",
                                                           output.getFD(), new String[0]);
                        Log.d(TAG, "Dumped = " + dumped);
                    } catch (IOException e) {
                        Log.e(TAG, "Can't dump service", e);
                    } finally {
                        StrictMode.setThreadPolicy(oldPolicy);
                    }
                    Log.d(TAG, "Did service dump.");
                }
            });

        final Button lingerCloseButton = (Button) findViewById(R.id.linger_close_button);
        lingerCloseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    closeWithLinger(true);
                }
            });

        final Button nonlingerCloseButton = (Button) findViewById(R.id.nonlinger_close_button);
        nonlingerCloseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    closeWithLinger(false);
                }
            });

        final Button leakCursorButton = (Button) findViewById(R.id.leak_cursor_button);
        leakCursorButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    final StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
                    try {
                        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                               .detectLeakedSqlLiteObjects()
                                               .penaltyLog()
                                               .penaltyDropBox()
                                               .build());
                        mDb.execSQL("CREATE TABLE IF NOT EXISTS FOO (a INT)");
                        Cursor c = mDb.rawQuery("SELECT * FROM foo", null);
                        c = null;  // never close it
                        Runtime.getRuntime().gc();
                    } finally {
                        StrictMode.setVmPolicy(oldPolicy);
                    }

                }
            });

        final Button customButton = (Button) findViewById(R.id.custom_button);
        customButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    StrictMode.noteSlowCall("my example call");
                }
            });

        final Button gcInstanceButton = (Button) findViewById(R.id.gc_instance_button);
        gcInstanceButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ArrayList<DummyObject> list = new ArrayList<DummyObject>();
                    list.add(new DummyObject());
                    list.add(new DummyObject());

                    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                                           .setClassInstanceLimit(DummyObject.class, 1)
                                           .penaltyLog()
                                           .penaltyDropBox()
                                           .build());
                    StrictMode.conditionallyCheckInstanceCounts();
                    list.clear();
                }
            });

        final CheckBox checkNoWrite = (CheckBox) findViewById(R.id.policy_no_write);
        final CheckBox checkNoRead = (CheckBox) findViewById(R.id.policy_no_reads);
        final CheckBox checkNoNetwork = (CheckBox) findViewById(R.id.policy_no_network);
        final CheckBox checkCustom = (CheckBox) findViewById(R.id.policy_custom);
        final CheckBox checkPenaltyLog = (CheckBox) findViewById(R.id.policy_penalty_log);
        final CheckBox checkPenaltyDialog = (CheckBox) findViewById(R.id.policy_penalty_dialog);
        final CheckBox checkPenaltyDeath = (CheckBox) findViewById(R.id.policy_penalty_death);
        final CheckBox checkPenaltyDropBox = (CheckBox) findViewById(R.id.policy_penalty_dropbox);
        final CheckBox checkPenaltyFlash = (CheckBox) findViewById(R.id.policy_penalty_flash);
        final CheckBox checkPenaltyNetworkDeath = (CheckBox) findViewById(R.id.policy_penalty_network_death);

        View.OnClickListener changePolicy = new View.OnClickListener() {
                public void onClick(View v) {
                    StrictMode.ThreadPolicy.Builder newPolicy = new StrictMode.ThreadPolicy.Builder();
                    if (checkNoWrite.isChecked()) newPolicy.detectDiskWrites();
                    if (checkNoRead.isChecked()) newPolicy.detectDiskReads();
                    if (checkNoNetwork.isChecked()) newPolicy.detectNetwork();
                    if (checkCustom.isChecked()) newPolicy.detectCustomSlowCalls();
                    if (checkPenaltyLog.isChecked()) newPolicy.penaltyLog();
                    if (checkPenaltyDialog.isChecked()) newPolicy.penaltyDialog();
                    if (checkPenaltyDeath.isChecked()) newPolicy.penaltyDeath();
                    if (checkPenaltyDropBox.isChecked()) newPolicy.penaltyDropBox();
                    if (checkPenaltyFlash.isChecked()) newPolicy.penaltyFlashScreen();
                    if (checkPenaltyNetworkDeath.isChecked()) newPolicy.penaltyDeathOnNetwork();
                    StrictMode.ThreadPolicy policy = newPolicy.build();
                    Log.v(TAG, "Changing policy to: " + policy);
                    StrictMode.setThreadPolicy(policy);
                }
            };
        checkNoWrite.setOnClickListener(changePolicy);
        checkNoRead.setOnClickListener(changePolicy);
        checkNoNetwork.setOnClickListener(changePolicy);
        checkCustom.setOnClickListener(changePolicy);
        checkPenaltyLog.setOnClickListener(changePolicy);
        checkPenaltyDialog.setOnClickListener(changePolicy);
        checkPenaltyDeath.setOnClickListener(changePolicy);
        checkPenaltyDropBox.setOnClickListener(changePolicy);
        checkPenaltyFlash.setOnClickListener(changePolicy);
        checkPenaltyNetworkDeath.setOnClickListener(changePolicy);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDb.close();
        mDb = null;
    }

    private void closeWithLinger(boolean linger) {
        Log.d(TAG, "Socket linger test; linger=" + linger);
        try {
            Socket socket = new Socket();
            socket.setSoLinger(linger, 5);
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error with linger close", e);
        }
    }

    private void fileReadLoop() {
        RandomAccessFile raf = null;
        File filename = getFileStreamPath("test.dat");
        try {
            long sumNanos = 0;
            byte[] buf = new byte[512];

            //raf = new RandomAccessFile(filename, "rw");
            //raf.write(buf);
            //raf.close();
            //raf = null;

            // The data's almost certainly cached -- it's not clear what we're testing here
            raf = new RandomAccessFile(filename, "r");
            raf.seek(0);
            raf.read(buf);
        } catch (IOException e) {
            Log.e(TAG, "File read failed", e);
        } finally {
            try { if (raf != null) raf.close(); } catch (IOException e) {}
        }
    }

    // Returns milliseconds taken, or -1 on failure.
    private long settingsWrite(int mode) {
        Cursor c = null;
        long startTime = SystemClock.uptimeMillis();
        // The database will take care of replacing duplicates.
        try {
            ContentValues values = new ContentValues();
            values.put("name", "dummy_for_testing");
            values.put("value", "" + startTime);
            Uri uri = cr.insert(SYSTEM_SETTINGS_URI, values);
            Log.v(TAG, "inserted uri: " + uri);
        } catch (SQLException e) {
            Log.w(TAG, "sqliteexception during write: " + e);
            return -1;
        }
        long duration = SystemClock.uptimeMillis() - startTime;
        return duration;
    }

    @Override public void onResume() {
        super.onResume();
        bindService(new Intent(this, LocalService.class),
                    mLocalServiceConn, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, RemoteService.class),
                    mRemoteServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override public void onPause() {
        super.onPause();
        unbindService(mLocalServiceConn);
        unbindService(mRemoteServiceConn);
    }

    private static class DummyObject {
        int foo;
    }
}
