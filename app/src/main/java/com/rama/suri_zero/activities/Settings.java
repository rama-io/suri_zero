package com.rama.suri_zero.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rama.suri_zero.R;
import com.rama.suri_zero.dialogs.GestureDialog;
import com.rama.suri_zero.helpers.SystemBars;
import com.rama.suri_zero.managers.ActionManager;
import com.rama.suri_zero.managers.FontManager;
import com.rama.suri_zero.managers.PrefsManager;
import com.rama.suri_zero.managers.ThemeManager;
import com.rama.suri_zero.managers.ZoomManager;
import com.rama.suri_zero.objects.Gesture;
import com.rama.suri_zero.objects.PrefTheme;
import com.rama.suri_zero.objects.Themes;
import com.rama.suri_zero.services.SuriService;
import com.rama.suri_zero.widgets.WdRadio;
import com.rama.suri_zero.widgets.WdRadioGroup;

import java.util.List;

public class Settings extends BaseActivity {
    private static final int[] GESTURE_BUTTON_IDS = {
            R.id.gesture_single_tap,
            R.id.gesture_double_tap,
            R.id.gesture_long_touch,
            R.id.gesture_swipe_right,
            R.id.gesture_swipe_left
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        View root = findViewById(R.id.root);
        SystemBars.applyInsets(root);
        FontManager.apply(root, FontManager.getJersey25(this));
        if (savedInstanceState == null) {
            ThemeManager.rollIfRandom(this);
        }
        setupSystemSection();
        setupZoomSection();
        setupGestureSection();
        setupAppearanceSection();
        ThemeManager.applyTheme(this, root);
        findViewById(R.id.go_about).setOnClickListener(v -> startActivity(new Intent(Settings.this, About.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshServiceStatus();
        refreshWriteSettingsStatus();
        refreshGestureButtons();
    }

    private void setupSystemSection() {
        findViewById(R.id.activate_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(Settings.this, R.string.toast_unable_open_settings, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.allow_modify_system_settings).setOnClickListener(v -> ActionManager.requestWriteSettings(Settings.this));
    }

    private void refreshWriteSettingsStatus() {
        TextView status = findViewById(R.id.write_settings_status);
        if (ActionManager.canWriteSettings(this)) {
            status.setText(R.string.status_write_settings_on);
            status.setTag("success");
        } else {
            status.setText(R.string.status_write_settings_off);
            status.setTag("warning");
        }
        ThemeManager.applyTheme(this, status);
    }

    private void refreshServiceStatus() {
        TextView status = findViewById(R.id.service_status);
        if (!SuriService.isEnabled(this)) {
            status.setText(R.string.status_service_off);
            status.setTag("warning");
        } else if (SuriService.notchState == SuriService.NOTCH_MISSING) {
            status.setText(R.string.status_service_no_cutout);
            status.setTag("error");
        } else {
            status.setText(R.string.status_service_on);
            status.setTag("success");
        }
        ThemeManager.applyTheme(this, status);
    }

    private void setupGestureSection() {
        Gesture[] gestures = Gesture.values();
        for (int i = 0; i < gestures.length; i++) {
            final Gesture gesture = gestures[i];
            findViewById(GESTURE_BUTTON_IDS[i]).setOnClickListener(v -> GestureDialog.show(Settings.this, gesture, this::refreshGestureButtons));
        }
    }

    private void refreshGestureButtons() {
        Gesture[] gestures = Gesture.values();
        for (int i = 0; i < gestures.length; i++) {
            Button button = findViewById(GESTURE_BUTTON_IDS[i]);
            button.setText(getString(R.string.btn_gesture, getString(gestures[i].labelRes), ActionManager.describe(this, gestures[i])));
        }
    }

    private void setupZoomSection() {
        final EditText zoom = findViewById(R.id.zoom);
        zoom.setText(String.valueOf(ZoomManager.getPercent(this)));
        zoom.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyZoom();
            }
            return false;
        });
        zoom.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                applyZoom();
            }
        });
    }

    private void applyZoom() {
        saveZoomFromField();
        ZoomManager.apply(this, findViewById(R.id.root));
    }

    private void saveZoomFromField() {
        EditText zoom = findViewById(R.id.zoom);
        int percent;
        try {
            percent = ZoomManager.clamp(Integer.parseInt(zoom.getText().toString().trim()));
        } catch (NumberFormatException e) {
            percent = ZoomManager.getPercent(this);
        }
        String normalized = String.valueOf(percent);
        if (!normalized.equals(zoom.getText().toString())) {
            zoom.setText(normalized);
        }
        PrefsManager.getInstance(this).setZoomPercent(percent);
    }

    @Override
    protected void onPause() {
        saveZoomFromField();
        super.onPause();
    }

    private void setupAppearanceSection() {
        final PrefsManager prefs = PrefsManager.getInstance(this);
        WdRadioGroup themeGroup = findViewById(R.id.theme_group);
        boolean randomMode = PrefTheme.CATPPUCCIN_MOCHA_RANDOM.equals(prefs.getTheme());
        String currentTheme = ThemeManager.currentPalette(this).id;
        WdRadio randomRadio = new WdRadio(this);
        randomRadio.setId(999);
        randomRadio.setText("Catppuccin Mocha (Random)");
        randomRadio.setTextColor(getResources().getColor(R.color.text));
        randomRadio.setChecked(randomMode);
        themeGroup.addView(randomRadio);
        FontManager.apply(randomRadio, FontManager.getJersey25(this));
        randomRadio.setOnClickListener(v -> {
            prefs.setTheme(PrefTheme.CATPPUCCIN_MOCHA_RANDOM);
            ThemeManager.roll(Settings.this);
            ThemeManager.applyTheme(Settings.this, findViewById(R.id.root));
        });
        List<Themes.Palette> palettes = Themes.all();
        for (int i = 0; i < palettes.size(); i++) {
            final Themes.Palette palette = palettes.get(i);
            WdRadio radio = new WdRadio(this);
            radio.setId(1000 + i);
            radio.setText(palette.label);
            radio.setTextColor(getResources().getColor(R.color.text));
            radio.setChecked(!randomMode && palette.id.equals(currentTheme));
            themeGroup.addView(radio);
            FontManager.apply(radio, FontManager.getJersey25(this));
            radio.setOnClickListener(v -> {
                prefs.setTheme(palette.id);
                ThemeManager.applyTheme(Settings.this, findViewById(R.id.root));
            });
        }
    }
}
