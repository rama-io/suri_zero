package com.rama.suri_zero.services;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.accessibility.AccessibilityEvent;

import com.rama.suri_zero.managers.ActionManager;
import com.rama.suri_zero.managers.LanternManager;

public class SuriService extends AccessibilityService {
    public static final int NOTCH_UNKNOWN = 0;
    public static final int NOTCH_FOUND = 1;
    public static final int NOTCH_MISSING = 2;

    public static volatile int notchState = NOTCH_UNKNOWN;

    private NotchOverlay overlay;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        LanternManager.register(this);
        overlay = new NotchOverlay(this, gesture -> ActionManager.run(this, gesture));
        overlay.show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (overlay != null) {
            overlay.show();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No event types are requested, nothing to do.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        tearDown();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        tearDown();
        super.onDestroy();
    }

    private void tearDown() {
        if (overlay != null) {
            overlay.hide();
            overlay = null;
        }
        LanternManager.unregister();
        notchState = NOTCH_UNKNOWN;
    }

    public static boolean isEnabled(Context context) {
        String enabled = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null || enabled.length() == 0) return false;
        ComponentName self = new ComponentName(context, SuriService.class);
        for (String entry : enabled.split(":")) {
            if (self.equals(ComponentName.unflattenFromString(entry))) {
                return true;
            }
        }
        return false;
    }
}
