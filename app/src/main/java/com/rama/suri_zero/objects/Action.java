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
    BRIGHTNESS_UP("brightness_up", R.string.action_brightness_up, R.string.hint_brightness, R.string.label_amount_percent, 1, 100),
    BRIGHTNESS_DOWN("brightness_down", R.string.action_brightness_down, R.string.hint_brightness, R.string.label_amount_percent, 1, 100),
    VOLUME_UP("volume_up", R.string.action_volume_up, R.string.hint_volume_steps, R.string.label_amount_steps, 1, 30),
    VOLUME_DOWN("volume_down", R.string.action_volume_down, R.string.hint_volume_steps, R.string.label_amount_steps, 1, 30);

    public final String id;
    public final int labelRes;
    public final int hintRes;
    public final int amountLabelRes;
    public final int defaultAmount;
    public final int maxAmount;
    public final boolean needsApp;

    Action(String id, int labelRes) {
        this(id, labelRes, 0, 0, 0, 0, false);
    }

    Action(String id, int labelRes, boolean needsApp) {
        this(id, labelRes, 0, 0, 0, 0, needsApp);
    }

    Action(String id, int labelRes, int hintRes, int amountLabelRes, int defaultAmount, int maxAmount) {
        this(id, labelRes, hintRes, amountLabelRes, defaultAmount, maxAmount, false);
    }

    Action(String id, int labelRes, int hintRes, int amountLabelRes, int defaultAmount, int maxAmount, boolean needsApp) {
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

    public String label(Context context, int amount) {
        return hasAmount() ? context.getString(labelRes, amount) : context.getString(labelRes);
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
