package com.mango;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private Intent serviceIntent;
    private ScreenUsageMonitorService screenUsageMonitorService;
    private TextView counterTextView;
    private Messenger serviceMessenger;
    private Messenger clientMessenger;
    private Timer timer;
    private static int counter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i (MainActivity.class.getSimpleName(),"onCreate");
        setContentView(R.layout.activity_main);
        screenUsageMonitorService = new ScreenUsageMonitorService();
        clientMessenger = new Messenger(new ScreenUsageMonitoringServiceMessagesHandler());
        serviceIntent = new Intent(this, screenUsageMonitorService.getClass());
        if (!isMyServiceRunning(screenUsageMonitorService.getClass())) {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        Log.i (MainActivity.class.getSimpleName(),"onStart");
        bindService(new Intent(this,
                screenUsageMonitorService.getClass()), serviceConnection, Context.BIND_AUTO_CREATE);
        counterTextView = findViewById(R.id.counterTextView);

    }

    @Override
    protected void onResume(){
        super.onResume();
        counterTextView.setText(String.valueOf(counter));
    }

    protected void onPause(){
        super.onPause();
    }


    protected void onStop(){
        super.onStop();
        stopTimer();
        doUnbindService();
    }

    @Override
    protected void onDestroy() {
        Log.i(MainActivity.class.getSimpleName(), "onDestroy!");
        stopService(serviceIntent);
        super.onDestroy();

    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            if (findServiceAndCheckIfRun(serviceClass, manager)) return true;
        }
        Log.i (MainActivity.class.getSimpleName(),"isMyServiceRunning? " + false+"");
        return false;
    }

    private boolean findServiceAndCheckIfRun(Class<?> serviceClass, ActivityManager manager) {
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i (MainActivity.class.getSimpleName(),"isMyServiceRunning? " + true+"");
                return true;
            }
        }
        return false;
    }

    static class ScreenUsageMonitoringServiceMessagesHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ScreenUsageMonitorService.GET_COUNTER:
                    counter = msg.arg1;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            serviceMessenger = new Messenger(service);
            try {
                Message msg = Message.obtain(null,
                        ScreenUsageMonitorService.MSG_REGISTER_CLIENT);
                msg.replyTo = clientMessenger;
                serviceMessenger.send(msg);
                Log.i(MainActivity.class.getSimpleName(), "Connected to " + ScreenUsageMonitorService.class.getSimpleName());
            } catch (RemoteException e) {
                Log.e(MainActivity.class.getSimpleName(), "Failed Connect to " + ScreenUsageMonitorService.class.getSimpleName());
                e.printStackTrace();
            }
            startTimer();
        }

        public void onServiceDisconnected(ComponentName className) {
            serviceMessenger = null;
            Log.i(MainActivity.class.getSimpleName(), "Disconnected from " + ScreenUsageMonitorService.class.getSimpleName());
        }
    };

    void doUnbindService() {
        if (serviceMessenger != null) {
            try {
                Message msg = Message.obtain(null,
                        ScreenUsageMonitorService.MSG_UNREGISTER_CLIENT);
                serviceMessenger.send(msg);
                unbindService(serviceConnection);
            } catch (RemoteException e) {
                Log.e(MainActivity.class.getSimpleName(), "Failed disconnect from " + ScreenUsageMonitorService.class.getSimpleName());
            }
        }
    }

    private void startTimer() {
        timer = new Timer();
        TimerTask timerTask = initializeTimerTask();
        timer.schedule(timerTask, 1000, 1000);
        Log.i(MainActivity.class.getSimpleName(), "Timer for getCounter scheduled");
    }

    private TimerTask initializeTimerTask() {
        return new TimerTask() {
            public void run() {
                try {
                    Message msg = Message.obtain(null,
                            ScreenUsageMonitorService.GET_COUNTER);
                    serviceMessenger.send(msg);
                } catch (RemoteException e) {
                    Log.w(MainActivity.class.getSimpleName(), "Failed send getCounter message");
                }
            }
        };
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
