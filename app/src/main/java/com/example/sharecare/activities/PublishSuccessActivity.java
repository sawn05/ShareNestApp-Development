package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.sharecare.R;
import com.example.sharecare.fragments.MyPostsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PublishSuccessActivity extends AppCompatActivity {

    // Pháº£i giá»‘ng collection trong AddProductActivity.
    private static final String COLLECTION = "products";

    private ImageView btnBack;
    private ImageView imgProduct;
    private ImageView btnMore;

    private TextView txtProductName;
    private TextView txtProductStatus;
    private TextView txtCreatedAt;
    private TextView txtRequestCount;
    private TextView btnGoHome;

    private LinearLayout btnEditProduct;
    private LinearLayout btnDeleteProduct;

    private Button btnViewMyPosts;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private String productId;
    private String productOwnerId;
    private ListenerRegistration productListener;

    private boolean isDeleting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_publish_success);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        productId = getIntent().getStringExtra("productId");

        initViews();
        setupListeners();

        if (productId == null || productId.trim().isEmpty()) {
            showToast("KhÃ´ng tÃ¬m tháº¥y mÃ£ bÃ i Ä‘Äƒng");
            goHome();
            return;
        }

        loadProduct();
    }

    // ===================================
    // 1. ÃNH Xáº  VIEW
    // ===================================

    private void initViews() {

        btnBack = findViewById(R.id.btnBack);
        imgProduct = findViewById(R.id.imgProduct);
        btnMore = findViewById(R.id.btnMore);

        txtProductName = findViewById(R.id.txtProductName);
        txtProductStatus = findViewById(R.id.txtProductStatus);
        txtCreatedAt = findViewById(R.id.txtCreatedAt);
        txtRequestCount = findViewById(R.id.txtRequestCount);

        btnEditProduct = findViewById(R.id.btnEditProduct);
        btnDeleteProduct = findViewById(R.id.btnDeleteProduct);

        btnViewMyPosts = findViewById(R.id.btnViewMyPosts);

        // Trong XML Ä‘Ã£ Ä‘á»•i Button thÃ nh TextView.
        btnGoHome = findViewById(R.id.btnGoHome);
    }

    // ===================================
    // 2. Sá»° KIá»†N CÃC NÃšT
    // ===================================

    private void setupListeners() {

        btnBack.setOnClickListener(v -> goHome());

        btnGoHome.setOnClickListener(v -> goHome());

        btnViewMyPosts.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PublishSuccessActivity.this,
                    HomeActivity.class
            );

            startActivity(intent);

            intent.putExtra("openTab", "requests");

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });

        btnEditProduct.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PublishSuccessActivity.this,
                    EditProductActivity.class
            );

            intent.putExtra(
                    "productId",
                    getIntent().getStringExtra("productId")
            );

            startActivity(intent);
        });
        btnDeleteProduct.setOnClickListener(v ->
                showDeleteDialog()
        );

        btnMore.setOnClickListener(v -> {
            String[] options = {
                    "Chá»‰nh sá»­a",
                    "XÃ³a bÃ i Ä‘Äƒng"
            };

            new AlertDialog.Builder(this)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            btnEditProduct.performClick();
                        } else {
                            showDeleteDialog();
                        }
                    })
                    .show();
        });
    }

    // ===================================
    // 3. Láº¤Y BÃ€I ÄÄ‚NG Tá»ª FIRESTORE
    // ===================================

    private void loadProduct() {

        productListener = firestore
                .collection(COLLECTION)
                .document(productId)
                .addSnapshotListener((document, error) -> {

                    if (error != null) {
                        showToast(
                                "Lá»—i táº£i bÃ i Ä‘Äƒng: "
                                        + error.getMessage()
                        );
                        return;
                    }

                    if (document == null || !document.exists()) {
                        showToast("BÃ i Ä‘Äƒng khÃ´ng tá»“n táº¡i");
                        goHome();
                        return;
                    }

                    Boolean isDeleted =
                            document.getBoolean("isDeleted");

                    if (Boolean.TRUE.equals(isDeleted)) {
                        showToast("BÃ i Ä‘Äƒng Ä‘Ã£ bá»‹ xÃ³a");
                        goHome();
                        return;
                    }

                    displayProduct(document);
                });
    }

    // ===================================
    // 4. HIá»‚N THá»Š THÃ”NG TIN Váº¬T PHáº¨M
    // ===================================

    private void displayProduct(DocumentSnapshot document) {

        String name = document.getString("name");
        String imageUrl = document.getString("coverImageUrl");

// Náº¿u coverImageUrl trá»‘ng, láº¥y áº£nh Ä‘áº§u tiÃªn trong imageUrls
        if (imageUrl == null || imageUrl.trim().isEmpty()) {

            List<String> imageUrls =
                    (List<String>) document.get("imageUrls");

            if (imageUrls != null && !imageUrls.isEmpty()) {
                imageUrl = imageUrls.get(0);
            }
        }
        String status = document.getString("status");

        productOwnerId = document.getString("ownerId");

        txtProductName.setText(
                name == null || name.trim().isEmpty()
                        ? "Váº­t pháº©m"
                        : name
        );

        // Tráº¡ng thÃ¡i bÃ i Ä‘Äƒng.
        if ("AVAILABLE".equals(status)) {
            txtProductStatus.setText("â— CÃ²n kháº£ dá»¥ng");

        } else if ("RESERVED".equals(status)) {
            txtProductStatus.setText("â— ÄÃ£ cÃ³ ngÆ°á»i nháº­n");

        } else if ("COMPLETED".equals(status)) {
            txtProductStatus.setText("â— ÄÃ£ hoÃ n táº¥t");

        } else {
            txtProductStatus.setText("â— KhÃ´ng xÃ¡c Ä‘á»‹nh");
        }

        // Hiá»ƒn thá»‹ áº£nh tá»« Firebase.
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .centerCrop()
                    .into(imgProduct);

        } else {
            imgProduct.setImageResource(
                    R.drawable.placeholder
            );
        }

        Date createdAt = document.getDate("createdAt");

        if (createdAt != null) {

            SimpleDateFormat format = new SimpleDateFormat(
                    "dd/MM/yyyy - HH:mm",
                    new Locale("vi", "VN")
            );

            format.setTimeZone(
                    java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            );

            txtCreatedAt.setText(
                    "ÄÄƒng lÃºc " + format.format(createdAt)
            );

        } else {

            txtCreatedAt.setText("Vá»«a Ä‘Äƒng");
        }


        // Sá»‘ lÆ°á»£t yÃªu cáº§u nháº­n.
        Long requestCount = document.getLong("requestCount");

        if (requestCount == null) {
            requestCount = 0L;
        }

        txtRequestCount.setText(
                requestCount + " yÃªu cáº§u nháº­n"
        );
    }

    // ===================================
    // 5. DIALOG XÃC NHáº¬N XÃ“A
    // ===================================

    private void showDeleteDialog() {

        if (isDeleting) return;

        new AlertDialog.Builder(this)
                .setTitle("XÃ¡c nháº­n xÃ³a bÃ i Ä‘Äƒng")
                .setMessage(
                        "Báº¡n cÃ³ cháº¯c cháº¯n muá»‘n xÃ³a váº­t pháº©m nÃ y?\n\n"
                                + "Sau khi xÃ³a, bÃ i Ä‘Äƒng sáº½ khÃ´ng cÃ²n "
                                + "hiá»ƒn thá»‹ trÃªn trang chá»§."
                )
                .setNegativeButton("Há»§y", (dialog, which) ->
                        dialog.dismiss()
                )
                .setPositiveButton("XÃ³a bÃ i", (dialog, which) ->
                        deleteProduct()
                )
                .show();
    }

    // ===================================
    // 6. XÃ“A Má»€M BÃ€I ÄÄ‚NG
    // ===================================

    private void deleteProduct() {

        if (isDeleting) return;

        if (auth.getCurrentUser() == null) {
            showToast("Báº¡n cáº§n Ä‘Äƒng nháº­p");
            return;
        }

        String currentUid =
                auth.getCurrentUser().getUid();

        if (productOwnerId == null
                || !currentUid.equals(productOwnerId)) {

            showToast("Báº¡n khÃ´ng cÃ³ quyá»n xÃ³a bÃ i nÃ y");
            return;
        }

        isDeleting = true;
        btnDeleteProduct.setEnabled(false);

        firestore.collection(COLLECTION)
                .document(productId)
                .update(
                        "isDeleted", true,
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {

                    showToast("ÄÃ£ xÃ³a bÃ i Ä‘Äƒng thÃ nh cÃ´ng");

                    goHome();
                })
                .addOnFailureListener(e -> {

                    isDeleting = false;
                    btnDeleteProduct.setEnabled(true);

                    showToast(
                            "XÃ³a bÃ i tháº¥t báº¡i: " + e.getMessage()
                    );
                });
    }

    // ===================================
    // 7. Vá»€ TRANG CHá»¦
    // ===================================

    private void goHome() {

        Intent intent = new Intent(
                PublishSuccessActivity.this,
                HomeActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }

    // ===================================
    // 8. THÃ”NG BÃO
    // ===================================

    private void showToast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // ===================================
    // 9. Há»¦Y LISTENER KHI THOÃT
    // ===================================

    @Override
    protected void onDestroy() {

        if (productListener != null) {
            productListener.remove();
            productListener = null;
        }

        super.onDestroy();
    }
}
