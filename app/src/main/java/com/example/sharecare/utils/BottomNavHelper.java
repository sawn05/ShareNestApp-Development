package com.example.sharecare.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.sharecare.R;
import com.example.sharecare.activities.AddProductActivity;
import com.example.sharecare.activities.HomeActivity;
import com.example.sharecare.activities.MyRequestsActivity;
import com.example.sharecare.activities.NotificationActivity;
import com.example.sharecare.activities.SettingsActivity;

public final class BottomNavHelper {

    public static final String TAB_HOME = "home";
    public static final String TAB_REQUESTS = "requests";
    public static final String TAB_ADD = "add";
    public static final String TAB_POSTS = "posts";
    public static final String TAB_PROFILE = "profile";

    private BottomNavHelper() {
    }

    public static void attach(Activity activity, String selectedTab) {
        bindClick(activity, R.id.navHome, TAB_HOME);
        bindClick(activity, R.id.navRequests, TAB_REQUESTS);
        bindClick(activity, R.id.navAdd, TAB_ADD);
        bindClick(activity, R.id.navPosts, TAB_POSTS);
        bindClick(activity, R.id.navProfile, TAB_PROFILE);

        View notificationButton = activity.findViewById(R.id.navNotifications);
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> {
                if (activity.getClass() != NotificationActivity.class) {
                    activity.startActivity(new Intent(
                            activity,
                            NotificationActivity.class
                    ));
                }
            });
        }

        NotificationBadgeHelper.bind(activity);
        highlight(activity, selectedTab);
    }

    public static void highlight(Activity activity, String selectedTab) {
        int green = Color.parseColor("#167950");
        int gray = Color.parseColor("#718079");
        int lightGreen = Color.parseColor("#E8F5ED");

        int[] backgrounds = {
                R.id.navHomeBackground,
                R.id.navRequestsTabBackground,
                R.id.navPostsBackground,
                R.id.navProfileBackground
        };
        int[] icons = {
                R.id.iconHome,
                R.id.iconRequestsTab,
                R.id.iconPosts,
                R.id.iconProfile
        };
        int[] labels = {
                R.id.textHome,
                R.id.textRequestsTab,
                R.id.textPosts,
                R.id.textProfile
        };
        String[] tabs = {
                TAB_HOME,
                TAB_REQUESTS,
                TAB_POSTS,
                TAB_PROFILE
        };

        for (int i = 0; i < tabs.length; i++) {
            boolean selected = tabs[i].equals(selectedTab);

            View background = activity.findViewById(backgrounds[i]);
            ImageView icon = activity.findViewById(icons[i]);
            TextView label = activity.findViewById(labels[i]);

            if (background != null) {
                if (selected) {
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setColor(lightGreen);
                    drawable.setCornerRadius(dpToPx(activity, 15));
                    background.setBackground(drawable);
                } else {
                    background.setBackground(null);
                }
            }

            if (icon != null) {
                icon.setColorFilter(selected ? green : gray);
            }

            if (label != null) {
                label.setTextColor(selected ? green : gray);
                label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
    }

    private static void bindClick(Activity activity, int viewId, String tab) {
        View view = activity.findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> navigate(activity, tab));
        }
    }

    private static void navigate(Activity activity, String tab) {
        Intent intent;
        switch (tab) {
            case TAB_REQUESTS:
                intent = new Intent(activity, MyRequestsActivity.class);
                break;
            case TAB_ADD:
                intent = new Intent(activity, AddProductActivity.class);
                break;
            case TAB_POSTS:
                intent = new Intent(activity, HomeActivity.class);
                intent.putExtra("openTab", TAB_POSTS);
                break;
            case TAB_PROFILE:
                intent = new Intent(activity, SettingsActivity.class);
                break;
            default:
                intent = new Intent(activity, HomeActivity.class);
                intent.putExtra("openTab", TAB_HOME);
                break;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    private static float dpToPx(Activity activity, float dp) {
        return dp * activity.getResources().getDisplayMetrics().density;
    }
}
