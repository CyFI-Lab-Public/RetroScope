package com.xtremelabs.robolectric.shadows;

import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.WithTestDefaultsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(WithTestDefaultsRunner.class)
public class ListFragmentTest {

    @Test
    public void shouldSupportSettingAndGettingListAdapter(){
        ListFragment listFragment = new ListFragment();
        Robolectric.shadowOf(listFragment).setView(new ListView(null));
        ListAdapter adapter = new CountingAdapter(5);
        listFragment.setListAdapter(adapter);

        assertThat(listFragment.getListAdapter(), is(notNullValue()));
    }
}
