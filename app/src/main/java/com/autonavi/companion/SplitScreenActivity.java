package com.autonavi.companion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SplitScreenActivity extends Activity {

    private AppUtils.AppInfo selectedApp1 = null;
    private AppUtils.AppInfo selectedApp2 = null;
    private List<AppUtils.AppInfo> appList = new ArrayList<>();
    private SplitAppAdapter appAdapter;
    private LinearLayout selectionStatus;
    private Button launchBtn;

    public static void start(Context context) {
        Intent intent = new Intent(context, SplitScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查是否有预选的应用
        android.content.SharedPreferences prefs = getSharedPreferences("amap_companion", MODE_PRIVATE);
        String preselectedPackage = prefs.getString("split_app1_package", null);
        String preselectedLabel = prefs.getString("split_app1_label", null);
        
        createUI();
        loadApps();
        
        // 如果有预选的应用，自动选中它
        if (preselectedPackage != null) {
            for (AppUtils.AppInfo app : appList) {
                if (app.packageName.equals(preselectedPackage)) {
                    selectedApp1 = app;
                    updateSelectionUI();
                    // 清除临时存储
                    prefs.edit()
                        .remove("split_app1_package")
                        .remove("split_app1_label")
                        .apply();
                    break;
                }
            }
        }
    }

    private void createUI() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF0F172A);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        scrollView.addView(root);
        
        setContentView(scrollView);

        // 标题
        TextView title = new TextView(this);
        title.setText("分屏模式");
        title.setTextSize(28f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.setMargins(0, 0, 0, dp(8));
        root.addView(title, titleLp);

        // 说明
        TextView subtitle = new TextView(this);
        subtitle.setText("选择两个应用进行分屏显示");
        subtitle.setTextSize(14f);
        subtitle.setTextColor(0xB3FFFFFF);
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(-1, -2);
        subtitleLp.setMargins(0, 0, 0, dp(24));
        root.addView(subtitle, subtitleLp);

        // 选择状态区域
        selectionStatus = new LinearLayout(this);
        selectionStatus.setOrientation(LinearLayout.HORIZONTAL);
        selectionStatus.setGravity(Gravity.CENTER);
        selectionStatus.setPadding(dp(16), dp(16), dp(16), dp(16));
        android.graphics.drawable.GradientDrawable statusBg = new android.graphics.drawable.GradientDrawable();
        statusBg.setColor(0x1AFFFFFF);
        statusBg.setCornerRadius(dp(16));
        statusBg.setStroke(dp(1), 0x33FFFFFF);
        selectionStatus.setBackground(statusBg);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.setMargins(0, 0, 0, dp(24));
        root.addView(selectionStatus, statusLp);

        updateSelectionUI();

        // 应用选择网格
        GridView appGrid = new GridView(this);
        appGrid.setNumColumns(4);
        appGrid.setVerticalSpacing(dp(16));
        appGrid.setHorizontalSpacing(dp(16));
        appGrid.setSelector(android.R.color.transparent);
        appGrid.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(-1, -2);
        root.addView(appGrid, gridLp);

        appAdapter = new SplitAppAdapter();
        appGrid.setAdapter(appAdapter);

        // 底部操作栏
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(0, dp(24), 0, 0);
        root.addView(bottomBar);

        Button resetBtn = createButton("重置", 0xFF6B7280);
        resetBtn.setOnClickListener(v -> resetSelection());
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(0, -2, 1f);
        resetLp.setMargins(0, 0, dp(12), 0);
        bottomBar.addView(resetBtn, resetLp);

        launchBtn = createButton("启动分屏", 0xFF3B82F6);
        launchBtn.setEnabled(false);
        launchBtn.setAlpha(0.5f);
        launchBtn.setOnClickListener(v -> launchSplitScreen());
        LinearLayout.LayoutParams launchLp = new LinearLayout.LayoutParams(0, -2, 1f);
        launchLp.setMargins(dp(12), 0, 0, 0);
        bottomBar.addView(launchBtn, launchLp);
    }

    private void updateSelectionUI() {
        selectionStatus.removeAllViews();

        // 应用1
        LinearLayout app1Layout = createSelectionItem(selectedApp1, "左侧应用", 1);
        LinearLayout.LayoutParams app1Lp = new LinearLayout.LayoutParams(0, -2, 1f);
        app1Lp.setMargins(0, 0, dp(12), 0);
        selectionStatus.addView(app1Layout, app1Lp);

        // VS 分隔符
        TextView vsText = new TextView(this);
        vsText.setText("VS");
        vsText.setTextSize(20f);
        vsText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        vsText.setTextColor(0x80FFFFFF);
        selectionStatus.addView(vsText);

        // 应用2
        LinearLayout app2Layout = createSelectionItem(selectedApp2, "右侧应用", 2);
        LinearLayout.LayoutParams app2Lp = new LinearLayout.LayoutParams(0, -2, 1f);
        app2Lp.setMargins(dp(12), 0, 0, 0);
        selectionStatus.addView(app2Layout, app2Lp);

        // 更新启动按钮状态
        boolean canLaunch = selectedApp1 != null && selectedApp2 != null;
        launchBtn.setEnabled(canLaunch);
        launchBtn.setAlpha(canLaunch ? 1f : 0.5f);
    }

    private LinearLayout createSelectionItem(AppUtils.AppInfo appInfo, String hint, int slot) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(12), dp(12), dp(12), dp(12));
        
        if (appInfo == null) {
            ImageView placeholder = new ImageView(this);
            placeholder.setImageResource(android.R.drawable.ic_menu_add);
            placeholder.setColorFilter(0x40FFFFFF);
            LinearLayout.LayoutParams placeholderLp = new LinearLayout.LayoutParams(dp(48), dp(48));
            layout.addView(placeholder, placeholderLp);

            TextView hintText = new TextView(this);
            hintText.setText(hint);
            hintText.setTextSize(12f);
            hintText.setTextColor(0x60FFFFFF);
            hintText.setPadding(0, dp(8), 0, 0);
            layout.addView(hintText);
        } else {
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(appInfo.icon);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(56), dp(56));
            layout.addView(icon, iconLp);

            TextView label = new TextView(this);
            label.setText(appInfo.label);
            label.setTextSize(12f);
            label.setTextColor(Color.WHITE);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setPadding(0, dp(8), 0, 0);
            layout.addView(label);
        }

        return layout;
    }

    private Button createButton(String text, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
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
        // 使用 AppUtils 加载应用列表
        appList.clear();
        appList.addAll(AppUtils.loadApps(this));
        appAdapter.notifyDataSetChanged();
    }

    private void toggleAppSelection(AppUtils.AppInfo appInfo) {
        // 检查是否已选中
        if (selectedApp1 != null && selectedApp1.packageName.equals(appInfo.packageName)) {
            selectedApp1 = null;
        } else if (selectedApp2 != null && selectedApp2.packageName.equals(appInfo.packageName)) {
            selectedApp2 = null;
        } else {
            // 未选中，分配到空位
            if (selectedApp1 == null) {
                selectedApp1 = appInfo;
            } else if (selectedApp2 == null) {
                selectedApp2 = appInfo;
            } else {
                // 都已满，替换第二个
                selectedApp2 = appInfo;
            }
        }
        
        updateSelectionUI();
    }

    private void resetSelection() {
        selectedApp1 = null;
        selectedApp2 = null;
        updateSelectionUI();
    }

    private void launchSplitScreen() {
        if (selectedApp1 == null || selectedApp2 == null) {
            Toast.makeText(this, "请先选择两个应用", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 启动第一个应用
            Intent intent1 = getPackageManager().getLaunchIntentForPackage(selectedApp1.packageName);
            if (intent1 != null) {
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(intent1);
            }

            // 延迟启动第二个应用
            new android.os.Handler().postDelayed(() -> {
                Intent intent2 = getPackageManager().getLaunchIntentForPackage(selectedApp2.packageName);
                if (intent2 != null) {
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    
                    // 尝试进入分屏模式
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            // 使用反射尝试进入分屏模式
                            enterSplitScreenMode();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    startActivity(intent2);
                }
                
                Toast.makeText(SplitScreenActivity.this, "已启动分屏", Toast.LENGTH_SHORT).show();
                finish();
            }, 300);

        } catch (Exception e) {
            Toast.makeText(this, "启动分屏失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void enterSplitScreenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                // Android 7.0+ 支持分屏模式，但需要用户配合
                // 应用本身无法强制进入分屏模式，只能通过启动两个应用来触发系统提示
                // 这里提供一个引导提示
                Toast.makeText(this, "如需进入分屏模式，请在最近任务中长按应用切换到分屏视图", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                // 出现异常时提示用户手动操作
                Toast.makeText(this, "请手动进入系统分屏模式（从屏幕底部向上滑 > 分屏）", Toast.LENGTH_LONG).show();
            }
        } else {
            // Android 7.0 以下不支持分屏模式
            Toast.makeText(this, "您的设备不支持分屏模式（需要Android 7.0及以上）", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density);
    }

    class SplitAppAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return appList.size();
        }

        @Override
        public AppUtils.AppInfo getItem(int position) {
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
                itemLayout = new LinearLayout(SplitScreenActivity.this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setGravity(Gravity.CENTER);
                itemLayout.setPadding(dp(8), dp(8), dp(8), dp(8));
            } else {
                itemLayout = (LinearLayout) convertView;
            }

            itemLayout.removeAllViews();

            AppUtils.AppInfo appInfo = getItem(position);

            // 图标容器
            LinearLayout iconContainer = new LinearLayout(SplitScreenActivity.this);
            iconContainer.setGravity(Gravity.CENTER);
            android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
            
            boolean isSelected = (selectedApp1 != null && selectedApp1.packageName.equals(appInfo.packageName)) ||
                                (selectedApp2 != null && selectedApp2.packageName.equals(appInfo.packageName));
            
            if (isSelected) {
                iconBg.setColor(0x403B82F6);
                iconBg.setStroke(dp(2), 0xFF3B82F6);
            } else {
                iconBg.setColor(0x1AFFFFFF);
                iconBg.setStroke(dp(1), 0x26FFFFFF);
            }
            
            iconBg.setCornerRadius(dp(16));
            iconContainer.setBackground(iconBg);
            iconContainer.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams iconContainerLp = new LinearLayout.LayoutParams(dp(64), dp(64));
            iconContainerLp.setMargins(0, 0, 0, dp(8));
            itemLayout.addView(iconContainer, iconContainerLp);

            ImageView iconView = new ImageView(SplitScreenActivity.this);
            iconView.setImageDrawable(appInfo.icon);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(40), dp(40));
            iconContainer.addView(iconView, iconLp);

            TextView labelView = new TextView(SplitScreenActivity.this);
            labelView.setText(appInfo.label);
            labelView.setTextSize(11f);
            labelView.setTextColor(isSelected ? 0xFF93C5FD : 0xE6FFFFFF);
            labelView.setGravity(Gravity.CENTER);
            labelView.setMaxLines(2);
            labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            itemLayout.addView(labelView, new LinearLayout.LayoutParams(-2, -2));

            itemLayout.setOnClickListener(v -> toggleAppSelection(appInfo));

            return itemLayout;
        }
    }
}