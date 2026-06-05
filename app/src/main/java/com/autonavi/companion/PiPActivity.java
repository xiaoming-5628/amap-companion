package com.autonavi.companion;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PiPActivity extends Activity {

    public static final String PREF_PIP_PACKAGE = "pip_selected_package";
    public static final String PREF_PIP_LABEL = "pip_selected_label";
    private static final String EXTRA_START_PIP = "start_pip";

    private ListView appListView;
    private AppAdapter appAdapter;
    private List<AppInfo> appList = new ArrayList<>();
    private SharedPreferences prefs;
    private String selectedPackage;
    private String selectedLabel;

    static class AppInfo {
        String label;
        String packageName;
        android.graphics.drawable.Drawable icon;

        AppInfo(String label, String packageName, android.graphics.drawable.Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    public static void start(Context context, boolean startPiP) {
        Intent intent = new Intent(context, PiPActivity.class);
        intent.putExtra(EXTRA_START_PIP, startPiP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("amap_companion", MODE_PRIVATE);
        selectedPackage = prefs.getString(PREF_PIP_PACKAGE, null);
        selectedLabel = prefs.getString(PREF_PIP_LABEL, null);

        // 检查是否直接进入画中画模式
        if (getIntent().getBooleanExtra(EXTRA_START_PIP, false) && selectedPackage != null) {
            enterPictureInPictureMode();
            return;
        }

        createSelectionUI();
    }

    private void createSelectionUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0F172A);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        setContentView(root);

        // 标题
        TextView title = new TextView(this);
        title.setText("画中画应用选择");
        title.setTextSize(24f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.setMargins(0, 0, 0, dp(8));
        root.addView(title, titleLp);

        // 说明
        TextView subtitle = new TextView(this);
        subtitle.setText("选择一个应用以在画中画模式中快速启动");
        subtitle.setTextSize(14f);
        subtitle.setTextColor(0xB3FFFFFF);
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(-1, -2);
        subtitleLp.setMargins(0, 0, 0, dp(24));
        root.addView(subtitle, subtitleLp);

        // 当前选中的应用
        if (selectedPackage != null) {
            LinearLayout selectedLayout = new LinearLayout(this);
            selectedLayout.setOrientation(LinearLayout.HORIZONTAL);
            selectedLayout.setGravity(Gravity.CENTER_VERTICAL);
            selectedLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(0x1AFFFFFF);
            bg.setCornerRadius(dp(16));
            bg.setStroke(dp(1), 0x33FFFFFF);
            selectedLayout.setBackground(bg);
            LinearLayout.LayoutParams selectedLp = new LinearLayout.LayoutParams(-1, -2);
            selectedLp.setMargins(0, 0, 0, dp(24));
            root.addView(selectedLayout, selectedLp);

            ImageView icon = new ImageView(this);
            try {
                icon.setImageDrawable(getPackageManager().getApplicationIcon(selectedPackage));
            } catch (Exception e) {
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            icon.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
            selectedLayout.addView(icon);

            TextView selectedText = new TextView(this);
            selectedText.setText(selectedLabel);
            selectedText.setTextSize(16f);
            selectedText.setTextColor(0xFFFFFFFF);
            selectedText.setPadding(dp(16), 0, 0, 0);
            selectedLayout.addView(selectedText);

            Button launchBtn = createButton("立即启动画中画", 0xFF3B82F6);
            launchBtn.setOnClickListener(v -> launchSelectedAppAndPiP());
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, -2);
            btnLp.setMargins(0, dp(12), 0, 0);
            root.addView(launchBtn, btnLp);
        }

        // 应用列表
        appListView = new ListView(this);
        appListView.setDivider(null);
        appListView.setDividerHeight(0);
        appListView.setBackgroundColor(0xFF1E293B);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(appListView, listLp);

        appAdapter = new AppAdapter();
        appListView.setAdapter(appAdapter);

        loadApps();
    }

    private Button createButton(String text, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(16f);
        btn.setAllCaps(false);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        btn.setPadding(dp(24), dp(14), dp(24), dp(14));
        return btn;
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
            android.graphics.drawable.Drawable icon = info.loadIcon(pm);
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

    private void selectApp(AppInfo appInfo) {
        selectedPackage = appInfo.packageName;
        selectedLabel = appInfo.label;

        prefs.edit()
            .putString(PREF_PIP_PACKAGE, selectedPackage)
            .putString(PREF_PIP_LABEL, selectedLabel)
            .apply();

        Toast.makeText(this, "已选择: " + selectedLabel, Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void launchSelectedAppAndPiP() {
        if (selectedPackage == null) {
            Toast.makeText(this, "请先选择一个应用", Toast.LENGTH_SHORT).show();
            return;
        }

        // 启动选中的应用
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(selectedPackage);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        }

        // 延迟进入画中画模式
        new android.os.Handler().postDelayed(() -> {
            enterPictureInPictureMode();
        }, 500);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // 当用户按下Home键时自动进入画中画模式
        if (selectedPackage != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        if (isInPictureInPictureMode) {
            // 画中画模式：显示简化UI
            createPiPUI();
        } else {
            // 退出画中画模式：返回选择界面
            createSelectionUI();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void enterPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(9, 16);
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build();
            enterPictureInPictureMode(params);
        }
    }

    private void createPiPUI() {
        FrameLayout pipLayout = new FrameLayout(this);
        pipLayout.setBackgroundColor(0xFF0F172A);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        pipLayout.addView(content);

        if (selectedPackage != null) {
            ImageView icon = new ImageView(this);
            try {
                icon.setImageDrawable(getPackageManager().getApplicationIcon(selectedPackage));
            } catch (Exception e) {
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(64), dp(64));
            content.addView(icon, iconLp);

            TextView label = new TextView(this);
            label.setText(selectedLabel);
            label.setTextSize(14f);
            label.setTextColor(0xFFFFFFFF);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, dp(8), 0, 0);
            content.addView(label);
        }

        TextView hint = new TextView(this);
        hint.setText("点击重新打开");
        hint.setTextSize(10f);
        hint.setTextColor(0x80FFFFFF);
        hint.setPadding(0, dp(8), 0, 0);
        content.addView(hint);

        pipLayout.setOnClickListener(v -> {
            if (selectedPackage != null) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(selectedPackage);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                }
            }
        });

        setContentView(pipLayout);
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
                itemLayout = new LinearLayout(PiPActivity.this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setGravity(Gravity.CENTER_VERTICAL);
                itemLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
            } else {
                itemLayout = (LinearLayout) convertView;
            }

            itemLayout.removeAllViews();

            AppInfo appInfo = getItem(position);

            ImageView iconView = new ImageView(PiPActivity.this);
            iconView.setImageDrawable(appInfo.icon);
            iconView.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
            itemLayout.addView(iconView);

            TextView labelView = new TextView(PiPActivity.this);
            labelView.setText(appInfo.label);
            labelView.setTextSize(16f);
            labelView.setTextColor(0xFFFFFFFF);
            labelView.setPadding(dp(16), 0, 0, 0);
            labelView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            itemLayout.addView(labelView);

            if (appInfo.packageName.equals(selectedPackage)) {
                TextView checkMark = new TextView(PiPActivity.this);
                checkMark.setText("✓");
                checkMark.setTextSize(20f);
                checkMark.setTextColor(0xFF3B82F6);
                itemLayout.addView(checkMark);
            }

            itemLayout.setOnClickListener(v -> selectApp(appInfo));

            return itemLayout;
        }
    }
}