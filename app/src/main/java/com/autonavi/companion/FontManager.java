package com.autonavi.companion;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

/**
 * 字体管理器
 * 支持自定义字体加载，具有缓存和重试机制
 */
final class FontManager {
    private static final String TAG = "FontManager";
    private static final int MAX_RETRIES = 3;
    private static final long CACHE_EXPIRY_MS = 5000; // 缓存过期时间（毫秒）

    private static final String[] FONT_PATHS = {
            "amap_companion/font.ttf",
            "amap_companion/font.otf",
            "amap_companion/custom_font.ttf",
            "amap_companion/custom_font.otf",
            "amap_companion/diy/font.ttf",
            "amap_companion/diy/font.otf",
            "amap_companion/diy/custom_font.ttf",
            "amap_companion/diy/custom_font.otf"
    };

    // 缓存相关字段
    private static String cachedPath;
    private static long cachedModified;
    private static Typeface cachedTypeface;
    private static long lastLoadTime;

    private FontManager() {
    }

    static Typeface regular(Context context) {
        return customTypeface(context);
    }

    static Typeface styled(Context context, int style) {
        Typeface base = customTypeface(context);
        if (base == null) {
            return style == Typeface.BOLD ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        }
        return Typeface.create(base, style);
    }

    static Typeface bold(Context context) {
        return styled(context, Typeface.BOLD);
    }

    static void applyToViewTree(Context context, View view) {
        Typeface custom = customTypeface(context);
        if (custom == null || view == null) {
            return;
        }
        applyToViewTreeInternal(custom, view);
    }

    private static void applyToViewTreeInternal(Typeface custom, View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            Typeface current = textView.getTypeface();
            int style = current == null ? Typeface.NORMAL : current.getStyle();
            textView.setTypeface(Typeface.create(custom, style));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyToViewTreeInternal(custom, group.getChildAt(i));
            }
        }
    }

    /**
     * 加载自定义字体
     * 具有缓存机制：同一文件在5秒内不重复加载
     * 具有重试机制：加载失败最多重试3次
     */
    private static Typeface customTypeface(Context context) {
        File file = findFontFile();
        if (file == null) {
            clearCache();
            return null;
        }

        String path = file.getAbsolutePath();
        long modified = file.lastModified();
        long now = System.currentTimeMillis();

        // 检查缓存是否有效（路径和修改时间一致，且未过期）
        if (cachedTypeface != null && path.equals(cachedPath) 
                && modified == cachedModified && (now - lastLoadTime) < CACHE_EXPIRY_MS) {
            return cachedTypeface;
        }

        // 尝试加载字体，最多重试3次
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Typeface typeface = Typeface.createFromFile(file);
                
                // 加载成功，更新缓存
                cachedPath = path;
                cachedModified = modified;
                cachedTypeface = typeface;
                lastLoadTime = now;
                
                if (attempt > 1) {
                    Log.d(TAG, "字体加载成功（重试次数：" + attempt + "）: " + path);
                }
                return typeface;
                
            } catch (Throwable e) {
                Log.w(TAG, "字体加载失败（尝试 " + attempt + "/" + MAX_RETRIES + "）: " + path + " - " + e.getMessage());
                
                // 最后一次尝试失败
                if (attempt == MAX_RETRIES) {
                    Log.e(TAG, "字体加载最终失败，已达到最大重试次数: " + path);
                    clearCache();
                }
                
                // 短暂等待后重试
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * 查找字体文件
     * 遍历预设的路径列表，返回第一个找到的有效字体文件
     */
    private static File findFontFile() {
        File root = Environment.getExternalStorageDirectory();
        if (root == null) {
            Log.w(TAG, "外部存储目录不可用");
            return null;
        }

        for (String relativePath : FONT_PATHS) {
            File file = new File(root, relativePath);
            if (file.isFile() && file.length() > 0) {
                Log.d(TAG, "找到字体文件: " + file.getAbsolutePath());
                return file;
            }
        }
        
        Log.d(TAG, "未找到任何字体文件");
        return null;
    }

    /**
     * 清除字体缓存
     */
    private static void clearCache() {
        cachedPath = null;
        cachedModified = 0L;
        cachedTypeface = null;
        lastLoadTime = 0L;
    }
}
