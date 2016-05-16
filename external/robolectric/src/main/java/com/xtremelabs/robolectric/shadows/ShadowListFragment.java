package com.xtremelabs.robolectric.shadows;

import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Shadow of {@code ListFragment}
 */
@Implements(ListFragment.class)
public class ShadowListFragment extends ShadowFragment {

    private ListAdapter listAdapter;

    @Implementation
    public ListAdapter getListAdapter() {
        return listAdapter;
    }

    @Implementation
    public void setListAdapter(ListAdapter listAdapter) {
        this.listAdapter = listAdapter;
    }
}
