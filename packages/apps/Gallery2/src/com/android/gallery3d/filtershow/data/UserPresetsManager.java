package com.android.gallery3d.filtershow.data;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

import java.util.ArrayList;

public class UserPresetsManager implements Handler.Callback {

    private static final String LOGTAG = "UserPresetsManager";

    private FilterShowActivity mActivity;
    private HandlerThread mHandlerThread = null;
    private Handler mProcessingHandler = null;
    private FilterStackSource mUserPresets;

    private static final int LOAD = 1;
    private static final int LOAD_RESULT = 2;
    private static final int SAVE = 3;
    private static final int DELETE = 4;
    private static final int UPDATE = 5;

    private ArrayList<FilterUserPresetRepresentation> mRepresentations;

    private final Handler mResultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_RESULT:
                    resultLoad(msg);
                    break;
            }
        }
    };

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case LOAD:
                processLoad();
                return true;
            case SAVE:
                processSave(msg);
                return true;
            case DELETE:
                processDelete(msg);
                return true;
            case UPDATE:
                processUpdate(msg);
                return true;
        }
        return false;
    }

    public UserPresetsManager(FilterShowActivity context) {
        mActivity = context;
        mHandlerThread = new HandlerThread(LOGTAG,
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        mProcessingHandler = new Handler(mHandlerThread.getLooper(), this);
        mUserPresets = new FilterStackSource(mActivity);
        mUserPresets.open();
    }

    public ArrayList<FilterUserPresetRepresentation> getRepresentations() {
        return mRepresentations;
    }

    public void load() {
        Message msg = mProcessingHandler.obtainMessage(LOAD);
        mProcessingHandler.sendMessage(msg);
    }

    public void close() {
        mUserPresets.close();
        mHandlerThread.quit();
    }

    static class SaveOperation {
        String json;
        String name;
    }

    public void save(ImagePreset preset, String name) {
        Message msg = mProcessingHandler.obtainMessage(SAVE);
        SaveOperation op = new SaveOperation();
        op.json = preset.getJsonString(ImagePreset.JASON_SAVED);
        op.name = name;
        msg.obj = op;
        mProcessingHandler.sendMessage(msg);
    }

    public void delete(int id) {
        Message msg = mProcessingHandler.obtainMessage(DELETE);
        msg.arg1 = id;
        mProcessingHandler.sendMessage(msg);
    }

    static class UpdateOperation {
        int id;
        String name;
    }

    public void update(FilterUserPresetRepresentation representation) {
        Message msg = mProcessingHandler.obtainMessage(UPDATE);
        UpdateOperation op = new UpdateOperation();
        op.id = representation.getId();
        op.name = representation.getName();
        msg.obj = op;
        mProcessingHandler.sendMessage(msg);
    }

    private void processLoad() {
        ArrayList<FilterUserPresetRepresentation> list = mUserPresets.getAllUserPresets();
        Message msg = mResultHandler.obtainMessage(LOAD_RESULT);
        msg.obj = list;
        mResultHandler.sendMessage(msg);
    }

    private void resultLoad(Message msg) {
        mRepresentations =
                (ArrayList<FilterUserPresetRepresentation>) msg.obj;
        mActivity.updateUserPresetsFromManager();
    }

    private void processSave(Message msg) {
        SaveOperation op = (SaveOperation) msg.obj;
        mUserPresets.insertStack(op.name, op.json.getBytes());
        processLoad();
    }

    private void processDelete(Message msg) {
        int id = msg.arg1;
        mUserPresets.removeStack(id);
        processLoad();
    }

    private void processUpdate(Message msg) {
        UpdateOperation op = (UpdateOperation) msg.obj;
        mUserPresets.updateStackName(op.id, op.name);
        processLoad();
    }

}
