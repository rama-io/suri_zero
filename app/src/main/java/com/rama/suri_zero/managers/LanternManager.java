package com.rama.suri_zero.managers;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;

public final class LanternManager {
    private static CameraManager cameraManager;
    private static String torchId;
    private static boolean torchOn;
    private static CameraManager.TorchCallback callback;

    private LanternManager() {
    }

    public static synchronized void register(Context context) {
        if (callback != null) return;
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) return;
        torchId = findTorchId(cameraManager);
        if (torchId == null) return;
        callback = new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                if (cameraId.equals(torchId)) {
                    torchOn = enabled;
                }
            }
        };
        cameraManager.registerTorchCallback(callback, new Handler(Looper.getMainLooper()));
    }

    public static synchronized void unregister() {
        if (cameraManager != null && callback != null) {
            cameraManager.unregisterTorchCallback(callback);
        }
        callback = null;
        cameraManager = null;
        torchId = null;
        torchOn = false;
    }

    public static synchronized boolean toggle(Context context) throws CameraAccessException {
        if (callback == null) {
            register(context);
        }
        if (cameraManager == null || torchId == null) return false;
        cameraManager.setTorchMode(torchId, !torchOn);
        return true;
    }

    private static String findTorchId(CameraManager manager) {
        try {
            String fallback = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics c = manager.getCameraCharacteristics(id);
                if (!Boolean.TRUE.equals(c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) continue;
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) return id;
                if (fallback == null) fallback = id;
            }
            return fallback;
        } catch (CameraAccessException e) {
            return null;
        }
    }
}
