package com.rama.suri_zero.objects;

import android.content.Context;

import com.rama.suri_zero.R;

public enum Action {
    NONE("none", R.string.action_empty),
    OPEN_APP("open_app", R.string.action_open_app, true),
    TOGGLE_LANTERN("toggle_lantern", R.string.action_toggle_lantern),
    LOCK_SCREEN("lock_screen", R.string.action_lock_screen),
    TAKE_SCREENSHOT("take_screenshot", R.string.action_take_screenshot),
    PLAY_PAUSE_MUSIC("play_pause_music", R.string.action_play_pause_music),
    BRIGHTNESS_UP("brightness_up", R.string.action_brightness_up, R.string.hint_brightness, R.string.label_amount_percent, 1f, 100f),
    BRIGHTNESS_DOWN("brightness_down", R.string.action_brightness_down, R.string.hint_brightness, R.string.label_amount_percent, 1f, 100f),
    VOLUME_UP("volume_up", R.string.action_volume_up, R.string.hint_volume_steps, R.string.label_amount_steps, 1f, 30f),
    VOLUME_DOWN("volume_down", R.string.action_volume_down, R.string.hint_volume_steps, R.string.label_amount_steps, 1f, 30f);

    public final String id;
    public final int labelRes;
    public final int hintRes;
    public final int amountLabelRes;
    public final float defaultAmount;
    public final float maxAmount;
    public final boolean needsApp;

    Action(String id, int labelRes) {
        this(id, labelRes, 0, 0, 0f, 0f, false);
    }

    Action(String id, int labelRes, boolean needsApp) {
        this(id, labelRes, 0, 0, 0f, 0f, needsApp);
    }

    Action(String id, int labelRes, int hintRes, int amountLabelRes, float defaultAmount, float maxAmount) {
        this(id, labelRes, hintRes, amountLabelRes, defaultAmount, maxAmount, false);
    }

    Action(String id, int labelRes, int hintRes, int amountLabelRes, float defaultAmount, float maxAmount, boolean needsApp) {
        this.id = id;
        this.labelRes = labelRes;
        this.hintRes = hintRes;
        this.amountLabelRes = amountLabelRes;
        this.defaultAmount = defaultAmount;
        this.maxAmount = maxAmount;
        this.needsApp = needsApp;
    }

    public boolean hasAmount() {
        return amountLabelRes != 0;
    }

    public boolean isBrightness() {
        return this == BRIGHTNESS_UP || this == BRIGHTNESS_DOWN;
    }

    public boolean allowsDecimals() {
        return isBrightness();
    }

    public float minAmount() {
        return allowsDecimals() ? 0.1f : 1f;
    }

    public static String formatAmount(float amount) {
        if (amount == Math.rint(amount)) return String.valueOf((int) amount);
        return String.valueOf(Math.round(amount * 100f) / 100f);
    }

    public String label(Context context, float amount) {
        return hasAmount() ? context.getString(labelRes, formatAmount(amount)) : context.getString(labelRes);
    }

    public static Action fromId(String id) {
        for (Action action : values()) {
            if (action.id.equals(id)) {
                return action;
            }
        }
        return NONE;
    }
}
