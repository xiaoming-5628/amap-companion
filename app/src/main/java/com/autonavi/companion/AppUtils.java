package com.autonavi.companion;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 应用加载工具类
 * 提取 LauncherActivity、SplitScreenActivity、PiPActivity 中的公共应用加载逻辑
 */
public final class AppUtils {

    /**
     * 应用信息数据类
     */
    public static class AppInfo {
        public final String label;
        public final String packageName;
        public final Drawable icon;

        public AppInfo(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    private AppUtils() {
        // 工具类不允许实例化
    }

    /**
     * 加载设备上所有可启动的应用（排除当前应用自身）
     * @param context 上下文
     * @return 按名称排序的应用列表
     */
    public static List<AppInfo> loadApps(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // 根据 SDK 版本选择合适的查询标志
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PackageManager.MATCH_ALL : 0;
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, flags);

        List<AppInfo> appList = new ArrayList<>();
        String selfPackage = context.getPackageName();

        for (ResolveInfo info : resolveInfos) {
            // 跳过无效或自身应用
            if (info.activityInfo == null || info.activityInfo.packageName == null) {
                continue;
            }
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(selfPackage)) {
                continue;
            }

            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            appList.add(new AppInfo(label, pkg, icon));
        }

        // 按应用名称排序（不区分大小写）
        Collections.sort(appList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });

        return appList;
    }
}
