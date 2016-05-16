/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.wallpaper.livepicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.WallpaperInfo;
import android.os.Bundle;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

public class LiveWallpaperActivity extends Activity {
    private static final String LOG_TAG = "LiveWallpapersPicker";
    private static final int REQUEST_PREVIEW = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_wallpaper_base);

        Fragment fragmentView = getFragmentManager().findFragmentById(R.id.live_wallpaper_fragment);
        if (fragmentView == null) {
            /* When the screen is XLarge, the fragment is not included in the layout, so show it
             * as a dialog
             */
            DialogFragment fragment = WallpaperDialog.newInstance();
            fragment.show(getFragmentManager(), "dialog");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PREVIEW) {
            if (resultCode == RESULT_OK) finish();
        }
    }

    public static class WallpaperDialog extends DialogFragment implements
            AdapterView.OnItemClickListener{
        private static final String EMBEDDED_KEY = "com.android.wallpaper.livepicker."
                + "LiveWallpaperActivity$WallpaperDialog.EMBEDDED_KEY";
        private LiveWallpaperListAdapter mAdapter;
        private boolean mEmbedded;

        public static WallpaperDialog newInstance() {
            WallpaperDialog dialog = new WallpaperDialog();
            dialog.setCancelable(true);
            return dialog;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null && savedInstanceState.containsKey(EMBEDDED_KEY)) {
                mEmbedded = savedInstanceState.getBoolean(EMBEDDED_KEY);
            } else {
                mEmbedded = isInLayout();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putBoolean(EMBEDDED_KEY, mEmbedded);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            /* On orientation changes, the dialog is effectively "dismissed" so this is called
             * when the activity is no longer associated with this dying dialog fragment. We
             * should just safely ignore this case by checking if getActivity() returns null
             */
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int contentInset = getResources().getDimensionPixelSize(
                    R.dimen.dialog_content_inset);
            View view = generateView(getActivity().getLayoutInflater(), null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setNegativeButton(R.string.wallpaper_cancel, null);
            builder.setTitle(R.string.live_wallpaper_picker_title);
            builder.setView(view, contentInset, contentInset, contentInset, contentInset);
            return builder.create();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            /* If this fragment is embedded in the layout of this activity, then we should
             * generate a view to display. Otherwise, a dialog will be created in
             * onCreateDialog()
             */
            if (mEmbedded) {
                return generateView(inflater, container);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private View generateView(LayoutInflater inflater, ViewGroup container) {
            View layout = inflater.inflate(R.layout.live_wallpaper_list, container, false);

            mAdapter = new LiveWallpaperListAdapter(getActivity());
            AdapterView<BaseAdapter> adapterView =
                    (AdapterView<BaseAdapter>) layout.findViewById(android.R.id.list);
            adapterView.setAdapter(mAdapter);
            adapterView.setOnItemClickListener(this);
            adapterView.setEmptyView(layout.findViewById(android.R.id.empty));
            return layout;
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            LiveWallpaperListAdapter.LiveWallpaperInfo wallpaperInfo =
                    (LiveWallpaperListAdapter.LiveWallpaperInfo) mAdapter.getItem(position);
            final Intent intent = wallpaperInfo.intent;
            final WallpaperInfo info = wallpaperInfo.info;
            LiveWallpaperPreview.showPreview(getActivity(), REQUEST_PREVIEW, intent, info);
        }
    }
}
