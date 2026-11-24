package com.example.sharecare.utils;

import android.widget.EditText;
import android.widget.ImageButton;

public final class PasswordVisibilityHelper {

    private PasswordVisibilityHelper() {
    }

    public static void attach(EditText field, ImageButton toggle) {
        toggle.setTag(Boolean.FALSE);
        apply(field, toggle, false);

        toggle.setOnClickListener(v -> {
            boolean visible = Boolean.TRUE.equals(toggle.getTag());
            boolean nextVisible = PasswordToggleState.nextVisible(visible);
            toggle.setTag(nextVisible);
            apply(field, toggle, nextVisible);
            field.setSelection(field.length());
        });
    }

    private static void apply(EditText field, ImageButton toggle, boolean visible) {
        field.setInputType(PasswordToggleState.inputTypeFor(visible));
        toggle.setImageResource(PasswordToggleState.iconFor(visible));
        toggle.setContentDescription(PasswordToggleState.descriptionFor(visible));
    }
}
