package com.rama.suri_zero.managers;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Turns the whole screen white (a "screen lantern") using an accessibility overlay,
 * so no extra permission is needed. Tap anywhere on the screen, or run the action again, to turn it off.
 */
public final class ScreenLanternManager {
    private static View overlay;
    private static WindowManager windowManager;

    private ScreenLanternManager() {
    }

    public static synchronized boolean isOn() {
        return overlay != null;
    }

    public static synchronized void toggle(AccessibilityService service) {
        if (overlay != null) {
            hide();
        } else {
            show(service);
        }
    }

    public static synchronized void show(AccessibilityService service) {
        if (overlay != null) return;
        WindowManager wm = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
        if (wm == null) return;

        View view = new View(service);
        view.setBackgroundColor(Color.WHITE);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        view.setOnClickListener(v -> hide());

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        try {
            wm.addView(view, lp);
            overlay = view;
            windowManager = wm;
        } catch (RuntimeException ignored) {
            overlay = null;
            windowManager = null;
        }
    }

    public static synchronized void hide() {
        if (overlay != null && windowManager != null) {
            try {
                windowManager.removeViewImmediate(overlay);
            } catch (RuntimeException ignored) {
                // already gone
            }
        }
        overlay = null;
        windowManager = null;
    }
}
