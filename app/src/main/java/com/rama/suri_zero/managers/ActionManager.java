package com.rama.suri_zero.managers;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Toast;

import com.rama.suri_zero.R;
import com.rama.suri_zero.objects.Action;
import com.rama.suri_zero.objects.Gesture;

public final class ActionManager {
    private ActionManager() {
    }

    public static void run(AccessibilityService service, Gesture gesture) {
        PrefsManager prefs = PrefsManager.getInstance(service);
        Action action = prefs.getGestureAction(gesture);
        if (action == Action.NONE) return;
        try {
            execute(service, action, prefs.getGestureAmount(gesture, action), prefs.getGestureApp(gesture));
        } catch (Exception e) {
            toast(service, R.string.toast_action_failed);
        }
    }

    private static void execute(AccessibilityService service, Action action, float amount, String appPackage) throws Exception {
        switch (action) {
            case TOGGLE_LANTERN:
                if (!LanternManager.toggle(service)) {
                    toast(service, R.string.toast_no_flashlight);
                }
                break;
            case LOCK_SCREEN:
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
                break;
            case TAKE_SCREENSHOT:
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT);
                break;
            case OPEN_APP:
                openApp(service, appPackage);
                break;
            case PLAY_PAUSE_MUSIC:
                playPause(service);
                break;
            case BRIGHTNESS_UP:
                changeBrightness(service, amount, 1);
                break;
            case BRIGHTNESS_DOWN:
                changeBrightness(service, amount, -1);
                break;
            case VOLUME_UP:
                changeVolume(service, amount, 1);
                break;
            case VOLUME_DOWN:
                changeVolume(service, amount, -1);
                break;
            default:
                break;
        }
    }

    public static String describe(Context context, Gesture gesture) {
        PrefsManager prefs = PrefsManager.getInstance(context);
        Action action = prefs.getGestureAction(gesture);
        if (action == Action.OPEN_APP) {
            String pkg = prefs.getGestureApp(gesture);
            if (pkg == null) return context.getString(action.labelRes);
            return context.getString(R.string.action_open_app_named, appLabel(context, pkg));
        }
        return action.label(context, prefs.getGestureAmount(gesture, action));
    }

    private static String appLabel(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            return FontManager.sanitizeForFont(String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0))));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static void openApp(Context context, String pkg) {
        Intent launch = pkg == null ? null : context.getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) {
            toast(context, R.string.toast_unable_to_launch_app);
            return;
        }
        startFirst(context, launch);
    }

    private static void playPause(Context context) {
        AudioManager audio = audio(context);
        long now = SystemClock.uptimeMillis();
        audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0));
        audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0));
    }

    private static void changeVolume(Context context, float steps, int sign) {
        AudioManager audio = audio(context);
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int next = Math.max(0, Math.min(max, audio.getStreamVolume(AudioManager.STREAM_MUSIC) + sign * Math.max(1, Math.round(steps))));
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, AudioManager.FLAG_SHOW_UI);
    }

    private static void changeBrightness(Context context, float percent, int sign) {
        if (!canWriteSettings(context)) {
            toast(context, R.string.toast_need_write_settings);
            requestWriteSettings(context);
            return;
        }
        ContentResolver resolver = context.getContentResolver();
        int current;
        try {
            current = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            current = 128;
        }
        int next = Math.max(1, Math.min(255, current + sign * Math.max(1, Math.round(255f * percent / 100f))));
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, next);
    }

    public static boolean canWriteSettings(Context context) {
        return Settings.System.canWrite(context);
    }

    public static void requestWriteSettings(Context context) {
        startFirst(context, new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.getPackageName())));
    }

    private static AudioManager audio(Context context) {
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private static boolean startFirst(Context context, Intent... intents) {
        for (Intent intent : intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // try the next one
            }
        }
        toast(context, R.string.toast_unable_to_launch_app);
        return false;
    }

    private static void toast(Context context, int res) {
        Toast.makeText(context, res, Toast.LENGTH_SHORT).show();
    }
}
