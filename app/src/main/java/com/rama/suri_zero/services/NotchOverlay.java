package com.rama.suri_zero.services;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

final class NotchOverlay {
    private static final int MIN_TARGET_DP = 48;
    private static final int PADDING_DP = 4;

    private final SuriService service;
    private final WindowManager windowManager;
    private final NotchTouchListener touchListener;

    private View probe;
    private View zone;

    NotchOverlay(SuriService service, NotchTouchListener.Callback callback) {
        this.service = service;
        this.windowManager = (WindowManager) service.getSystemService(SuriService.WINDOW_SERVICE);
        this.touchListener = new NotchTouchListener(service, callback);
    }

    void show() {
        hide();
        final View probeView = new View(service);
        probe = probeView;
        probeView.setOnApplyWindowInsetsListener((v, insets) -> {
            if (probe != probeView) return insets;
            final Rect cutout = largestCutout(insets);
            probeView.post(() -> {
                if (probe != probeView) return;
                removeQuietly(probeView);
                probe = null;
                SuriService.notchState = cutout == null ? SuriService.NOTCH_MISSING : SuriService.NOTCH_FOUND;
                if (cutout != null) {
                    addZone(cutout);
                }
            });
            return insets;
        });
        try {
            windowManager.addView(probeView, params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE));
        } catch (RuntimeException e) {
            probe = null;
        }
    }

    void hide() {
        if (probe != null) {
            removeQuietly(probe);
            probe = null;
        }
        if (zone != null) {
            removeQuietly(zone);
            zone = null;
        }
        touchListener.release();
        SuriService.notchState = SuriService.NOTCH_UNKNOWN;
    }

    private Rect largestCutout(WindowInsets insets) {
        DisplayCutout cutout = insets.getDisplayCutout();
        if (cutout == null) return null;
        Rect best = null;
        for (Rect rect : cutout.getBoundingRects()) {
            if (rect.isEmpty()) continue;
            if (best == null || area(rect) > area(best)) {
                best = rect;
            }
        }
        return best == null ? null : new Rect(best);
    }

    private static long area(Rect rect) {
        return (long) rect.width() * rect.height();
    }

    private void addZone(Rect cutout) {
        float density = service.getResources().getDisplayMetrics().density;
        int padding = Math.round(PADDING_DP * density);
        int minSize = Math.round(MIN_TARGET_DP * density);
        int width = Math.max(cutout.width() + 2 * padding, minSize);
        int height = Math.max(cutout.height() + 2 * padding, minSize);

        View view = new View(service);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        view.setOnTouchListener(touchListener);

        WindowManager.LayoutParams lp = params(width, height, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        lp.x = Math.max(0, cutout.centerX() - width / 2);
        lp.y = Math.max(0, cutout.centerY() - height / 2);
        try {
            windowManager.addView(view, lp);
            zone = view;
        } catch (RuntimeException e) {
            zone = null;
        }
    }

    private WindowManager.LayoutParams params(int width, int height, int extraFlags) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                extraFlags
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30
                ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        return lp;
    }

    private void removeQuietly(View view) {
        try {
            windowManager.removeViewImmediate(view);
        } catch (RuntimeException ignored) {
            // already gone
        }
    }
}
