package com.xtremelabs.robolectric.shadows;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import com.xtremelabs.robolectric.WithTestDefaultsRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(WithTestDefaultsRunner.class)
public class ProcessTest {

    @Test
    public void testMyPidIsJvmProcessId() {
        int pid = android.os.Process.myPid();

        assertThat(pid, not(equalTo(0)));
    }

    @Test
    public void testSetPid() {
        ShadowProcess.setPid(47);

        assertThat(android.os.Process.myPid(), equalTo(47));
    }
}
