package com.mango;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class ScreenUsageMonitorService extends Service {
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int GET_COUNTER = 3;
    private static Messenger clientMessenger;
    private Messenger serviceMessenger;
    private static int counter;
    private Timer timer;
    private TimerTask timerTask;


    public ScreenUsageMonitorService() {
        super();
        Log.i(ScreenUsageMonitorService.class.getSimpleName(), "Instance created (Empty ctor)");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(ScreenUsageMonitorService.class.getSimpleName(), "onStartCommand");
        setupService();
        return START_STICKY;
    }

    private void setupService() {
        Log.i(ScreenUsageMonitorService.class.getSimpleName(), "Started");
        serviceMessenger = new Messenger(new ClientMessegesHandler());
        counter = 0;
        startTimer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(ScreenUsageMonitorService.class.getSimpleName(), "onDestroy");
        Intent broadcastIntent = new Intent("com.mango.action.RestartService");
        sendBroadcast(broadcastIntent);
        stoptimertask();
    }

    private void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 1000, 1000);
    }

    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                Log.i(ScreenUsageMonitorService.class.getSimpleName(), "in timer ++++  "+ (counter++));
            }
        };
    }

    /**
     * not needed
     */
    private void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    static  class ClientMessegesHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    clientMessenger = msg.replyTo;
                    Log.i(ScreenUsageMonitorService.class.getSimpleName(), "Registered client");
                    break;
                case MSG_UNREGISTER_CLIENT:
                    clientMessenger = null;
                    break;
                case GET_COUNTER:
                    try {
                        clientMessenger.send(Message.obtain(null,
                                GET_COUNTER, getCounter(), 0));
                    } catch (RemoteException e) {
                        clientMessenger = null;
                    } catch (NullPointerException e){
                        e.printStackTrace();
                    }

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceMessenger.getBinder();
    }

    private static int getCounter(){
        return counter;
    }
}
