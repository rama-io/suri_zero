package com.rama.suri_zero.objects;

import com.rama.suri_zero.R;

public enum Gesture {
    SINGLE_TAP("single_tap", R.string.gesture_single_tap, Action.NONE),
    DOUBLE_TAP("double_tap", R.string.gesture_double_tap, Action.NONE),
    LONG_TOUCH("long_touch", R.string.gesture_long_touch, Action.NONE),
    SWIPE_RIGHT("swipe_right", R.string.gesture_swipe_right, Action.NONE),
    SWIPE_LEFT("swipe_left", R.string.gesture_swipe_left, Action.NONE);

    public final String id;
    public final int labelRes;
    public final Action defaultAction;

    Gesture(String id, int labelRes, Action defaultAction) {
        this.id = id;
        this.labelRes = labelRes;
        this.defaultAction = defaultAction;
    }
}
