package com.example.sharecare.utils;

import android.text.InputType;

import com.example.sharecare.R;

public final class PasswordToggleState {

    private PasswordToggleState() {
    }

    public static boolean nextVisible(boolean visible) {
        return !visible;
    }

    public static int inputTypeFor(boolean visible) {
        if (visible) {
            return InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        }
        return InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    }

    public static int iconFor(boolean visible) {
        return visible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off;
    }

    public static String descriptionFor(boolean visible) {
        return visible ? "Ẩn mật khẩu" : "Hiện mật khẩu";
    }
}
