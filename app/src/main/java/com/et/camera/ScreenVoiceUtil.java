package com.et.camera;

import static android.media.AudioManager.STREAM_MUSIC;

import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

/**
 * @author Liuyq
 * @date 2018/12/20
 * 屏幕声音相关
 */
public class ScreenVoiceUtil {

    private static final String TAG = "ScreenVoiceUtil";
    private static ScreenVoiceUtil screenVoiceUtil;
    private static float scale = -1f;

    public static ScreenVoiceUtil getInstance() {
        if (null == screenVoiceUtil) {
            synchronized (ScreenVoiceUtil.class) {
                if (null == screenVoiceUtil) {
                    screenVoiceUtil = new ScreenVoiceUtil();
                }
            }
        }
        return screenVoiceUtil;
    }

    private WeakReference<Context> mWeakReference;
    private String screenDirection = "1"; // 0横屏，1竖屏。默认竖屏
    private String screenResolution = ""; // 分辨率
    private String size = ""; // 尺寸
    private int voiceNow = -1; // 当前声音
    private String widthAndHeight = ""; // 宽*高

    public int getVoiceNow() {
        return voiceNow;
    }

    public String getScreenDirection() {
        return screenDirection;
    }

    public String getScreenResolution() {
        return screenResolution;
    }

    public String getSize() {
        return size;
    }

    public String getWidthAndHeight() {
        return widthAndHeight;
    }

    /**
     * 防止设置音量失败及NULL异常
     *
     * @return
     */
    public boolean weakIsNull() {
        return null == mWeakReference || null == mWeakReference.get();
    }

    public void init(Context context) {
        mWeakReference = new WeakReference<>(context);
        voiceNow = getVolume();
        getScreenConfig(mWeakReference.get());
    }

    public int getScreenWidth() {
        int mScreenWidth = 1080;
        String widthAndHeight = ScreenVoiceUtil.getInstance().getWidthAndHeight();
        if (!widthAndHeight.isEmpty()) {
            String[] split = widthAndHeight.split("\\*");
            if (split.length >= 2) {
                mScreenWidth = ScreenVoiceUtil.getInstance().px2dip(Float.parseFloat(split[0]));
            }
        }
        return mScreenWidth;
    }

    public int getScreenHeight() {
        int screenHeight = 1920;
        String widthAndHeight = ScreenVoiceUtil.getInstance().getWidthAndHeight();
        if (!widthAndHeight.isEmpty()) {
            String[] split = widthAndHeight.split("\\*");
            if (split.length >= 2) {
                screenHeight = ScreenVoiceUtil.getInstance().px2dip(Float.parseFloat(split[1]));
            }
        }
        return screenHeight;
    }

    /**
     * 调整音量 0-15
     *
     * @param volume
     * @return
     */
    public void setVolume(int volume) {
        if (volume < 0) return;
        try {
            if (volume == voiceNow) {
                // 音量相同
                Log.d("et_log", "当前音量：".concat(String.valueOf(voiceNow))
                        .concat(" 调至：").concat(String.valueOf(volume)));
                return;
            }
            Context context = mWeakReference.get();
            if (null == context) return;
//            if (null == context) return;
            //获取系统的Audio管理者
            AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//            if (null == mAudioManager) return;
            //最大音量
            int maxVolume = mAudioManager.getStreamMaxVolume(STREAM_MUSIC);
            //当前音量
            int currentVolume = mAudioManager.getStreamVolume(STREAM_MUSIC);
            if (volume > maxVolume) volume = maxVolume;
            mAudioManager.setStreamVolume(STREAM_MUSIC, volume, 0);
            Log.d("et_log", "当前音量：".concat(String.valueOf(currentVolume))
                    .concat(" 调至：").concat(String.valueOf(volume)));
            voiceNow = volume;
        } catch (Exception e) {
            Log.d("et_log", "E-setVolume: ", e);
        }
    }

    /**
     * 获取当前音量
     *
     * @return
     */
    private int getVolume() {
        try {
            Context context = mWeakReference.get();
            if (null == context) return -1;
            //获取系统的Audio管理者
            AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (null == mAudioManager) return -1;
            //当前音量
            return mAudioManager.getStreamVolume(STREAM_MUSIC);
        } catch (Exception e) {
            Log.d("et_log", "E-getVolume: ", e);
            return 0;
        }
    }

    /**
     * 修改屏幕亮度 0-255（<=0为默认屏幕亮度）
     *
     * @param brightness
     */
    public void changeAppBrightness(int brightness) {
        try {
            AppCompatActivity context = (AppCompatActivity) mWeakReference.get();
            // 当前屏幕亮度
            int systemBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            // 设置屏幕亮度
            Window window = context.getWindow();
            WindowManager.LayoutParams lp = window.getAttributes();
            if (brightness > 255) {
                lp.screenBrightness = 255;
            } else if (brightness < 0) {
                lp.screenBrightness =
                        WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            }
            Log.d("et_log", "当前屏幕亮度：" + systemBrightness + " 设置屏幕亮度：" + brightness);
            window.setAttributes(lp);
        } catch (Exception e) {
            Log.d("et_log", "E-update screen light: ", e);
        }
    }

    /**
     * 获取屏幕相关参数
     *
     * @return screenList 没有则为0
     * 0 方向
     * 1 分辨率（width * height）
     * 2 尺寸
     */
    private ArrayList<String> getScreenConfig(Context context) {
        ArrayList<String> screenList = new ArrayList<>();
        try {
            if (null == context) {
                return screenList;
            }
            Configuration mConfiguration = context.getResources().getConfiguration(); //获取设置的配置信息
            int ori = mConfiguration.orientation; //获取屏幕方向
//            LogUtils.e(ori);
            if (ori == Configuration.ORIENTATION_LANDSCAPE) {
                //横屏
                screenDirection = "0";
//                Vending.isLand = true;
                Log.d("et_log", "屏幕方向：横屏");
            } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
                //竖屏
//                Vending.isLand = false;
                screenDirection = "1";
                Log.d("et_log", "屏幕方向：竖屏");
            }
            screenList.add(screenDirection);
            String screenDensity = getScreenDensity(context);
            String[] split = screenDensity.split(",");
            screenResolution = split[0];
            size = split[1];
            if (split.length == 2) {
                screenList.add(split[0]);
                screenList.add(split[1]);
            }
            return screenList;
        } catch (Exception e) {
            Log.d("et_log", "E-getScreenConfig: ", e);
            return screenList;
        }
    }

    /**
     * 获取屏幕分辨率、尺寸
     *
     * @param context
     */
    private String getScreenDensity(Context context) {
        try {
            // 通过Resources获取
            DisplayMetrics mDisplayMetrics = context.getResources().getDisplayMetrics();
            int width = mDisplayMetrics.widthPixels;
            int height = mDisplayMetrics.heightPixels;
            float density = mDisplayMetrics.density;
            int densityDpi = mDisplayMetrics.densityDpi;
            String densityStr = width + "*" + height;
            widthAndHeight = densityStr;
            Log.d("et_log", "分辨率：" + densityStr + "，屏幕密度："
                    + density + "，屏幕密度Dpi：" + densityDpi);
            // 尺寸
            double size = Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2)) / densityDpi;
            String sizeStr = String.format(Locale.getDefault(), "%.2f", size);
            size = Double.parseDouble(sizeStr);
            Log.d("et_log", "尺寸：" + size);
            return densityStr.concat(",").concat(String.valueOf(size));
        } catch (Exception e) {
            Log.d("et_log", "E-screenDensity: ", e);
            return "";
        }
    }

    // 根据手机的分辨率从 dp 的单位 转成为 px(像素)
    public int dip2px(float dpValue) {
        if (scale > 0f) {
            return (int) (dpValue * scale + 0.5f);
        } else {
            if (null != mWeakReference.get()) {
                scale = mWeakReference.get().getResources().getDisplayMetrics().density;
                return (int) (dpValue * scale + 0.5f);
            }
        }

        return -1;
    }

    // 根据手机的分辨率从 px(像素) 的单位 转成为 dp
    public int px2dip(float pxValue) {
        if (scale > 0f) {
            return (int) (pxValue / scale + 0.5f);
        } else {
            if (null != mWeakReference && null != mWeakReference.get()) {
                scale = mWeakReference.get().getResources().getDisplayMetrics().density;
                return (int) (pxValue / scale + 0.5f);
            }
        }
        return -1;
    }
}
