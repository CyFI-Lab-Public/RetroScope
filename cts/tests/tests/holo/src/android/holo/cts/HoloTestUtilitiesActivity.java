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

package android.holo.cts;

import com.android.cts.holo.R;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * {@link ListActivity} with helpful utilities like viewing the layout tests
 * and generating the reference assets that can be packaged back into the
 * test APK.
 */
public class HoloTestUtilitiesActivity extends ListActivity {

    private static final int TASK_VIEW_DISPLAY_INFO = 0;
    private static final int TASK_VIEW_TESTS = 1;
    private static final int TASK_GENERATE_ONE_BITMAP = 2;
    private static final int TASK_GENERATE_ALL_BITMAPS = 3;
    private static final int TASK_CLEAR_REFERENCE_BITMAPS = 4;
    private static final int TASK_CLEAR_FAILED_BITMAPS = 5;
    private static final int TASK_CLEAR_DIFF_BITMAPS = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayAdapter<Task> taskAdapter = new ArrayAdapter<Task>(this,
                android.R.layout.simple_list_item_1);
        taskAdapter.add(new Task(R.string.task_view_display_info, TASK_VIEW_DISPLAY_INFO));
        taskAdapter.add(new Task(R.string.task_view_tests, TASK_VIEW_TESTS));
        taskAdapter.add(new Task(R.string.task_generate_one_bitmap, TASK_GENERATE_ONE_BITMAP));
        taskAdapter.add(new Task(R.string.task_generate_all_bitmaps, TASK_GENERATE_ALL_BITMAPS));
        taskAdapter.add(new Task(R.string.task_clear_reference_bitmaps,
                TASK_CLEAR_REFERENCE_BITMAPS));
        taskAdapter.add(new Task(R.string.task_clear_failure_bitmaps,
                TASK_CLEAR_FAILED_BITMAPS));
        taskAdapter.add(new Task(R.string.task_clear_diff_bitmaps,
                TASK_CLEAR_DIFF_BITMAPS));
        setListAdapter(taskAdapter);
    }

    class Task {
        private final int mTextId;
        private final int mTaskId;

        private Task(int textId, int taskId) {
            mTextId = textId;
            mTaskId = taskId;
        }

        @Override
        public String toString() {
            return getString(mTextId);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayAdapter<Task> getListAdapter() {
        return (ArrayAdapter<Task>) super.getListAdapter();
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Task task = getListAdapter().getItem(position);
        switch (task.mTaskId) {
            case TASK_VIEW_DISPLAY_INFO:
                viewDisplayInfo();
                break;

            case TASK_VIEW_TESTS:
                viewTests();
                break;

            case TASK_GENERATE_ONE_BITMAP:
                generateSingleTestBitmaps();
                break;

            case TASK_GENERATE_ALL_BITMAPS:
                generateAllBitmaps();
                break;

            case TASK_CLEAR_REFERENCE_BITMAPS:
                clearBitmaps(BitmapAssets.TYPE_REFERENCE);
                break;

            case TASK_CLEAR_FAILED_BITMAPS:
                clearBitmaps(BitmapAssets.TYPE_FAILED);
                break;

            case TASK_CLEAR_DIFF_BITMAPS:
                clearBitmaps(BitmapAssets.TYPE_DIFF);
                break;

            default:
                throw new IllegalArgumentException("Bad task id: " + task.mTaskId);
        }
    }

    private void viewDisplayInfo() {
        Intent intent = new Intent(this, DisplayInfoActivity.class);
        startActivity(intent);
    }

    private void viewTests() {
        Intent intent = new Intent(this, ThemePickerActivity.class);
        intent.putExtra(ThemePickerActivity.EXTRA_TASK, ThemeTestActivity.TASK_VIEW_LAYOUTS);
        startActivity(intent);
    }

    private void generateSingleTestBitmaps() {
        Intent intent = new Intent(this, LayoutPickerActivity.class);
        intent.putExtra(ThemePickerActivity.EXTRA_TASK, ThemeTestActivity.TASK_GENERATE_BITMAPS);
        startActivity(intent);
    }

    private void generateAllBitmaps() {
        Intent intent = new Intent(this, ThemeTestActivity.class);
        intent.putExtra(ThemeTestActivity.EXTRA_TASK, ThemeTestActivity.TASK_GENERATE_BITMAPS);
        intent.putExtra(ThemeTestActivity.EXTRA_LAYOUT_ADAPTER_MODE, LayoutAdapter.MODE_TESTING);
        startActivity(intent);
    }

    private void clearBitmaps(int type) {
        Intent intent = new Intent(this, BitmapDeletionActivity.class);
        intent.putExtra(BitmapDeletionActivity.EXTRA_BITMAP_TYPE, type);
        startActivity(intent);
    }
}
