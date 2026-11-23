package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sharecare.R;
import com.example.sharecare.utils.AuthSession;
import com.example.sharecare.utils.BottomNavHelper;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvAccountEmail;
    private TextView tvAccountUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        tvAccountEmail = findViewById(R.id.tvAccountEmail);
        tvAccountUid = findViewById(R.id.tvAccountUid);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnBack.setOnClickListener(v -> onBackPressed());

        String email = AuthSession.getSessionEmail(this);
        if (email != null) {
            tvAccountEmail.setText("Email: " + email);
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        tvAccountUid.setText("UID: " + uid);

        BottomNavHelper.attach(this, BottomNavHelper.TAB_PROFILE);

        btnLogout.setOnClickListener(v -> {
            AuthSession.clearSession(SettingsActivity.this);
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(SettingsActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

}
