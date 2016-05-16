package com.xtremelabs.robolectric.shadows;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.xtremelabs.robolectric.R;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.util.Transcript;

public class TestFragment extends Fragment {
    public static final int FRAGMENT_VIEW_ID = 2323;
    public boolean onAttachWasCalled;
    public boolean onCreateWasCalled;
    public boolean onCreateViewWasCalled;
    public boolean onActivityCreatedWasCalled;
    public LayoutInflater onCreateViewInflater;
    public View onCreateViewReturnValue;
    public boolean onStartWasCalled;
    public boolean onResumeWasCalled;
    public boolean onSaveInstanceStateWasCalled;
    public boolean onPauseWasCalled;
    public Activity onAttachActivity;
    public Bundle onActivityCreated_savedInstanceState;
    public Bundle onCreateSavedInstanceState;

    Transcript transcript = new Transcript();

    @Override
    public void onAttach(Activity activity) {
        onAttachWasCalled = true;
        onAttachActivity = activity;
        transcript.add("onAttach");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        onCreateWasCalled = true;
        transcript.add("onCreate");
        onCreateSavedInstanceState = savedInstanceState;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        onCreateViewWasCalled = true;
        onCreateViewInflater = inflater;

        Context context = Robolectric.application.getApplicationContext();
        Button button = new Button(context);
        button.setId(R.id.button);
        FrameLayout view = new FrameLayout(context);
        view.addView(button);

        onCreateViewReturnValue = view;
        onCreateViewReturnValue.setId(FRAGMENT_VIEW_ID);
        transcript.add("onCreateView");
        return onCreateViewReturnValue;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        transcript.add("onViewCreated");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        onActivityCreatedWasCalled = true;
        onActivityCreated_savedInstanceState = savedInstanceState;
        transcript.add("onActivityCreated");
    }

    @Override
    public void onStart() {
        onStartWasCalled = true;
        transcript.add("onStart");
    }

    @Override
    public void onResume() {
        onResumeWasCalled = true;
        transcript.add("onResume");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        onSaveInstanceStateWasCalled = true;
    }

    @Override
    public void onPause() {
        onPauseWasCalled = true;
    }
}
