package com.xtremelabs.robolectric.shadows;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import android.os.Binder;

import com.xtremelabs.robolectric.WithTestDefaultsRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(WithTestDefaultsRunner.class)
public class BinderTest {

    @Test
    public void testSetCallingPid() {
        ShadowBinder.setCallingPid(47);

        assertThat(Binder.getCallingPid(), equalTo(47));
    }

    @Test
    public void testCallingProcessIsJvmProcessId() {
        int pid = Binder.getCallingPid();

        assertThat(pid, not(equalTo(0)));
    }
}
