package com.rama.suri_zero.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rama.suri_zero.R;
import com.rama.suri_zero.managers.ActionManager;
import com.rama.suri_zero.managers.FontManager;
import com.rama.suri_zero.managers.PrefsManager;
import com.rama.suri_zero.managers.ThemeManager;
import com.rama.suri_zero.managers.ZoomManager;
import com.rama.suri_zero.objects.Action;
import com.rama.suri_zero.objects.Gesture;
import com.rama.suri_zero.widgets.WdRadio;
import com.rama.suri_zero.widgets.WdRadioGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GestureDialog {
    public interface OnSaved {
        void onSaved();
    }

    private static final class AppEntry {
        final String packageName;
        final String label;

        AppEntry(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }
    }

    private final Activity activity;
    private final Gesture gesture;
    private final OnSaved onSaved;
    private final Dialog dialog;

    private final WdRadioGroup actionGroup;
    private final WdRadioGroup appGroup;
    private final View amountLayout;
    private final View appLayout;
    private final TextView amountLabel;
    private final TextView hint;
    private final EditText amountField;

    private final List<Action> actions = new ArrayList<>();
    private final List<WdRadio> actionRadios = new ArrayList<>();
    private final List<String> appPackages = new ArrayList<>();

    private String selectedApp;
    private boolean appsRequested;
    private boolean amountEdited;
    private boolean settingAmount;

    public static void show(Activity activity, Gesture gesture, OnSaved onSaved) {
        new GestureDialog(activity, gesture, onSaved).dialog.show();
    }

    private GestureDialog(Activity activity, Gesture gesture, OnSaved onSaved) {
        this.activity = activity;
        this.gesture = gesture;
        this.onSaved = onSaved;

        dialog = new Dialog(activity, R.style.AppDialog);
        dialog.setContentView(R.layout.dialog_gesture);
        int width = Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.92f);
        dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        actionGroup = dialog.findViewById(R.id.action_group);
        appGroup = dialog.findViewById(R.id.app_group);
        amountLayout = dialog.findViewById(R.id.amount_layout);
        appLayout = dialog.findViewById(R.id.app_layout);
        amountLabel = dialog.findViewById(R.id.amount_label);
        hint = dialog.findViewById(R.id.action_hint);
        amountField = dialog.findViewById(R.id.amount);

        ((TextView) dialog.findViewById(R.id.dialog_title)).setText(gesture.labelRes);

        PrefsManager prefs = PrefsManager.getInstance(activity);
        Action current = prefs.getGestureAction(gesture);
        selectedApp = prefs.getGestureApp(gesture);
        amountEdited = current.hasAmount();
        // Empty until an amount action is chosen, so each "+{amount} ..." row shows its own default meanwhile.
        setAmountText(current.hasAmount() ? Action.formatAmount(prefs.getGestureAmount(gesture, current)) : "");

        buildActionList(current);
        appGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int index = group.getCheckedIndex();
            if (index >= 0 && index < appPackages.size()) {
                selectedApp = appPackages.get(index);
            }
        });
        amountField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!settingAmount) {
                    amountEdited = true;
                }
                updateAmountLabels();
            }
        });
        dialog.findViewById(R.id.save_button).setOnClickListener(v -> save());

        styleTree(dialog.findViewById(R.id.dialog_root));
        onActionChanged();
    }

    private void buildActionList(Action current) {
        for (Action action : Action.values()) {
            actions.add(action);
            WdRadio radio = actionGroup.addOption(action.label(activity, currentAmountOr(action.defaultAmount)));
            actionRadios.add(radio);
            if (action == current) {
                actionGroup.check(radio.getId());
            }
        }
        actionGroup.setOnCheckedChangeListener((group, checkedId) -> onActionChanged());
    }

    private Action selectedAction() {
        int index = actionGroup.getCheckedIndex();
        return index < 0 ? Action.NONE : actions.get(index);
    }

    private void onActionChanged() {
        Action action = selectedAction();

        amountLayout.setVisibility(action.hasAmount() ? View.VISIBLE : View.GONE);
        if (action.hasAmount()) {
            amountLabel.setText(action.amountLabelRes);
            amountField.setInputType(action.allowsDecimals()
                    ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    : InputType.TYPE_CLASS_NUMBER);
            if (!amountEdited) {
                setAmountText(Action.formatAmount(action.defaultAmount));
            }
        }

        appLayout.setVisibility(action.needsApp ? View.VISIBLE : View.GONE);
        if (action.needsApp) {
            loadApps();
        }

        hint.setVisibility(action.hintRes != 0 ? View.VISIBLE : View.GONE);
        if (action.hintRes != 0) {
            hint.setText(action.hintRes);
        }
    }

    private float currentAmountOr(float fallback) {
        try {
            return Float.parseFloat(amountField.getText().toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void setAmountText(String text) {
        settingAmount = true;
        amountField.setText(text);
        settingAmount = false;
    }

    private void updateAmountLabels() {
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            if (action.hasAmount()) {
                actionRadios.get(i).setText(action.label(activity, currentAmountOr(action.defaultAmount)));
            }
        }
    }

    private void loadApps() {
        if (appsRequested) return;
        appsRequested = true;
        final PackageManager pm = activity.getPackageManager();
        new Thread(() -> {
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            Set<String> seen = new HashSet<>();
            final List<AppEntry> entries = new ArrayList<>();
            for (ResolveInfo info : pm.queryIntentActivities(main, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(activity.getPackageName()) || !seen.add(pkg)) continue;
                entries.add(new AppEntry(pkg, FontManager.sanitizeForFont(String.valueOf(info.loadLabel(pm)))));
            }
            Collections.sort(entries, (a, b) -> a.label.compareToIgnoreCase(b.label));
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || !dialog.isShowing()) return;
                populateApps(entries);
            });
        }).start();
    }

    private void populateApps(List<AppEntry> entries) {
        for (AppEntry entry : entries) {
            WdRadio radio = appGroup.addOption(entry.label);
            appPackages.add(entry.packageName);
            if (entry.packageName.equals(selectedApp)) {
                appGroup.check(radio.getId());
            }
        }
        styleTree(appLayout);
    }

    private void save() {
        Action action = selectedAction();

        float amount = 0f;
        if (action.hasAmount()) {
            amount = currentAmountOr(0f);
            boolean wholeOnly = !action.allowsDecimals() && amount != Math.rint(amount);
            if (amount < action.minAmount() || amount > action.maxAmount || wholeOnly) {
                Toast.makeText(activity, activity.getString(R.string.toast_amount_invalid,
                        Action.formatAmount(action.minAmount()), Action.formatAmount(action.maxAmount)), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String appPackage = null;
        if (action.needsApp) {
            if (selectedApp == null) {
                Toast.makeText(activity, R.string.toast_pick_app, Toast.LENGTH_SHORT).show();
                return;
            }
            appPackage = selectedApp;
        }

        PrefsManager.getInstance(activity).setGesture(gesture, action, amount, appPackage);
        if (action.isBrightness() && !ActionManager.canWriteSettings(activity)) {
            Toast.makeText(activity, R.string.toast_need_write_settings, Toast.LENGTH_SHORT).show();
            ActionManager.requestWriteSettings(activity);
        }
        dialog.dismiss();
        onSaved.onSaved();
    }

    private void styleTree(View root) {
        FontManager.apply(root, FontManager.getJersey25(activity));
        ThemeManager.applyTheme(dialog.getContext(), root);
        ZoomManager.apply(activity, root);
    }
}
