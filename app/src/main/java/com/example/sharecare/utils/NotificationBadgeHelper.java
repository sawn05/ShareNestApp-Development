package com.example.sharecare.utils;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.sharecare.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public final class NotificationBadgeHelper {

    private NotificationBadgeHelper() {
    }

    public static void bind(Activity activity) {
        TextView badge = activity.findViewById(R.id.notificationBadge);
        if (badge == null) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            update(badge, 0);
            return;
        }

        ListenerRegistration registration = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        update(badge, 0);
                        return;
                    }
                    update(badge, snapshots.size());
                });

        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(
                    (LifecycleEventObserver) (owner, event) -> {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            registration.remove();
                        }
                    }
            );
        }
    }

    private static void update(TextView badge, int count) {
        if (count <= 0) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setText(count > 99 ? "99+" : String.valueOf(count));
        badge.setVisibility(View.VISIBLE);
    }
}
