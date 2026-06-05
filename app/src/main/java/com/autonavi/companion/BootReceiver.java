package com.autonavi.companion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "AmapCompanion";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) {
            return;
        }
        String action = intent == null ? "" : intent.getAction();
        boolean isAutoStartEvent = Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_SCREEN_ON.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action);
        if (!isAutoStartEvent) {
            return;
        }
        
        // 检查是否需要启动悬浮窗服务
        if (MainActivity.isAutoStartEnabled(context)) {
            Log.d(TAG, "auto start overlay service after " + action);
            MainActivity.startOverlayService(context);
        }
        
        // 检查是否需要在开机时显示车载桌面（只在BOOT_COMPLETED时）
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) && MainActivity.isAutoStartLauncherEnabled(context)) {
            Log.d(TAG, "auto start launcher after boot completed");
            Intent launcherIntent = new Intent(context, LauncherActivity.class);
            launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(launcherIntent);
        }
    }
}
