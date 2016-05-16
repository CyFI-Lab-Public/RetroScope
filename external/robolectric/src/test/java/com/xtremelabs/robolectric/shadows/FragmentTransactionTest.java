package com.xtremelabs.robolectric.shadows;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.xtremelabs.robolectric.WithTestDefaultsRunner;
import com.xtremelabs.robolectric.tester.android.util.TestFragmentManager;
import com.xtremelabs.robolectric.tester.android.util.TestFragmentTransaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(WithTestDefaultsRunner.class)
public class FragmentTransactionTest {
    private MockTestFragmentManager manager;
    private Fragment fragment;
    private TestFragmentTransaction txn;
    private String tag;
    private int id;

    @Before
    public void setUp() throws Exception {
        manager = new MockTestFragmentManager();
        txn = new TestFragmentTransaction(manager);
        fragment = new TestFragment();
        tag = "tag";
        id = 111;
    }

    @Test
    public void testGetters() throws Exception {
        txn.add(fragment, tag);
        assertSame(fragment, txn.getFragment());
        assertSame(tag, txn.getTag());
        assertEquals(View.NO_ID, txn.getContainerViewId());

        txn.add(id, fragment);
        assertEquals(id, txn.getContainerViewId());
        assertSame(fragment, txn.getFragment());

        txn.add(id, fragment, tag);
        assertEquals(id, txn.getContainerViewId());
        assertSame(fragment, txn.getFragment());
        assertSame(tag, txn.getTag());
        assertFalse(txn.isReplacing());

        txn.replace(id, fragment);
        assertEquals(id, txn.getContainerViewId());
        assertSame(fragment, txn.getFragment());
        assertTrue(txn.isReplacing());

        txn.replace(id, fragment, tag);
        assertEquals(id, txn.getContainerViewId());
        assertSame(fragment, txn.getFragment());
        assertSame(tag, txn.getTag());
        assertTrue(txn.isReplacing());
    }

    @Test
    public void testAddToBackStack() throws Exception {
        assertFalse(txn.isAddedToBackStack());
        FragmentTransaction returnedTransaction = txn.addToBackStack("name");
        assertSame(txn, returnedTransaction);
        assertTrue(txn.isAddedToBackStack());
        assertEquals("name", txn.getBackStackName());
    }

    @Test
    public void addWithId_commit_shouldCallFragmentManager() throws Exception {
        txn.add(111, fragment).commit();

        assertTrue(manager.addFragmentWasCalled);
        assertEquals(manager.addFragmentContainerViewId, 111);
        assertEquals(manager.addFragmentTag, null);
        assertSame(manager.addFragmentFragment, fragment);
        assertFalse(manager.addFragmentReplace);
    }

    @Test
    public void addWithIdAndTag_commit_shouldCallFragmentManager() throws Exception {
        txn.add(111, fragment, "tag1").commit();

        assertTrue(manager.addFragmentWasCalled);
        assertEquals(manager.addFragmentContainerViewId, 111);
        assertEquals(manager.addFragmentTag, "tag1");
        assertSame(manager.addFragmentFragment, fragment);
        assertFalse(manager.addFragmentReplace);
    }

    @Test
    public void addWithTag_commit_shouldCallFragmentManager() throws Exception {
        txn.add(fragment, "tag1").commit();

        assertTrue(manager.addFragmentWasCalled);
        assertEquals(manager.addFragmentContainerViewId, View.NO_ID);
        assertEquals(manager.addFragmentTag, "tag1");
        assertSame(manager.addFragmentFragment, fragment);
        assertFalse(manager.addFragmentReplace);
    }

    @Test
    public void replaceWithId_commit_shouldCallFragmentManager() throws Exception {
        txn.replace(111, fragment).commit();

        assertTrue(manager.addFragmentWasCalled);
        assertEquals(manager.addFragmentContainerViewId, 111);
        assertEquals(manager.addFragmentTag, null);
        assertSame(manager.addFragmentFragment, fragment);
        assertTrue(manager.addFragmentReplace);
    }

    @Test
    public void replaceWithIdAndTag_commit_shouldCallFragmentManager() throws Exception {
        txn.replace(111, fragment, "tag1").commit();

        assertTrue(manager.addFragmentWasCalled);
        assertEquals(manager.addFragmentContainerViewId, 111);
        assertEquals(manager.addFragmentTag, "tag1");
        assertSame(manager.addFragmentFragment, fragment);
        assertTrue(manager.addFragmentReplace);
    }

    @Test
    public void startActivity_shouldNotDelegateToParentActivity() throws Exception {
        // because for some reason that's not what Android does in real life
        StartActivityTrackingActivity trackingActivity = new StartActivityTrackingActivity();
        shadowOf(fragment).setActivity(trackingActivity);
        fragment.startActivity(null);
        assertFalse(trackingActivity.startActivityWasCalled);
    }

    @Test
    public void commit_shouldNotActLikeCommitAllowingStateLoss() throws Exception {
        txn.add(fragment, "tag1").commit();
        assertFalse(txn.isCommittedAllowingStateLoss());
    }

    @Test
    public void commitAllowingStateLoss_shouldCommitAndSetAFlag() throws Exception {
        txn.add(fragment, "tag1").commitAllowingStateLoss();

        assertTrue(manager.addFragmentWasCalled);
        assertEquals(manager.addFragmentContainerViewId, View.NO_ID);
        assertEquals(manager.addFragmentTag, "tag1");
        assertSame(manager.addFragmentFragment, fragment);

        assertTrue(txn.isCommittedAllowingStateLoss());
    }

    @Test
    public void attach_shouldCauseFragmentToBecomeAttached() throws Exception {
        shadowOf(fragment).setAttached(false);
        txn.attach(fragment).commit();
        assertTrue(shadowOf(fragment).isAttached());
    }

    @Test
    public void hide_shouldCauseFragmentToBecomeHidden() {
        shadowOf(fragment).setHidden(false);
        txn.hide(fragment).commit();
        assertTrue(shadowOf(fragment).isHidden());
    }

    @Test
    public void show_shouldCauseFragmentToBecomeNotHidden() {
        shadowOf(fragment).setHidden(true);
        txn.show(fragment).commit();
        assertFalse(shadowOf(fragment).isHidden());
    }

    @Test
    public void hide_shouldCallOnHiddenChangedOnFragment() {
        final String key = "onHiddenChanged";
        final Map<String, Boolean> calls = new HashMap<String, Boolean>();
        Fragment fragment = new TestFragment() {
          @Override
          public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
            calls.put(key, hidden);
          }
        };
        shadowOf(fragment).setHidden(false);
        txn.hide(fragment).commit();
        assertEquals(true, calls.get(key));
    }

    @Test
    public void show_shouldCallOnHiddenChangedOnFragment() {
        final String key = "onHiddenChanged";
        final Map<String, Boolean> calls = new HashMap<String, Boolean>();
        Fragment fragment = new TestFragment() {
          @Override
          public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
            calls.put(key, hidden);
          }
        };
        shadowOf(fragment).setHidden(true);
        txn.show(fragment).commit();
        assertEquals(false, calls.get(key));
    }

    @Test
    public void hide_shouldNotCallOnHiddenChangedOnFragmentIfAlreadyHidden() {
        final String key = "onHiddenChanged";
        final Map<String, Boolean> calls = new HashMap<String, Boolean>();
        Fragment fragment = new TestFragment() {
          @Override
          public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
            calls.put(key, hidden);
          }
        };
        shadowOf(fragment).setHidden(true);
        txn.hide(fragment).commit();
        assertFalse(calls.containsKey(key));
    }

    @Test
    public void show_shouldNotCallOnHiddenChangedOnFragmentIfAlreadyNotHidden() {
        final String key = "onHiddenChanged";
        final Map<String, Boolean> calls = new HashMap<String, Boolean>();
        Fragment fragment = new TestFragment() {
          @Override
          public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
            calls.put(key, hidden);
          }
        };
        shadowOf(fragment).setHidden(false);
        txn.show(fragment).commit();
        assertFalse(calls.containsKey(key));
    }

    @Test
    public void showHide_shouldHideFragment() {
        shadowOf(fragment).setHidden(false);
        txn.show(fragment).hide(fragment).commit();
        assertTrue(shadowOf(fragment).isHidden());
    }

    @Test
    public void hideShow_shouldShowFragment() {
        shadowOf(fragment).setHidden(false);
        txn.hide(fragment).show(fragment).commit();
        assertFalse(shadowOf(fragment).isHidden());
    }

    private static class MockTestFragmentManager extends TestFragmentManager {
        private boolean addFragmentWasCalled;
        private int addFragmentContainerViewId;
        private String addFragmentTag;
        private Fragment addFragmentFragment;
        private boolean addFragmentReplace;

        public MockTestFragmentManager() {
            super(new FragmentActivity());
        }

        @Override
        public void addFragment(int containerViewId, String tag, Fragment fragment, boolean replace) {
            addFragmentWasCalled = true;
            addFragmentContainerViewId = containerViewId;
            addFragmentTag = tag;
            addFragmentFragment = fragment;
            addFragmentReplace = replace;
        }
    }

    private static class StartActivityTrackingActivity extends FragmentActivity {
        boolean startActivityWasCalled;

        @Override
        public void startActivity(Intent intent) {
            super.startActivity(intent);
            startActivityWasCalled = true;
        }
    }
}
