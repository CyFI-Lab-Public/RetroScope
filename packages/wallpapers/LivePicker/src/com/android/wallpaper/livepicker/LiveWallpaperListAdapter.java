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

package com.android.wallpaper.livepicker;

import android.app.WallpaperInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.service.wallpaper.WallpaperService;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LiveWallpaperListAdapter extends BaseAdapter implements ListAdapter {
    private static final String LOG_TAG = "LiveWallpaperListAdapter";

    private final LayoutInflater mInflater;
    private final PackageManager mPackageManager;

    private List<LiveWallpaperInfo> mWallpapers;

    @SuppressWarnings("unchecked")
    public LiveWallpaperListAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPackageManager = context.getPackageManager();

        List<ResolveInfo> list = mPackageManager.queryIntentServices(
                new Intent(WallpaperService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);

        mWallpapers = generatePlaceholderViews(list.size());

        new LiveWallpaperEnumerator(context).execute(list);
    }

    private List<LiveWallpaperInfo> generatePlaceholderViews(int amount) {
        ArrayList<LiveWallpaperInfo> list = new ArrayList<LiveWallpaperInfo>(amount);
        for (int i = 0; i < amount; i++) {
            LiveWallpaperInfo info = new LiveWallpaperInfo();
            list.add(info);
        }
        return list;
    }

    public int getCount() {
        if (mWallpapers == null) {
            return 0;
        }
        return mWallpapers.size();
    }

    public Object getItem(int position) {
        return mWallpapers.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.live_wallpaper_entry, parent, false);

            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        LiveWallpaperInfo wallpaperInfo = mWallpapers.get(position);
        if (holder.thumbnail != null) {
            holder.thumbnail.setImageDrawable(wallpaperInfo.thumbnail);
        }

        if (holder.title != null && wallpaperInfo.info != null) {
            holder.title.setText(wallpaperInfo.info.loadLabel(mPackageManager));
            if (holder.thumbnail == null) {
                holder.title.setCompoundDrawablesWithIntrinsicBounds(null, wallpaperInfo.thumbnail,
                    null, null);
            }
        }

        return convertView;
    }

    public class LiveWallpaperInfo {
        public Drawable thumbnail;
        public WallpaperInfo info;
        public Intent intent;
    }

    private class ViewHolder {
        TextView title;
        ImageView thumbnail;
    }

    private class LiveWallpaperEnumerator extends
            AsyncTask<List<ResolveInfo>, LiveWallpaperInfo, Void> {
        private Context mContext;
        private int mWallpaperPosition;

        public LiveWallpaperEnumerator(Context context) {
            super();
            mContext = context;
            mWallpaperPosition = 0;
        }

        @Override
        protected Void doInBackground(List<ResolveInfo>... params) {
            final PackageManager packageManager = mContext.getPackageManager();

            List<ResolveInfo> list = params[0];

            final Resources res = mContext.getResources();
            BitmapDrawable galleryIcon = (BitmapDrawable) res.getDrawable(
                    R.drawable.livewallpaper_placeholder);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            paint.setTextAlign(Paint.Align.CENTER);
            Canvas canvas = new Canvas();

            Collections.sort(list, new Comparator<ResolveInfo>() {
                final Collator mCollator;

                {
                    mCollator = Collator.getInstance();
                }

                public int compare(ResolveInfo info1, ResolveInfo info2) {
                    return mCollator.compare(info1.loadLabel(packageManager),
                            info2.loadLabel(packageManager));
                }
            });

            for (ResolveInfo resolveInfo : list) {
                WallpaperInfo info = null;
                try {
                    info = new WallpaperInfo(mContext, resolveInfo);
                } catch (XmlPullParserException e) {
                    Log.w(LOG_TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                    continue;
                } catch (IOException e) {
                    Log.w(LOG_TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                    continue;
                }

                LiveWallpaperInfo wallpaper = new LiveWallpaperInfo();
                wallpaper.intent = new Intent(WallpaperService.SERVICE_INTERFACE);
                wallpaper.intent.setClassName(info.getPackageName(), info.getServiceName());
                wallpaper.info = info;

                Drawable thumb = info.loadThumbnail(packageManager);
                if (thumb == null) {
                    int thumbWidth = res.getDimensionPixelSize(
                            R.dimen.live_wallpaper_thumbnail_width);
                    int thumbHeight = res.getDimensionPixelSize(
                            R.dimen.live_wallpaper_thumbnail_height);

                    Bitmap thumbnail = Bitmap.createBitmap(thumbWidth, thumbHeight,
                            Bitmap.Config.ARGB_8888);

                    paint.setColor(res.getColor(R.color.live_wallpaper_thumbnail_background));
                    canvas.setBitmap(thumbnail);
                    canvas.drawPaint(paint);

                    galleryIcon.setBounds(0, 0, thumbWidth, thumbHeight);
                    galleryIcon.setGravity(Gravity.CENTER);
                    galleryIcon.draw(canvas);

                    String title = info.loadLabel(packageManager).toString();

                    paint.setColor(res.getColor(R.color.live_wallpaper_thumbnail_text_color));
                    paint.setTextSize(
                            res.getDimensionPixelSize(R.dimen.live_wallpaper_thumbnail_text_size));

                    canvas.drawText(title, (int) (thumbWidth * 0.5),
                            thumbHeight - res.getDimensionPixelSize(
                                    R.dimen.live_wallpaper_thumbnail_text_offset), paint);

                    thumb = new BitmapDrawable(res, thumbnail);
                }
                wallpaper.thumbnail = thumb;
                publishProgress(wallpaper);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(LiveWallpaperInfo...infos) {
            for (LiveWallpaperInfo info : infos) {
                info.thumbnail.setDither(true);
                if (mWallpaperPosition < mWallpapers.size()) {
                    mWallpapers.set(mWallpaperPosition, info);
                } else {
                    mWallpapers.add(info);
                }
                mWallpaperPosition++;
                LiveWallpaperListAdapter.this.notifyDataSetChanged();
            }
        }
    }
}
