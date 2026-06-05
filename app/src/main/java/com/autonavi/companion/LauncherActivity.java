package com.autonavi.companion;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LauncherActivity extends Activity {

    private GridView appGrid;
    private AppAdapter appAdapter;
    private List<AppInfo> appList = new ArrayList<>();

    static class AppInfo {
        String label;
        String packageName;
        Drawable icon;

        AppInfo(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1A1A2E);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        setContentView(root);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams topBarLp = new LinearLayout.LayoutParams(-1, -2);
        topBarLp.setMargins(0, 0, 0, dp(20));
        root.addView(topBar, topBarLp);

        TextView title = new TextView(this);
        title.setText("车载桌面");
        title.setTextSize(28f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1f);
        topBar.addView(title, titleLp);

        TextView settingsBtn = new TextView(this);
        settingsBtn.setText("设置");
        settingsBtn.setTextSize(18f);
        settingsBtn.setTextColor(0xFFE0E0E0);
        settingsBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
        settingsBtn.setBackgroundColor(0x33FFFFFF);
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
        topBar.addView(settingsBtn);

        appGrid = new GridView(this);
        appGrid.setNumColumns(4);
        appGrid.setVerticalSpacing(dp(24));
        appGrid.setHorizontalSpacing(dp(24));
        appGrid.setSelector(android.R.color.transparent);
        appGrid.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(-1, -1);
        root.addView(appGrid, gridLp);

        appAdapter = new AppAdapter();
        appGrid.setAdapter(appAdapter);

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PackageManager.MATCH_ALL : 0;
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, flags);
        
        appList.clear();
        
        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) {
                continue;
            }
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) {
                continue;
            }
            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            appList.add(new AppInfo(label, pkg, icon));
        }

        Collections.sort(appList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });

        appAdapter.notifyDataSetChanged();
    }

    private void launchApp(AppInfo appInfo) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        }
    }

    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density);
    }

    class AppAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return appList.size();
        }

        @Override
        public AppInfo getItem(int position) {
            return appList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout itemLayout;
            if (convertView == null) {
                itemLayout = new LinearLayout(LauncherActivity.this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setGravity(Gravity.CENTER);
                itemLayout.setPadding(dp(8), dp(8), dp(8), dp(8));
            } else {
                itemLayout = (LinearLayout) convertView;
            }

            itemLayout.removeAllViews();

            AppInfo appInfo = getItem(position);

            ImageView iconView = new ImageView(LauncherActivity.this);
            iconView.setImageDrawable(appInfo.icon);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(72), dp(72));
            iconLp.setMargins(0, 0, 0, dp(8));
            itemLayout.addView(iconView, iconLp);

            TextView labelView = new TextView(LauncherActivity.this);
            labelView.setText(appInfo.label);
            labelView.setTextSize(14f);
            labelView.setTextColor(Color.WHITE);
            labelView.setGravity(Gravity.CENTER);
            labelView.setMaxLines(2);
            labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            itemLayout.addView(labelView, new LinearLayout.LayoutParams(-2, -2));

            itemLayout.setOnClickListener(v -> launchApp(appInfo));

            return itemLayout;
        }
    }
}
