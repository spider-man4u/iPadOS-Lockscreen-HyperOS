package com.ipados.lockscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.KeyguardManager;
import android.os.Build;
import android.os.IBinder;

public class LockScreenService extends Service {
    private static final String CHANNEL_ID = "ipados_lockscreen_service";
    private BroadcastReceiver screenReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startInForeground();
        registerScreenReceiver();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "iPadOS Lockscreen Service",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Maintains iPadOS Lockscreen active on wake");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void startInForeground() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        Notification notification = builder
                .setContentTitle("iPadOS Lockscreen")
                .setContentText("Active")
                .setSmallIcon(R.drawable.ic_launcher)
                .setPriority(Notification.PRIORITY_MIN)
                .build();
        startForeground(1001, notification);
    }

    private void registerScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_SCREEN_OFF.equals(action)) {
                    KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                    if (km != null && km.isKeyguardLocked()) {
                        Intent lockIntent = new Intent(context, LockScreenActivity.class);
                        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        context.startActivity(lockIntent);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
