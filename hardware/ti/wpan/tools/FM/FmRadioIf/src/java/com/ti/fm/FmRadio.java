/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.fm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.content.ServiceConnection;
import android.util.Log;

/**
 * The Android FmRadio API is not finalized, and *will* change. Use at your own
 * risk. Public API for controlling the FmRadio Service. FmRadio is a proxy
 * object for controlling the Fm Reception/Transmission Service via IPC.
 * Creating a FmRadio object will create a binding with the FMRX service. Users
 * of this object should call close() when they are finished with the FmRadio,
 * so that this proxy object can unbind from the service.
 *
 * @hide
 */
public class FmRadio {

    private static final String TAG = "FmRadio";

    private static final boolean DBG = true;

    private IFmRadio mService;

    private Context mContext;

    private ServiceListener mServiceListener;

    public static final int STATE_ENABLED = 0;

    public static final int STATE_DISABLED = 1;

    public static final int STATE_ENABLING = 2;

    public static final int STATE_DISABLING = 3;

    public static final int STATE_PAUSE = 4;

    public static final int STATE_RESUME = 5;

    public static final int STATE_DEFAULT = 6;

    private static FmRadio INSTANCE;

    /** Used when obtaining a reference to the singleton instance. */
    private static Object INSTANCE_LOCK = new Object();

    private boolean mInitialized = false;

    // Constructor
    private FmRadio() {

    }

    private ServiceConnection mConnection = new ServiceConnection() {
       public void onServiceConnected(ComponentName className, IBinder service) {
          // This is called when the connection with the service has been
          // established, giving us the service object we can use to
          // interact with the service. We are communicating with our
          // service through an IDL interface, so get a client-side
          // representation of that from the raw service object.
          Log.i(TAG, "Service connected");
          mService = IFmRadio.Stub.asInterface(service);
          if (mServiceListener != null) {
             Log.i(TAG, "Sending callback");
             mServiceListener.onServiceConnected();
          } else {
             Log.e(TAG, "mService is NULL");
          }
       }

       public void onServiceDisconnected(ComponentName className) {
          // This is called when the connection with the service has been
          // unexpectedly disconnected -- that is, its process crashed.
          Log.i(TAG, "Service disconnected");
          mService = null;
          if (mServiceListener != null) {
             mServiceListener.onServiceDisconnected();
          }
       }
    };

    public FmRadio(Context context, ServiceListener listener) {
       // This will be around as long as this process is
       mContext = context.getApplicationContext();
       mServiceListener = listener;
       Log.i(TAG, "FmRadio:Creating a FmRadio proxy object: " + mConnection);
       mContext.bindService(new Intent("com.ti.server.FmService"), mConnection,
             Context.BIND_AUTO_CREATE);
    }

    /**
    * An interface for notifying FmRadio IPC clients when they have been
    * connected to the FMRX service.
    */
    public interface ServiceListener {
       /**
        * Called to notify the client when this proxy object has been connected
        * to the FMRX service. Clients must wait for this callback before
        * making IPC calls on the FMRX service.
        */
       public void onServiceConnected();

       /**
        * Called to notify the client that this proxy object has been
        * disconnected from the FMRX service. Clients must not make IPC calls
        * on the FMRX service after this callback. This callback will currently
        * only occur if the application hosting the BluetoothAg service, but
        * may be called more often in future.
        */
       public void onServiceDisconnected();
    }

    protected void finalize() throws Throwable {
       if (DBG)
          Log.d(TAG, "FmRadio:finalize");
       try {
          close();
       } finally {
          super.finalize();
       }
    }

    /**
    * Close the connection to the backing service. Other public functions of
    * BluetoothAg will return default error results once close() has been
    * called. Multiple invocations of close() are ok.
    */
    public synchronized void close() {
       Log.i(TAG, "FmRadio:close");

       mContext.unbindService(mConnection);
    }

    /**
    * Returns true if the FM RX is enabled. Returns false if not enabled, or if
    * this proxy object if not currently connected to the FmRadio service.
    */
    public boolean rxIsEnabled() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxIsEnabled");
       if (mService != null) {
          try {
             return mService.rxIsEnabled();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    /**
    * Returns the current FM RX state
    */

    public int rxGetFMState() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetFMState");
       if (mService != null) {
          try {
             return mService.rxGetFMState();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return -1;
    }

    /**
    * Returns true if the FM RX is paused. Returns false if not paused, or if
    * this proxy object if not currently connected to the FmRadio service.
    */

    public boolean rxIsFMPaused() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxIsFMPaused");
       if (mService != null) {
          try {
             return mService.rxIsFMPaused();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    /**
    * Returns true if the FM RX is enabled. Returns false if not enabled, or if
    * this proxy object if not currently connected to the FmRadio service.
    */
    public boolean rxEnable() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxEnable");
       if (mService != null) {
          try {
             return mService.rxEnable();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    /**
    * Returns true if the EM RX is disabled. Returns false if not enabled, or
    * if this proxy object if not currently connected to the FmRadio service.
    */
    public boolean rxDisable() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxDisable");
       if (mService != null) {
          try {
             return mService.rxDisable();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean resumeFm() {
       if (DBG)
          Log.d(TAG, "FmRadio:resumeFm");
       if (mService != null) {
          try {
             return mService.resumeFm();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetBand(int band) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetBand");
       if (mService != null) {
          try {
             return mService.rxSetBand(band);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetBand_nb(int band) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetBand_nb");
       if (mService != null) {
          try {
             return mService.rxSetBand_nb(band);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetBand() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetBand");
       if (mService != null) {
          try {
             return mService.rxGetBand();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetBand_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetBand_nb");
       if (mService != null) {
          try {
             return mService.rxGetBand_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetMonoStereoMode(int mode) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetMonoStereoMode");
       if (mService != null) {
          try {
             return mService.rxSetMonoStereoMode(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetMonoStereoMode_nb(int mode) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetMonoStereoMode_nb");
       if (mService != null) {
          try {
             return mService.rxSetMonoStereoMode_nb(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxIsValidChannel() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxIsValidChannel");
       if (mService != null) {
          try {
             return mService.rxIsValidChannel();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxCompleteScan_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxCompleteScan_nb");
       if (mService != null) {
          try {
             return mService.rxCompleteScan_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxStopCompleteScan_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxStopCompleteScan_nb");
       if (mService != null) {
          try {
             return mService.rxStopCompleteScan_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxStopCompleteScan() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxStopCompleteScan");
       if (mService != null) {
          try {
             return mService.rxStopCompleteScan();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public double rxGetFwVersion() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetFwVersion");
       if (mService != null) {
          try {
             return mService.rxGetFwVersion();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public int rxGetCompleteScanProgress() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetCompleteScanProgress");
       if (mService != null) {
          try {
             return mService.rxGetCompleteScanProgress();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetCompleteScanProgress_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetCompleteScanProgress_nb");
       if (mService != null) {
          try {
             return mService.rxGetCompleteScanProgress_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetMonoStereoMode() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetMonoStereoMode");
       if (mService != null) {
          try {
             return mService.rxGetMonoStereoMode();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetMonoStereoMode_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetMonoStereoMode_nb");
       if (mService != null) {
          try {
             return mService.rxGetMonoStereoMode_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetMuteMode(int muteMode) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetMuteMode");
       if (mService != null) {
          try {
             return mService.rxSetMuteMode(muteMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetMuteMode_nb(int muteMode) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetMuteMode_nb");
       if (mService != null) {
          try {
             return mService.rxSetMuteMode_nb(muteMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetMuteMode() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetMuteMode");
       if (mService != null) {
          try {
             return mService.rxGetMuteMode();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetMuteMode_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetMuteMode_nb");
       if (mService != null) {
          try {
             return mService.rxGetMuteMode_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRfDependentMuteMode(int rfMuteMode) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRfDependentMuteMode");
       if (mService != null) {
          try {
             return mService.rxSetRfDependentMuteMode(rfMuteMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRfDependentMuteMode_nb(int rfMuteMode) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRfDependentMuteMode_nb");
       if (mService != null) {
          try {
             return mService.rxSetRfDependentMuteMode_nb(rfMuteMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetRfDependentMuteMode() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRfDependentMuteMode");
       if (mService != null) {
          try {
             return mService.rxGetRfDependentMuteMode();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetRfDependentMuteMode_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRfDependentMuteMode_nb");
       if (mService != null) {
          try {
             return mService.rxGetRfDependentMuteMode_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRssiThreshold(int threshhold) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRssiThreshold");
       if (mService != null) {
          try {
             return mService.rxSetRssiThreshold(threshhold);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRssiThreshold_nb(int threshhold) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRssiThreshold_nb");
       if (mService != null) {
          try {
             return mService.rxSetRssiThreshold_nb(threshhold);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetRssiThreshold() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRssiThreshold");
       if (mService != null) {
          try {
             return mService.rxGetRssiThreshold();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetRssiThreshold_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRssiThreshold_nb");
       if (mService != null) {
          try {
             return mService.rxGetRssiThreshold_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetDeEmphasisFilter(int filter) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetDeEmphasisFilter");
       if (mService != null) {
          try {
             return mService.rxSetDeEmphasisFilter(filter);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetDeEmphasisFilter_nb(int filter) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetDeEmphasisFilter_nb");
       if (mService != null) {
          try {
             return mService.rxSetDeEmphasisFilter_nb(filter);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetDeEmphasisFilter() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetDeEmphasisFilter");
       if (mService != null) {
          try {
             return mService.rxGetDeEmphasisFilter();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetDeEmphasisFilter_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetDeEmphasisFilter_nb");
       if (mService != null) {
          try {
             return mService.rxGetDeEmphasisFilter_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetVolume(int volume) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetVolume");
       if (mService != null) {
          try {
             return mService.rxSetVolume(volume);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetChannelSpacing(int channelSpace) {

       if (DBG)
          Log.d(TAG, "FmRadio:rxSetChannelSpacing");
       if (mService != null) {
          try {
             return mService.rxSetChannelSpacing(channelSpace);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetChannelSpacing_nb(int channelSpace) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetChannelSpacing_nb");
       if (mService != null) {
          try {
             return mService.rxSetChannelSpacing_nb(channelSpace);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetVolume() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetVolume");
       if (mService != null) {
          try {
             return mService.rxGetVolume();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetVolume_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetVolume_nb");
       if (mService != null) {
          try {
             return mService.rxGetVolume_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetChannelSpacing() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetChannelSpacing");
       if (mService != null) {
          try {
             return mService.rxGetChannelSpacing();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetChannelSpacing_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetChannelSpacing");
       if (mService != null) {
          try {
             return mService.rxGetChannelSpacing_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxTune_nb(int freq) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxTune_nb");
       if (mService != null) {
          try {
             return mService.rxTune_nb(freq);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetTunedFrequency() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetTunedFrequency");
       if (mService != null) {
          try {
             return mService.rxGetTunedFrequency();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetTunedFrequency_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetTunedFrequency_nb");
       if (mService != null) {
          try {
             return mService.rxGetTunedFrequency_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSeek_nb(int direction) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSeek_nb");
       if (mService != null) {
          try {
             return mService.rxSeek_nb(direction);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxStopSeek() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxStopSeek");
       if (mService != null) {
          try {
             return mService.rxStopSeek();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxStopSeek_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxStopSeek_nb");
       if (mService != null) {
          try {
             return mService.rxStopSeek_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetRdsSystem() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRdsSystem");
       if (mService != null) {
          try {
             return mService.rxGetRdsSystem();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetRdsSystem_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRdsSystem_nb");
       if (mService != null) {
          try {
             return mService.rxGetRdsSystem_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetRssi() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRssi");
       if (mService != null) {
          try {
             return mService.rxGetRssi();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetRssi_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRssi_nb");
       if (mService != null) {
          try {
             return mService.rxGetRssi_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRdsSystem(int system) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRdsSystem");
       if (mService != null) {
          try {
             return mService.rxSetRdsSystem(system);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRdsSystem_nb(int system) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRdsSystem_nb");
       if (mService != null) {
          try {
             return mService.rxSetRdsSystem_nb(system);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxEnableRds() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxEnableRds");
       if (mService != null) {
          try {
             return mService.rxEnableRds();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxDisableRds() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxDisableRds");
       if (mService != null) {
          try {
             return mService.rxDisableRds();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxEnableRds_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxEnableRds_nb");
       if (mService != null) {
          try {
             return mService.rxEnableRds_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxDisableRds_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxDisableRds_nb");
       if (mService != null) {
          try {
             return mService.rxDisableRds_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRdsGroupMask(int mask) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRdsGroupMask");
       if (mService != null) {
          try {
             return mService.rxSetRdsGroupMask(mask);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRdsGroupMask_nb(int mask) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRdsGroupMask_nb");
       if (mService != null) {
          try {
             return mService.rxSetRdsGroupMask_nb(mask);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public long rxGetRdsGroupMask() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRdsGroupMask");
       if (mService != null) {
          try {
             return mService.rxGetRdsGroupMask();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetRdsGroupMask_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRdsGroupMask_nb");
       if (mService != null) {
          try {
             return mService.rxGetRdsGroupMask_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRdsAfSwitchMode(int mode) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRdsAfSwitchMode");
       if (mService != null) {
          try {
             return mService.rxSetRdsAfSwitchMode(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxSetRdsAfSwitchMode_nb(int mode) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxSetRdsAfSwitchMode_nb");
       if (mService != null) {
          try {
             return mService.rxSetRdsAfSwitchMode_nb(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int rxGetRdsAfSwitchMode() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRdsAfSwitchMode");
       if (mService != null) {
          try {
             return mService.rxGetRdsAfSwitchMode();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean rxGetRdsAfSwitchMode_nb() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxGetRdsAfSwitchMode_nb");
       if (mService != null) {
          try {
             return mService.rxGetRdsAfSwitchMode_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxChangeAudioTarget(int mask, int digitalConfig) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxChangeAudioTarget");
       if (mService != null) {
          try {
             return mService.rxChangeAudioTarget(mask, digitalConfig);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxChangeDigitalTargetConfiguration(int digitalConfig) {
       if (DBG)
          Log.d(TAG, "FmRadio:rxChangeDigitalTargetConfiguration");
       if (mService != null) {
          try {
             return mService.rxChangeDigitalTargetConfiguration(digitalConfig);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxEnableAudioRouting() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxEnableAudioRouting");
       if (mService != null) {
          try {
             return mService.rxEnableAudioRouting();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean rxDisableAudioRouting() {
       if (DBG)
          Log.d(TAG, "FmRadio:rxDisableAudioRouting");
       if (mService != null) {
          try {
             return mService.rxDisableAudioRouting();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    /************** Fm TX *****************/
    public boolean txEnable() {
       if (DBG)
          Log.d(TAG, "FmRadio:txEnable");
       if (mService != null) {
          try {
             return mService.txEnable();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    /**
    * Returns true if the EM RX is disabled. Returns false if not enabled, or
    * if this proxy object if not currently connected to the FmRadio service.
    */
    public boolean txDisable() {
       if (DBG)
          Log.d(TAG, "FmRadio:txDisable");
       if (mService != null) {
          try {
             return mService.txDisable();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txStartTransmission() {
       if (DBG)
          Log.d(TAG, "FmRadio:txStartTransmission");
       if (mService != null) {
          try {
             return mService.txStartTransmission();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txStopTransmission() {
       if (DBG)
          Log.d(TAG, "FmRadio:txStopTransmission");
       if (mService != null) {
          try {
             return mService.txStopTransmission();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txTune(long freq) {
       if (DBG)
          Log.d(TAG, "FmRadio:txTune");
       if (mService != null) {
          try {
             return mService.txTune(freq);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetPowerLevel(int powerLevel) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetPowerLevel");
       if (mService != null) {
          try {
             return mService.txSetPowerLevel(powerLevel);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txEnableRds() {
       if (DBG)
          Log.d(TAG, "FmRadio:txEnableRds");
       if (mService != null) {
          try {
             return mService.txEnableRds();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txDisableRds() {
       if (DBG)
          Log.d(TAG, "FmRadio:txDisableRds");
       if (mService != null) {
          try {
             return mService.txDisableRds();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsTransmissionMode(int mode) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsTransmissionMode");
       if (mService != null) {
          try {
             return mService.txSetRdsTransmissionMode(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsTextPsMsg(String psString) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsTextPsMsg");
       if (mService != null) {
          try {
             return mService.txSetRdsTextPsMsg(psString);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txWriteRdsRawData(String rawData) {
       if (DBG)
          Log.d(TAG, "FmRadio:txWriteRdsRawData");
       if (mService != null) {
          try {
             return mService.txWriteRdsRawData(rawData);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetMonoStereoMode(int mode) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetMonoStereoMode");
       if (mService != null) {
          try {
             return mService.txSetMonoStereoMode(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetPreEmphasisFilter(int preEmpFilter) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetPreEmphasisFilter");
       if (mService != null) {
          try {
             return mService.txSetPreEmphasisFilter(preEmpFilter);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetMuteMode(int muteMode) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetMuteMode");
       if (mService != null) {
          try {
             return mService.txSetMuteMode(muteMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsAfCode(int afCode) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsAfCode");
       if (mService != null) {
          try {
             return mService.txSetRdsAfCode(afCode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsPiCode(int piCode) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsPiCode");
       if (mService != null) {
          try {
             return mService.txSetRdsPiCode(piCode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsPtyCode(int ptyCode) {
       Log.d(TAG, "FmRadio:txSetRdsPtyCode");
       if (mService != null) {
          try {
             return mService.txSetRdsPtyCode(ptyCode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsTextRepertoire(int repertoire) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsTextRepertoire");
       if (mService != null) {
          try {
             return mService.txSetRdsTextRepertoire(repertoire);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsPsDisplayMode(int dispalyMode) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsPsDisplayMode");
       if (mService != null) {
          try {
             return mService.txSetRdsPsDisplayMode(dispalyMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsPsScrollSpeed(int scrollSpeed) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsPsScrollSpeed");
       if (mService != null) {
          try {
             return mService.txSetRdsPsScrollSpeed(scrollSpeed);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsTextRtMsg(int msgType, String msg, int msgLength) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsTextRtMsg");
       if (mService != null) {
          try {
             return mService.txSetRdsTextRtMsg(msgType, msg, msgLength);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsTransmittedGroupsMask(long rdsTrasmittedGrpMask) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsTransmittedGroupsMask");
       if (mService != null) {
          try {
             return mService.txSetRdsTransmittedGroupsMask(rdsTrasmittedGrpMask);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsTrafficCodes(int taCode, int tpCode) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsTrafficCodes");
       if (mService != null) {
          try {
             return mService.txSetRdsTrafficCodes(taCode, tpCode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsMusicSpeechFlag(int musicSpeechFlag) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsMusicSpeechFlag");
       if (mService != null) {
          try {
             return mService.txSetRdsMusicSpeechFlag(musicSpeechFlag);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txSetRdsECC(int ecc) {
       if (DBG)
          Log.d(TAG, "FmRadio:txSetRdsECC");
       if (mService != null) {
          try {
             return mService.txSetRdsECC(ecc);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txChangeAudioSource(int audioSrc, int ecalSampleFreq) {
       if (DBG)
          Log.d(TAG, "FmRadio:txChangeAudioSource");
       if (mService != null) {
          try {
             return mService.txChangeAudioSource(audioSrc, ecalSampleFreq);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean txChangeDigitalSourceConfiguration(int ecalSampleFreq) {
       if (DBG)
          Log.d(TAG, "FmRadio:txChangeDigitalSourceConfiguration");
       if (mService != null) {
          try {
             return mService.txChangeDigitalSourceConfiguration(ecalSampleFreq);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    /**
    * Returns the current FM TX state
    */
    public int txGetFMState() {
       if (DBG)
          Log.d(TAG, "FmRadio:txGetFMState");
       if (mService != null) {
          try {
             return mService.txGetFMState();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return -1;
    }

}
