package com.rama.suri_zero.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.rama.suri_zero.managers.PrefsManager;
import com.rama.suri_zero.objects.Action;
import com.rama.suri_zero.objects.Gesture;

final class NotchTouchListener implements View.OnTouchListener {
    interface Callback {
        void onGesture(Gesture gesture);
    }

    private static final float SWIPE_DP = 24f;

    private final Callback callback;
    private final PrefsManager prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int touchSlop;
    private final float swipeDistance;
    private final long longPressMs = ViewConfiguration.getLongPressTimeout();
    private final long doubleTapMs = ViewConfiguration.getDoubleTapTimeout();

    private View view;
    private float downX;
    private float downY;
    private boolean moved;
    private boolean longPressed;
    private boolean secondTap;
    private boolean awaitingSecondTap;

    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            longPressed = true;
            awaitingSecondTap = false;
            fire(Gesture.LONG_TOUCH);
        }
    };

    private final Runnable singleTapRunnable = new Runnable() {
        @Override
        public void run() {
            awaitingSecondTap = false;
            fire(Gesture.SINGLE_TAP);
        }
    };

    NotchTouchListener(Context context, Callback callback) {
        this.callback = callback;
        this.prefs = PrefsManager.getInstance(context);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.swipeDistance = SWIPE_DP * context.getResources().getDisplayMetrics().density;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        view = v;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                moved = false;
                longPressed = false;
                secondTap = awaitingSecondTap;
                handler.removeCallbacks(singleTapRunnable);
                handler.postDelayed(longPressRunnable, longPressMs);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!moved && distance(event) > touchSlop) {
                    moved = true;
                    handler.removeCallbacks(longPressRunnable);
                }
                return true;

            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(longPressRunnable);
                if (longPressed) {
                    return true;
                }
                if (moved) {
                    awaitingSecondTap = false;
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;
                    if (Math.abs(dx) >= swipeDistance && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                        fire(dx > 0 ? Gesture.SWIPE_RIGHT : Gesture.SWIPE_LEFT);
                    }
                    return true;
                }
                if (secondTap) {
                    awaitingSecondTap = false;
                    fire(Gesture.DOUBLE_TAP);
                } else if (prefs.getGestureAction(Gesture.DOUBLE_TAP) == Action.NONE) {
                    fire(Gesture.SINGLE_TAP);
                } else {
                    awaitingSecondTap = true;
                    handler.postDelayed(singleTapRunnable, doubleTapMs);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                handler.removeCallbacks(singleTapRunnable);
                awaitingSecondTap = false;
                return true;

            default:
                return true;
        }
    }

    void release() {
        handler.removeCallbacksAndMessages(null);
    }

    private float distance(MotionEvent event) {
        float dx = event.getX() - downX;
        float dy = event.getY() - downY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void fire(Gesture gesture) {
        if (prefs.getGestureAction(gesture) == Action.NONE) return;
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
        callback.onGesture(gesture);
    }
}
