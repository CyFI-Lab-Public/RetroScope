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

package com.android.musicfx;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

/**
 * Provide backwards compatibility for existing control panels.
 * There are two major parts to this:
 * - a BroadcastReceiver that listens for installed or removed packages, and
 *   enables or disables control panel receivers as needed to ensure that only
 *   one control panel package will receive the broadcasts that applications end
 * - a high priority control panel activity that redirects to the currently
 *   selected control panel activity
 *
 */
public class Compatibility {

    private final static String TAG = "MusicFXCompat";
    // run "setprop log.tag.MusicFXCompat DEBUG" to turn on logging
    private final static boolean LOG = Log.isLoggable(TAG, Log.DEBUG);


    /**
     * This activity has an intent filter with the highest possible priority, so
     * it will always be chosen. It then looks up the correct control panel to
     * use and launches that.
     */
    public static class Redirector extends Activity {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            log("Compatibility Activity called from " + getCallingPackage());
            Intent i = new Intent(getIntent());
            i.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            SharedPreferences pref = getSharedPreferences("musicfx", MODE_PRIVATE);
            String defPackage = pref.getString("defaultpanelpackage", null);
            String defName = pref.getString("defaultpanelname", null);
            log("read " + defPackage + "/" + defName + " as default");
            if (defPackage == null || defName == null) {
                Log.e(TAG, "no default set!");
                // use the built-in panel
                i.setComponent(new ComponentName(this, ActivityMusic.class));
                // also save it as the default
                Intent updateIntent = new Intent(this, Service.class);
                updateIntent.putExtra("defPackage", getPackageName());
                updateIntent.putExtra("defName", ActivityMusic.class.getName());
                startService(updateIntent);
            } else {
                i.setComponent(new ComponentName(defPackage, defName));
            }
            startActivity(i);
            finish();
        }
    }

    /**
     * This BroadcastReceiver responds to BOOT_COMPLETED, PACKAGE_ADDED,
     * PACKAGE_REPLACED and PACKAGE_REMOVED intents. When run, it checks
     * to see whether the active control panel needs to be updated:
     * - if there is no default, it picks one
     * - if a new control panel is installed, it becomes the default
     * It then enables the open/close receivers in the active control panel,
     * and disables them in the others.
     */
    public static class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {

            log("received");
            Intent updateIntent = new Intent(context, Service.class);
            updateIntent.putExtra("reason", intent);
            context.startService(updateIntent);
        }
    }

    public static class Service extends IntentService {

        PackageManager mPackageManager;

        public Service() {
            super("CompatibilityService");
        }

        @Override
        protected void onHandleIntent(final Intent intent) {
            log("handleintent");
            if (mPackageManager == null) {
                mPackageManager = getPackageManager();
            }

            String defPackage = intent.getStringExtra("defPackage");
            String defName = intent.getStringExtra("defName");
            if (defPackage != null && defName != null) {
                setDefault(defPackage, defName);
                return;
            }

            Intent packageIntent = intent.getParcelableExtra("reason");
            Bundle b = packageIntent.getExtras();
            if (b != null) b.size();
            log("intentservice saw: " + packageIntent + " " + b);
            // TODO, be smarter about package upgrades (which results in three
            // broadcasts: removed, added, replaced)
            Uri packageUri = packageIntent.getData();
            String updatedPackage = null;
            if (packageUri != null) {
                updatedPackage = packageUri.toString().substring(8);
                pickDefaultControlPanel(updatedPackage);
            }
        }

        private void pickDefaultControlPanel(String updatedPackage) {

            ResolveInfo defPanel = null;
            ResolveInfo otherPanel = null;
            ResolveInfo thisPanel = null;
            Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            List<ResolveInfo> ris = mPackageManager.queryIntentActivities(i, PackageManager.GET_DISABLED_COMPONENTS);
            log("found: " + ris.size());
            SharedPreferences pref = getSharedPreferences("musicfx", MODE_PRIVATE);
            String savedDefPackage = pref.getString("defaultpanelpackage", null);
            String savedDefName = pref.getString("defaultpanelname", null);
            log("saved default: " + savedDefName);
            for (ResolveInfo foo: ris) {
                if (foo.activityInfo.name.equals(Compatibility.Redirector.class.getName())) {
                    log("skipping " + foo);
                    continue;
                }
                log("considering " + foo);
                if (foo.activityInfo.name.equals(savedDefName) &&
                        foo.activityInfo.packageName.equals(savedDefPackage) &&
                        foo.activityInfo.enabled) {
                    log("default: " + savedDefName);
                    defPanel = foo;
                    break;
                } else if (foo.activityInfo.packageName.equals(updatedPackage)) {
                    log("choosing newly installed package " + updatedPackage);
                    otherPanel = foo;
                } else if (otherPanel == null && !foo.activityInfo.packageName.equals(getPackageName())) {
                    otherPanel = foo;
                } else {
                    thisPanel = foo;
                }
            }

            if (defPanel == null) {
                // pick a default control panel
                if (otherPanel == null) {
                    if (thisPanel == null) {
                        Log.e(TAG, "No control panels found!");
                        return;
                    }
                    otherPanel = thisPanel;
                }
                defPanel = otherPanel;
            }

            // Now that we have selected a default control panel activity, ensure
            // that the broadcast receiver(s) in that same package are enabled,
            // and the ones in the other packages are disabled.
            String defPackage = defPanel.activityInfo.packageName;
            String defName = defPanel.activityInfo.name;
            setDefault(defPackage, defName);
        }

        private void setDefault(String defPackage, String defName) {
            Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            List<ResolveInfo> ris = mPackageManager.queryBroadcastReceivers(i, PackageManager.GET_DISABLED_COMPONENTS);
            setupReceivers(ris, defPackage);
            // The open and close receivers are likely the same, but they may not be.
            i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            ris = mPackageManager.queryBroadcastReceivers(i, PackageManager.GET_DISABLED_COMPONENTS);
            setupReceivers(ris, defPackage);

            // Write the selected default to the prefs so that the Redirector activity
            // knows which one to use.
            SharedPreferences pref = getSharedPreferences("musicfx", MODE_PRIVATE);
            Editor ed = pref.edit();
            ed.putString("defaultpanelpackage", defPackage);
            ed.putString("defaultpanelname", defName);
            ed.commit();
            log("wrote " + defPackage + "/" + defName + " as default");
        }

        private void setupReceivers(List<ResolveInfo> ris, String defPackage) {
            // TODO - we may need to keep track of active sessions and send "open session"
            // broadcast to newly enabled receivers, while sending "close session" to
            // receivers that are about to be disabled. We could also consider just
            // killing the process hosting the disabled components.
            for (ResolveInfo foo: ris) {
                ComponentName comp = new ComponentName(foo.activityInfo.packageName, foo.activityInfo.name);
                if (foo.activityInfo.packageName.equals(defPackage)) {
                    log("enabling receiver " + foo);
                    mPackageManager.setComponentEnabledSetting(comp,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                } else {
                    log("disabling receiver " + foo);
                    mPackageManager.setComponentEnabledSetting(comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            }
        }
    }

    private static void log(String out) {
        if (LOG) {
            Log.d(TAG, out);
        }
    }
}
