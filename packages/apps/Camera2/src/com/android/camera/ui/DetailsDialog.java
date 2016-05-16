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

package com.android.camera.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.camera.data.MediaDetails;
import com.android.camera2.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;

/**
 * Displays details (such as Exif) of a local media item.
 */
public class DetailsDialog {

    /**
     * Creates a dialog for showing media data.
     *
     * @param context the Android context.
     * @param mediaDetails the media details to display.
     * @return A dialog that can be made visible to show the media details.
     */
    public static Dialog create(Context context, MediaDetails mediaDetails) {
        ListView detailsList = (ListView) LayoutInflater.from(context).inflate(
                R.layout.details_list, null, false);
        detailsList.setAdapter(new DetailsAdapter(context, mediaDetails));

        final AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        return builder.setTitle(R.string.details).setView(detailsList)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).create();
    }

    /**
     * An adapter for feeding a details list view with the contents of a
     * {@link MediaDetails} instance.
     */
    private static class DetailsAdapter extends BaseAdapter {
        private final Context mContext;
        private final MediaDetails mMediaDetails;
        private final ArrayList<String> mItems;
        private final Locale mDefaultLocale = Locale.getDefault();
        private final DecimalFormat mDecimalFormat = new DecimalFormat(".####");
        private int mWidthIndex = -1;
        private int mHeightIndex = -1;

        public DetailsAdapter(Context context, MediaDetails details) {
            mContext = context;
            mMediaDetails = details;
            mItems = new ArrayList<String>(details.size());
            setDetails(context, details);
        }

        private void setDetails(Context context, MediaDetails details) {
            boolean resolutionIsValid = true;
            String path = null;
            for (Entry<Integer, Object> detail : details) {
                String value;
                switch (detail.getKey()) {
                    case MediaDetails.INDEX_SIZE: {
                        value = Formatter.formatFileSize(
                                context, (Long) detail.getValue());
                        break;
                    }
                    case MediaDetails.INDEX_WHITE_BALANCE: {
                        value = "1".equals(detail.getValue())
                                ? context.getString(R.string.manual)
                                : context.getString(R.string.auto);
                        break;
                    }
                    case MediaDetails.INDEX_FLASH: {
                        MediaDetails.FlashState flash =
                                (MediaDetails.FlashState) detail.getValue();
                        // TODO: camera doesn't fill in the complete values,
                        // show more information when it is fixed.
                        if (flash.isFlashFired()) {
                            value = context.getString(R.string.flash_on);
                        } else {
                            value = context.getString(R.string.flash_off);
                        }
                        break;
                    }
                    case MediaDetails.INDEX_EXPOSURE_TIME: {
                        value = (String) detail.getValue();
                        double time = Double.valueOf(value);
                        if (time < 1.0f) {
                            value = String.format(mDefaultLocale, "%d/%d", 1,
                                    (int) (0.5f + 1 / time));
                        } else {
                            int integer = (int) time;
                            time -= integer;
                            value = String.valueOf(integer) + "''";
                            if (time > 0.0001) {
                                value += String.format(mDefaultLocale, " %d/%d", 1,
                                        (int) (0.5f + 1 / time));
                            }
                        }
                        break;
                    }
                    case MediaDetails.INDEX_WIDTH:
                        mWidthIndex = mItems.size();
                        if (detail.getValue().toString().equalsIgnoreCase("0")) {
                            value = context.getString(R.string.unknown);
                            resolutionIsValid = false;
                        } else {
                            value = toLocalInteger(detail.getValue());
                        }
                        break;
                    case MediaDetails.INDEX_HEIGHT: {
                        mHeightIndex = mItems.size();
                        if (detail.getValue().toString().equalsIgnoreCase("0")) {
                            value = context.getString(R.string.unknown);
                            resolutionIsValid = false;
                        } else {
                            value = toLocalInteger(detail.getValue());
                        }
                        break;
                    }
                    case MediaDetails.INDEX_PATH:
                        // Prepend the new-line as a) paths are usually long, so
                        // the formatting is better and b) an RTL UI will see it
                        // as a separate section and interpret it for what it
                        // is, rather than trying to make it RTL (which messes
                        // up the path).
                        value = "\n" + detail.getValue().toString();
                        path = detail.getValue().toString();
                        break;
                    case MediaDetails.INDEX_ORIENTATION:
                        value = toLocalInteger(detail.getValue());
                        break;
                    case MediaDetails.INDEX_ISO:
                        value = toLocalNumber(Integer.parseInt((String) detail.getValue()));
                        break;
                    case MediaDetails.INDEX_FOCAL_LENGTH:
                        double focalLength = Double.parseDouble(detail.getValue().toString());
                        value = toLocalNumber(focalLength);
                        break;
                    default: {
                        Object valueObj = detail.getValue();
                        // This shouldn't happen, log its key to help us
                        // diagnose the problem.
                        if (valueObj == null) {
                            fail("%s's value is Null",
                                    getDetailsName(context,
                                            detail.getKey()));
                        }
                        value = valueObj.toString();
                    }
                }
                int key = detail.getKey();
                if (details.hasUnit(key)) {
                    value = String.format("%s: %s %s", getDetailsName(
                            context, key), value, context.getString(details.getUnit(key)));
                } else {
                    value = String.format("%s: %s", getDetailsName(
                            context, key), value);
                }
                mItems.add(value);
            }
            if (!resolutionIsValid) {
                resolveResolution(path);
            }
        }

        public void resolveResolution(String path) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap == null)
                return;
            onResolutionAvailable(bitmap.getWidth(), bitmap.getHeight());
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mMediaDetails.getDetail(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = (TextView) LayoutInflater.from(mContext).inflate(
                        R.layout.details, parent, false);
            } else {
                tv = (TextView) convertView;
            }
            tv.setText(mItems.get(position));
            return tv;
        }

        public void onResolutionAvailable(int width, int height) {
            if (width == 0 || height == 0)
                return;
            // Update the resolution with the new width and height
            String widthString = String.format(mDefaultLocale, "%s: %d",
                    getDetailsName(
                            mContext, MediaDetails.INDEX_WIDTH), width);
            String heightString = String.format(mDefaultLocale, "%s: %d",
                    getDetailsName(
                            mContext, MediaDetails.INDEX_HEIGHT), height);
            mItems.set(mWidthIndex, String.valueOf(widthString));
            mItems.set(mHeightIndex, String.valueOf(heightString));
            notifyDataSetChanged();
        }

        /**
         * Converts the given integer (given as String or Integer object) to a
         * localized String version.
         */
        private String toLocalInteger(Object valueObj) {
            if (valueObj instanceof Integer) {
                return toLocalNumber((Integer) valueObj);
            } else {
                String value = valueObj.toString();
                try {
                    value = toLocalNumber(Integer.parseInt(value));
                } catch (NumberFormatException ex) {
                    // Just keep the current "value" if we cannot
                    // parse it as a fallback.
                }
                return value;
            }
        }

        /** Converts the given integer to a localized String version. */
        private String toLocalNumber(int n) {
            return String.format(mDefaultLocale, "%d", n);
        }

        /** Converts the given double to a localized String version. */
        private String toLocalNumber(double n) {
            return mDecimalFormat.format(n);
        }
    }

    public static String getDetailsName(Context context, int key) {
        switch (key) {
            case MediaDetails.INDEX_TITLE:
                return context.getString(R.string.title);
            case MediaDetails.INDEX_DESCRIPTION:
                return context.getString(R.string.description);
            case MediaDetails.INDEX_DATETIME:
                return context.getString(R.string.time);
            case MediaDetails.INDEX_LOCATION:
                return context.getString(R.string.location);
            case MediaDetails.INDEX_PATH:
                return context.getString(R.string.path);
            case MediaDetails.INDEX_WIDTH:
                return context.getString(R.string.width);
            case MediaDetails.INDEX_HEIGHT:
                return context.getString(R.string.height);
            case MediaDetails.INDEX_ORIENTATION:
                return context.getString(R.string.orientation);
            case MediaDetails.INDEX_DURATION:
                return context.getString(R.string.duration);
            case MediaDetails.INDEX_MIMETYPE:
                return context.getString(R.string.mimetype);
            case MediaDetails.INDEX_SIZE:
                return context.getString(R.string.file_size);
            case MediaDetails.INDEX_MAKE:
                return context.getString(R.string.maker);
            case MediaDetails.INDEX_MODEL:
                return context.getString(R.string.model);
            case MediaDetails.INDEX_FLASH:
                return context.getString(R.string.flash);
            case MediaDetails.INDEX_APERTURE:
                return context.getString(R.string.aperture);
            case MediaDetails.INDEX_FOCAL_LENGTH:
                return context.getString(R.string.focal_length);
            case MediaDetails.INDEX_WHITE_BALANCE:
                return context.getString(R.string.white_balance);
            case MediaDetails.INDEX_EXPOSURE_TIME:
                return context.getString(R.string.exposure_time);
            case MediaDetails.INDEX_ISO:
                return context.getString(R.string.iso);
            default:
                return "Unknown key" + key;
        }
    }

    /**
     * Throw an assertion error wit the given message.
     *
     * @param message the message, can contain placeholders.
     * @param args if he message contains placeholders, these values will be
     *            used to fill them.
     */
    private static void fail(String message, Object... args) {
        throw new AssertionError(
                args.length == 0 ? message : String.format(message, args));
    }
}
