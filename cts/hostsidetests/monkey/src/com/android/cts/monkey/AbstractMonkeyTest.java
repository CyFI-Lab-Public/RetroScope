package com.android.cts.monkey;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;

abstract class AbstractMonkeyTest extends DeviceTestCase implements IBuildReceiver {

    static final String[] PKGS = {"com.android.cts.monkey", "com.android.cts.monkey2"};
    static final String[] APKS = {"CtsMonkeyApp.apk", "CtsMonkeyApp2.apk"};

    /** 
     * Base monkey command with flags to avoid side effects like airplane mode.
     */
    static final String MONKEY_CMD = "monkey --pct-touch 0 --pct-motion 0 --pct-majornav 0 --pct-syskeys 0 --pct-anyevent 0 --pct-rotation 0";

    CtsBuildHelper mBuild;
    ITestDevice mDevice;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = getDevice();
        for (int i = 0; i < PKGS.length; i++) {
            mDevice.uninstallPackage(PKGS[i]);
            File app = mBuild.getTestApp(APKS[i]);
            mDevice.installPackage(app, false);
        }
        clearLogCat();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        for (int i = 0; i < PKGS.length; i++) {
            mDevice.uninstallPackage(PKGS[i]);
        }
    }

    private void clearLogCat() throws DeviceNotAvailableException {
        mDevice.executeAdbCommand("logcat", "-c");
    }
}
