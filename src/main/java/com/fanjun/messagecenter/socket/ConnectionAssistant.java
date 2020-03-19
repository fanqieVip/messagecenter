package com.fanjun.messagecenter.socket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;

import com.fanjun.messagecenter.MessageCenter;

import java.util.logging.Handler;

/**
 * Socket连接助手
 */
public class ConnectionAssistant extends Thread implements ConnectStateListener {
    /**
     * 连接状态
     */
    private ConnetState connetState;
    /**
     * socket检查连接间隔时间
     */
    private int socketCheckIntervalTime;
    private Context context;
    private ScreenStateReceiver screenStateReceiver;
    private NetworkReceiver networkReceiver;
    private boolean threadLive;
    private SocketInterceptor socketInterceptor;
    private android.os.Handler handler;

    public ConnectionAssistant(Context context, int socketCheckIntervalTime, SocketInterceptor socketInterceptor) {
        this.context = context;
        this.handler = new android.os.Handler(Looper.getMainLooper());
        this.socketInterceptor = socketInterceptor;
        this.socketCheckIntervalTime = socketCheckIntervalTime;
        connetState = ConnetState.CANCEL;
        threadLive = true;
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction("android.intent.action.SCREEN_OFF");
        screenStateFilter.addAction("android.intent.action.SCREEN_ON");
        screenStateFilter.addAction("android.intent.action.USER_PRESENT");
        screenStateReceiver = new ScreenStateReceiver();
        context.registerReceiver(screenStateReceiver, screenStateFilter);
        IntentFilter networkFilter = new IntentFilter();
        networkFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        networkFilter.addAction("android.net.wifi.STATE_CHANGE");
        networkReceiver = new NetworkReceiver();
        context.registerReceiver(networkReceiver, networkFilter);
    }

    @Override
    public void run() {
        super.run();
        changeConnectState(ConnetState.CONNECTING, null);
        while (threadLive) {
            try {
                Thread.sleep(socketCheckIntervalTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (connetState == ConnetState.INTERRUPT && AppUtils.isNetworkAvailable(context)) {
                reConnection();
            }
        }
    }

    /**
     * 关闭
     */
    public void shutDown() {
        try {
            threadLive = false;
            if (this.isAlive() || !this.isInterrupted()) {
                this.interrupt();
            }
            changeConnectState(ConnetState.CANCEL, null);
            context.unregisterReceiver(screenStateReceiver);
            context.unregisterReceiver(networkReceiver);
        }catch (Exception e){}
    }

    /**
     * 关闭
     */
    public void reboot() {
        try {
            threadLive = false;
            if (this.isAlive() || !this.isInterrupted()) {
                this.interrupt();
            }
            context.unregisterReceiver(screenStateReceiver);
            context.unregisterReceiver(networkReceiver);
        }catch (Exception e){}
    }

    @Override
    public void connectionInterrupt(Exception exception) {
        changeConnectState(ConnetState.INTERRUPT, exception);
    }

    @Override
    public void connectionSuccess() {
        changeConnectState(ConnetState.SUCCESS, null);
    }

    /**
     * 重新连接
     */
    private void reConnection() {
        connetState = ConnetState.CONNECTING;
        MessageCenter.connectSocket();
    }

    /**
     * 变更网络状态
     */
    private void changeConnectState(final ConnetState connetState, final Exception e) {
        if (this.connetState != connetState) {
            this.connetState = connetState;
            if (socketInterceptor != null) {
                try {
                    handler.postAtFrontOfQueue(new Runnable() {
                        @Override
                        public void run() {
                            socketInterceptor.connectState(connetState, e);
                        }
                    });
                }catch (Exception e1){
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 监听网络变化情况，断网情况下需对socket进行自动重连
     */
    public class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 监听网络连接，包括wifi和移动数据的打开和关闭,以及连接上可用的连接都会接到监听
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                //获取联网状态的NetworkInfo对象
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    //如果当前的网络连接成功并且网络连接可用
                    if (NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()) {
                        if (info.getType() == ConnectivityManager.TYPE_WIFI
                                || info.getType() == ConnectivityManager.TYPE_MOBILE) {
                            //网络连接上了
                            if (connetState == ConnetState.INTERRUPT) {
                                reConnection();
                            }
                        }
                    } else {
                        //网络断开了
                        changeConnectState(ConnetState.INTERRUPT, new Exception(new Throwable("网络断开了")));
                    }
                }
            }
        }
    }

    /**
     * 屏幕状态广播
     */
    public class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            //屏幕已点亮，连接socket
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (connetState == ConnetState.INTERRUPT && AppUtils.isNetworkAvailable(context)) {
                    reConnection();
                }
            }
        }

    }
}
