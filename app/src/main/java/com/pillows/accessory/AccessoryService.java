package com.pillows.accessory;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.pillows.phonesafe.R;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by agudz on 06/01/16.
 */
public class AccessoryService extends SAAgent {
    private static final String TAG = "PhoneSafe";
    private static final int HELLOACCESSORY_CHANNEL_ID = 666;
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private final IBinder mBinder = new LocalBinder();
    private ServiceConnection mConnectionHandler = null;
    Handler mHandler = new Handler();

    public AccessoryService() {
        super(TAG, SASOCKET_CLASS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            // try to handle SsdkUnsupportedException
            if (processUnsupportedException(e) == true) {
                return;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            /*
             * Your application can not use Samsung Accessory SDK. Your application should work smoothly
             * without using this SDK, or you may want to notify user and close your application gracefully
             * (release resources, stop Service threads, close UI thread, etc.)
             */
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onFindPeerAgentResponse(SAPeerAgent peerAgent, int result) {
        Log.d(TAG, "onFindPeerAgentResponse : result =" + result);
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        if (peerAgent != null) {
            Toast.makeText(getBaseContext(), R.string.ConnectionAcceptedMsg, Toast.LENGTH_SHORT).show();
            acceptServiceConnectionRequest(peerAgent);
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        if (result == SAAgent.CONNECTION_SUCCESS) {
            if (socket != null) {
                mConnectionHandler = (ServiceConnection) socket;
            }
        } else if (result == SAAgent.CONNECTION_ALREADY_EXIST) {
            Log.e(TAG, "onServiceConnectionResponse, CONNECTION_ALREADY_EXIST");
        }
    }

    @Override
    protected void onAuthenticationResponse(SAPeerAgent peerAgent, SAAuthenticationToken authToken, int error) {
        /*
         * The authenticatePeerAgent(peerAgent) API may not be working properly depending on the firmware
         * version of accessory device. Please refer to another sample application for Security.
         */
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        super.onError(peerAgent, errorMessage, errorCode);
    }

    public class LocalBinder extends Binder {
        public AccessoryService getService() {
            return AccessoryService.this;
        }
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            if (mConnectionHandler == null) {
                return;
            }
            Calendar calendar = new GregorianCalendar();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd aa hh:mm:ss.SSS");
            String timeStr = " " + dateFormat.format(calendar.getTime());
            String strToUpdateUI = new String(data);

            Log.w(TAG, "before toast " + strToUpdateUI);
            Toast.makeText(getBaseContext(), strToUpdateUI, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "after toast " + strToUpdateUI);

            final String message = strToUpdateUI.concat(timeStr);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        mConnectionHandler.send(HELLOACCESSORY_CHANNEL_ID, message.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            mConnectionHandler = null;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), R.string.ConnectionTerminateddMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        switch (e.getType()) {
            case SsdkUnsupportedException.VENDOR_NOT_SUPPORTED:
                Log.e(TAG, "VENDOR_NOT_SUPPORTED");
                stopSelf();
                return true;
            case SsdkUnsupportedException.DEVICE_NOT_SUPPORTED:
                Log.e(TAG, "DEVICE_NOT_SUPPORTED");
                stopSelf();
                return true;
            case SsdkUnsupportedException.LIBRARY_NOT_INSTALLED:
                Log.e(TAG, "You need to install Samsung Accessory SDK to use this application.");
                return true;
            case SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED:
                Log.e(TAG, "You need to update Samsung Accessory SDK to use this application.");
                return true;
            case SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED:
                Log.e(TAG, "We recommend that you update your Samsung Accessory SDK before using this application.");
                return false;
        }
        return true;
    }
}
