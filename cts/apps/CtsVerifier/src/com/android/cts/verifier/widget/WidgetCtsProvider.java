/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.cts.verifier.widget;

import java.util.HashMap;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.RemoteViews;

import com.android.cts.verifier.R;

/**
 * The weather widget's AppWidgetProvider.
 */
public class WidgetCtsProvider extends AppWidgetProvider {
    class TextData {
        String title;
        String instruction;
        String dataP;
        String dataL;

        public TextData(String t, String i, String dp, String dl) {
            title = t;
            instruction = i;
            dataL = dl;
            dataP = dp;
        }
    }

    public static String PASS = "com.example.android.widgetcts.PASS";
    public static String FAIL = "com.example.android.widgetcts.FAIL";

    public static final int STATE_BEGIN = 0;
    public static final int STATE_VERIFY_SIZE_CALLBACK = 1;
    public static final int STATE_VERIFY_RESIZE = 2;
    public static final int STATE_VERIFY_COLLECTIONS = 3;
    public static final int STATE_VERIFY_HOME_OR_KEYGUARD_CALLBACK = 4;
    public static final int STATE_COMPLETE = 5;

    // If relevant, we want to verify the size callback first, before any
    // resizing.
    static HashMap<Integer, Integer> sStateMap = new HashMap<Integer, Integer>();
    static HashMap<Integer, Integer> sTestCount = new HashMap<Integer, Integer>();
    static HashMap<Integer, Integer> sPassCount = new HashMap<Integer, Integer>();

    private static int sSDKLevel = android.os.Build.VERSION.SDK_INT;

    public WidgetCtsProvider() {
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(PASS) || action.equals(FAIL)) {
            boolean pass = action.equals(PASS);

            int widgetId = (Integer) intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    -1);

            if (sStateMap.get(widgetId) != STATE_BEGIN && sStateMap.get(widgetId)
                    != STATE_COMPLETE) {
                if (!sTestCount.containsKey(widgetId)) {
                    sTestCount.put(widgetId, 0);
                }
                if (!sPassCount.containsKey(widgetId)) {
                    sPassCount.put(widgetId, 0);
                }

                sPassCount.put(widgetId, sPassCount.get(widgetId) + (pass ? 1 : 0));
                sTestCount.put(widgetId, sTestCount.get(widgetId) + 1);
            }
            gotoNextTest(widgetId);

            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            Bundle options = getAppWidgetOptions(mgr, widgetId);
            updateWidget(ctx, widgetId, mgr, options);
        }
        super.onReceive(ctx, intent);
    }

    @Override
    public void onDeleted(Context ctx, int[] ids) {
        for (int i = 0; i < ids.length; i++) {
            sStateMap.remove(ids[i]);
        }
    }

    Bundle getAppWidgetOptions(AppWidgetManager mgr, int widgetId) {
        if (sSDKLevel >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            return mgr.getAppWidgetOptions(widgetId);
        }
        return null;
    }

    void gotoNextTest(int widgetId) {
        int state = sStateMap.get(widgetId);
        boolean foundNextTest = false;
        do {
            state = state == STATE_COMPLETE ? state : state + 1;
            foundNextTest = shouldPerformTest(state);
        } while (state < STATE_COMPLETE && !foundNextTest);
        sStateMap.put(widgetId, state);
    }

    private boolean shouldPerformTest(int state) {
        if (state == STATE_VERIFY_SIZE_CALLBACK
                && sSDKLevel < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            return false;
        } else if (state == STATE_VERIFY_RESIZE
                && sSDKLevel < android.os.Build.VERSION_CODES.HONEYCOMB) {
            return false;
        } else if (state == STATE_VERIFY_COLLECTIONS
                && sSDKLevel < android.os.Build.VERSION_CODES.HONEYCOMB) {
            return false;
        } else if (state == STATE_VERIFY_HOME_OR_KEYGUARD_CALLBACK
                && sSDKLevel < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        }
        return true;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            int id = appWidgetIds[i];
            if (!sStateMap.containsKey(id)) {
                sStateMap.put(id, STATE_BEGIN);
            }
            updateWidget(context, appWidgetIds[i], appWidgetManager, null);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        updateWidget(context, appWidgetId, appWidgetManager, newOptions);
    }

    private TextData getInstruction(int state, Bundle options, int widgetId) {
        String title = null;
        String instruction = null;
        String dataL = null;
        String dataP = null;

        int widthP = -1;
        int heightP = -1;
        int widthL = -1;
        int heightL = -1;
        int category = -1;

        // We use nullness of options as a proxy for an sdk check >= JB
        if (options != null) {
            widthP = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1);
            heightP = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, -1);
            widthL = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, -1);
            heightL = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1);
            category = options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
        }

        int step = 1;
        if (sTestCount.containsKey(widgetId)) {
            step = sTestCount.get(widgetId) + 1;
        }
        if (state == STATE_BEGIN) {
            instruction = "This is a test of the widget framework";
        } else if (state == STATE_VERIFY_SIZE_CALLBACK) {
            title = "Step " + step + ": Verify dimensions";
            instruction = "Verify that the width and height indicated below constitute reasonable"
                    + " approximations of the widget's actual size:";
            dataP = "Width: " + widthP + "     Height: " + heightP;
            dataL = "Width: " + widthL + "     Height: " + heightL;
        } else if (state == STATE_VERIFY_RESIZE) {
            title = "Step " + step + ": Verify resizeability";
            instruction = "Verify that there is a functional affordance which allows this widget"
                    + " to be resized. For example, when picked up and dropped, there may be a "
                    + " frame with handles. (This is not a requirement for widgets hosted on "
                    + " a tablet keyguard).";
            if (sSDKLevel >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                instruction = instruction
                        + " Also, verify that after resize, the width and height below "
                        + "are updated accordingly.";
                dataP = "Width: " + widthP + "     Height: " + heightP;
                dataL = "Width: " + widthL + "     Height: " + heightL;
            }
        } else if (state == STATE_VERIFY_COLLECTIONS) {
            title = "Step " + step + ": Verify collections";
            instruction = "Verify that the widget contains a scrollable list of numbers from 1"
                    + " to " + WidgetCtsService.NUM_ITEMS;
        } else if (state == STATE_VERIFY_HOME_OR_KEYGUARD_CALLBACK) {
            title = "Step " + step + ": Verify category";
            instruction = "Verify that the text below accurately reflects whether this widget is"
                    + " on the home screen or the lock screen. ";
            if (category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
                dataL = dataP = "Widget is reportedly on: KEYGUARD";
            } else if (category == AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) {
                dataL = dataP = "Widget is reportedly on: HOME SCREEN";
            } else {
                dataL = dataP = "Error.";
            }
        } else if (state == STATE_COMPLETE) {
            title = "Test Complete";
            instruction = "This completes the test of the widget framework. " +
                    "Remove and re-add this widget to restart the test.";
            dataL = dataP = sPassCount.get(widgetId) + " of " + sTestCount.get(widgetId)
                    + " tests passed successfully.";
        }
        return new TextData(title, instruction, dataP, dataL);
    }

    private void updateWidget(Context context, int appWidgetId, AppWidgetManager appWidgetManager,
            Bundle newOptions) {
        // Pull them from the manager
        if (newOptions == null) {
            newOptions = getAppWidgetOptions(appWidgetManager, appWidgetId);
        }

        int baseLayout = R.layout.widget_layout;

        RemoteViews rv = new RemoteViews(context.getPackageName(), baseLayout);
        int state = sStateMap.get(appWidgetId);

        TextData text = getInstruction(state, newOptions, appWidgetId);
        rv.setTextViewText(R.id.instruction, text.instruction);

        // Update the title
        if (text.title != null) {
            rv.setTextViewText(R.id.title, text.title);
        }

        if (state == STATE_VERIFY_COLLECTIONS) {
            // Specify the service to provide data for the collection widget.
            // Note that we need to
            // embed the appWidgetId via the data otherwise it will be ignored.
            final Intent intent = new Intent(context, WidgetCtsService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            rv.setViewVisibility(R.id.list, View.VISIBLE);
            rv.setRemoteAdapter(appWidgetId, R.id.list, intent);
        } else {
            rv.setViewVisibility(R.id.list, View.GONE);
        }

        if (state == STATE_BEGIN) {
            rv.setViewVisibility(R.id.fail, View.GONE);
            rv.setTextViewText(R.id.pass, "Start Test");
        } else if (state == STATE_COMPLETE) {
            rv.setViewVisibility(R.id.fail, View.GONE);
            rv.setViewVisibility(R.id.pass, View.GONE);
        } else {
            rv.setViewVisibility(R.id.fail, View.VISIBLE);
            rv.setTextViewText(R.id.pass, "Pass");
        }

        final Intent pass = new Intent(context, WidgetCtsProvider.class);
        pass.setAction(WidgetCtsProvider.PASS);
        pass.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        pass.setData(Uri.parse(pass.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent passPendingIntent = PendingIntent.getBroadcast(context, 0, pass,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final Intent fail = new Intent(context, WidgetCtsProvider.class);
        fail.setAction(WidgetCtsProvider.FAIL);
        fail.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        fail.setData(Uri.parse(fail.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent failPendingIntent = PendingIntent.getBroadcast(context, 0, fail,
                PendingIntent.FLAG_UPDATE_CURRENT);

        rv.setOnClickPendingIntent(R.id.pass, passPendingIntent);
        rv.setOnClickPendingIntent(R.id.fail, failPendingIntent);

        RemoteViews rvL = null;
        if (text.dataP != null && !text.dataP.equals(text.dataL)) {
            rvL = rv.clone();

            System.out.println("hmmmm ok, made it innnnn");
            if (text.dataL != null) {
                rvL.setViewVisibility(R.id.data, View.VISIBLE);
                rvL.setTextViewText(R.id.data, text.dataL);
            } else {
                rvL.setViewVisibility(R.id.data, View.GONE);
            }
        }

        // Update the data
        if (text.dataP != null) {
            rv.setViewVisibility(R.id.data, View.VISIBLE);
            rv.setTextViewText(R.id.data, text.dataP);
        } else {
            rv.setViewVisibility(R.id.data, View.GONE);
        }

        RemoteViews rvFinal = rv;
        if (rvL != null) {
            rvFinal = new RemoteViews(rvL, rv);
        }

        appWidgetManager.updateAppWidget(appWidgetId, rvFinal);
    }
}
