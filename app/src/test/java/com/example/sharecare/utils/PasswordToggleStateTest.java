package com.example.sharecare.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.text.InputType;

import com.example.sharecare.R;

import org.junit.Test;

public class PasswordToggleStateTest {

    @Test
    public void nextVisibleState_flipsFromHiddenToVisible() {
        assertTrue(PasswordToggleState.nextVisible(false));
    }

    @Test
    public void nextVisibleState_flipsFromVisibleToHidden() {
        assertFalse(PasswordToggleState.nextVisible(true));
    }

    @Test
    public void inputTypeForState_usesVisibleVariationWhenVisible() {
        assertEquals(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                PasswordToggleState.inputTypeFor(true)
        );
    }

    @Test
    public void inputTypeForState_usesPasswordVariationWhenHidden() {
        assertEquals(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                PasswordToggleState.inputTypeFor(false)
        );
    }

    @Test
    public void iconForState_usesOpenEyeWhenVisible() {
        assertEquals(R.drawable.ic_visibility, PasswordToggleState.iconFor(true));
    }

    @Test
    public void iconForState_usesSlashEyeWhenHidden() {
        assertEquals(
                R.drawable.ic_visibility_off,
                PasswordToggleState.iconFor(false)
        );
    }

    @Test
    public void descriptionForState_tellsUserToHideWhenVisible() {
        assertEquals("Ẩn mật khẩu", PasswordToggleState.descriptionFor(true));
    }

    @Test
    public void descriptionForState_tellsUserToShowWhenHidden() {
        assertEquals("Hiện mật khẩu", PasswordToggleState.descriptionFor(false));
    }
}
