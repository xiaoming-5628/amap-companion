package com.autonavi.companion;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LauncherActivity extends Activity {

    private GridView appGrid;
    private AppAdapter appAdapter;
    private List<AppInfo> appList = new ArrayList<>();
    private TextView timeText;
    private TextView dateText;
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable;

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
        root.setBackground(createBackgroundGradient());
        root.setPadding(dp(32), dp(24), dp(32), dp(24));
        setContentView(root);

        LinearLayout statusBar = new LinearLayout(this);
        statusBar.setOrientation(LinearLayout.HORIZONTAL);
        statusBar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusBarLp = new LinearLayout.LayoutParams(-1, -2);
        statusBarLp.setMargins(0, 0, 0, dp(16));
        root.addView(statusBar, statusBarLp);

        LinearLayout leftInfo = new LinearLayout(this);
        leftInfo.setOrientation(LinearLayout.VERTICAL);
        leftInfo.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams leftInfoLp = new LinearLayout.LayoutParams(0, -2, 1f);
        statusBar.addView(leftInfo, leftInfoLp);

        timeText = new TextView(this);
        timeText.setTextSize(48f);
        timeText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        timeText.setTextColor(0xFFFFFFFF);
        leftInfo.addView(timeText);

        dateText = new TextView(this);
        dateText.setTextSize(16f);
        dateText.setTextColor(0xB3FFFFFF);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(-2, -2);
        dateLp.setMargins(0, dp(-4), 0, 0);
        leftInfo.addView(dateText, dateLp);

        LinearLayout rightInfo = new LinearLayout(this);
        rightInfo.setOrientation(LinearLayout.HORIZONTAL);
        rightInfo.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        statusBar.addView(rightInfo);

        TextView settingsBtn = createRoundedButton("设置", 0x33FFFFFF, 0x66FFFFFF);
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
        rightInfo.addView(settingsBtn);

        appGrid = new GridView(this);
        appGrid.setNumColumns(6);
        appGrid.setVerticalSpacing(dp(32));
        appGrid.setHorizontalSpacing(dp(24));
        appGrid.setSelector(android.R.color.transparent);
        appGrid.setBackgroundColor(Color.TRANSPARENT);
        appGrid.setPadding(dp(8), dp(16), dp(8), dp(8));
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(appGrid, gridLp);

        appAdapter = new AppAdapter();
        appGrid.setAdapter(appAdapter);

        loadApps();
        startTimeUpdate();
    }

    private GradientDrawable createBackgroundGradient() {
        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.TR_BL);
        gradient.setColors(new int[]{0xFF0F172A, 0xFF1E293B, 0xFF0F172A});
        return gradient;
    }

    private TextView createRoundedButton(String text, int bgColor, int textColor) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(16f);
        btn.setTextColor(textColor);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(24), dp(12), dp(24), dp(12));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), 0x33FFFFFF);
        btn.setBackground(bg);
        
        return btn;
    }

    private void startTimeUpdate() {
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void updateTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault());
        Date now = new Date();
        
        timeText.setText(timeFormat.format(now));
        dateText.setText(dateFormat.format(now));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        startTimeUpdate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
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
                itemLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
            } else {
                itemLayout = (LinearLayout) convertView;
            }

            itemLayout.removeAllViews();

            AppInfo appInfo = getItem(position);

            LinearLayout iconContainer = new LinearLayout(LauncherActivity.this);
            iconContainer.setGravity(Gravity.CENTER);
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setColor(0x1AFFFFFF);
            iconBg.setCornerRadius(dp(24));
            iconBg.setStroke(dp(1), 0x26FFFFFF);
            iconContainer.setBackground(iconBg);
            iconContainer.setPadding(dp(16), dp(16), dp(16), dp(16));
            LinearLayout.LayoutParams iconContainerLp = new LinearLayout.LayoutParams(dp(100), dp(100));
            iconContainerLp.setMargins(0, 0, 0, dp(12));
            itemLayout.addView(iconContainer, iconContainerLp);

            ImageView iconView = new ImageView(LauncherActivity.this);
            iconView.setImageDrawable(appInfo.icon);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(64), dp(64));
            iconContainer.addView(iconView, iconLp);

            TextView labelView = new TextView(LauncherActivity.this);
            labelView.setText(appInfo.label);
            labelView.setTextSize(13f);
            labelView.setTextColor(0xE6FFFFFF);
            labelView.setGravity(Gravity.CENTER);
            labelView.setMaxLines(2);
            labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labelView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            itemLayout.addView(labelView, new LinearLayout.LayoutParams(-2, -2));

            itemLayout.setOnClickListener(v -> {
                animateClick(itemLayout);
                launchApp(appInfo);
            });

            itemLayout.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    animateFocus(itemLayout, true);
                } else {
                    animateFocus(itemLayout, false);
                }
            });

            return itemLayout;
        }

        private void animateClick(View view) {
            ScaleAnimation scale = new ScaleAnimation(1f, 0.95f, 1f, 0.95f, 
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scale.setDuration(100);
            scale.setRepeatMode(Animation.REVERSE);
            scale.setRepeatCount(1);
            view.startAnimation(scale);
        }

        private void animateFocus(View view, boolean focused) {
            if (focused) {
                ScaleAnimation scale = new ScaleAnimation(1f, 1.1f, 1f, 1.1f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scale.setDuration(200);
                scale.setFillAfter(true);
                view.startAnimation(scale);
            } else {
                ScaleAnimation scale = new ScaleAnimation(1.1f, 1f, 1.1f, 1f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scale.setDuration(200);
                scale.setFillAfter(true);
                view.startAnimation(scale);
            }
        }
    }
}
