
package com.example.sharecare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharecare.R;
import com.example.sharecare.activities.EditProductActivity;
import com.example.sharecare.adapters.MyPostsAdapter;
import com.example.sharecare.models.SharenestProduct;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MyPostsFragment extends Fragment {

    private FirebaseFirestore db;
    private ListenerRegistration registration;

    private final List<SharenestProduct> allPosts = new ArrayList<>();

    private MyPostsAdapter adapter;

    private TextView tabAvailable;
    private TextView tabReserved;
    private TextView tabCompleted;

    private TextView txtSummary;
    private TextView txtEmpty;

    private View indicatorAvailable;
    private View indicatorReserved;
    private View indicatorCompleted;

    private String selectedStatus = "AVAILABLE";

    public MyPostsFragment() {
        // Constructor rá»—ng
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_my_posts,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Ãnh xáº¡ giao diá»‡n
        tabAvailable = view.findViewById(R.id.tabAvailable);
        tabReserved = view.findViewById(R.id.tabReserved);
        tabCompleted = view.findViewById(R.id.tabCompleted);

        indicatorAvailable = view.findViewById(R.id.indicatorAvailable);
        indicatorReserved = view.findViewById(R.id.indicatorReserved);
        indicatorCompleted = view.findViewById(R.id.indicatorCompleted);

        txtSummary = view.findViewById(R.id.txtSummary);
        txtEmpty = view.findViewById(R.id.txtEmpty);

        RecyclerView recycler = view.findViewById(
                R.id.recyclerMyPosts
        );

        recycler.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        // Giá»¯ nguyÃªn chá»©c nÄƒng Adapter cÅ©
        adapter = new MyPostsAdapter((SharenestProduct, action) -> {

            if ("delete".equals(action)) {
                deleteProduct(SharenestProduct);


            } else if ("edit".equals(action)) {

                Intent intent = new Intent(
                        requireContext(),
                        EditProductActivity.class
                );

                intent.putExtra("productId", SharenestProduct.id);

                startActivity(intent);
            }
        });

        recycler.setAdapter(adapter);

        // Chuyá»ƒn tráº¡ng thÃ¡i bÃ i Ä‘Äƒng
        tabAvailable.setOnClickListener(v -> {
            selectStatus("AVAILABLE");
        });

        tabReserved.setOnClickListener(v -> {
            selectStatus("RESERVED");
        });

        tabCompleted.setOnClickListener(v -> {
            selectStatus("COMPLETED");
        });

        // NÃºt quay láº¡i trang chá»§
        View btnBack = view.findViewById(R.id.btnBack);

        if (btnBack != null) {

            btnBack.setOnClickListener(v -> {
                requireActivity()
                        .findViewById(R.id.navHome)
                        .performClick();
            });
        }

        selectStatus("AVAILABLE");

        listenToMyPosts();
    }

    private void selectStatus(String status) {

        selectedStatus = status;

        int green = requireContext().getColor(
                R.color.posts_green
        );

        int gray = requireContext().getColor(
                R.color.posts_muted
        );

        tabAvailable.setTextColor(
                "AVAILABLE".equals(status) ? green : gray
        );

        tabReserved.setTextColor(
                "RESERVED".equals(status) ? green : gray
        );

        tabCompleted.setTextColor(
                "COMPLETED".equals(status) ? green : gray
        );

        indicatorAvailable.setVisibility(
                "AVAILABLE".equals(status)
                        ? View.VISIBLE
                        : View.INVISIBLE
        );

        indicatorReserved.setVisibility(
                "RESERVED".equals(status)
                        ? View.VISIBLE
                        : View.INVISIBLE
        );

        indicatorCompleted.setVisibility(
                "COMPLETED".equals(status)
                        ? View.VISIBLE
                        : View.INVISIBLE
        );

        render();
    }

    private void listenToMyPosts() {

        FirebaseUser user = FirebaseAuth
                .getInstance()
                .getCurrentUser();

        if (user == null) {

            txtSummary.setText(
                    "Báº¡n cáº§n Ä‘Äƒng nháº­p Ä‘á»ƒ xem bÃ i Ä‘Äƒng"
            );

            txtEmpty.setVisibility(View.VISIBLE);

            txtEmpty.setText(
                    "ChÆ°a cÃ³ tÃ i khoáº£n Ä‘Äƒng nháº­p"
            );

            return;
        }

        String uid = user.getUid();

        // Láº¥y bÃ i Ä‘Äƒng cá»§a tÃ i khoáº£n hiá»‡n táº¡i
        registration = db.collection("products")
                .whereEqualTo("ownerId", uid)
                .addSnapshotListener((snapshot, error) -> {

                    // TrÃ¡nh cáº­p nháº­t UI khi Fragment Ä‘Ã£ bá»‹ há»§y
                    if (getView() == null || !isAdded()) {
                        return;
                    }

                    if (error != null) {

                        txtSummary.setText(
                                "KhÃ´ng táº£i Ä‘Æ°á»£c bÃ i Ä‘Äƒng"
                        );

                        txtEmpty.setVisibility(View.VISIBLE);

                        txtEmpty.setText(
                                "Lá»—i Firestore: " + error.getMessage()
                        );

                        return;
                    }

                    allPosts.clear();

                    if (snapshot != null) {

                        for (DocumentSnapshot doc
                                : snapshot.getDocuments()) {

                            // Bá» bÃ i Ä‘Ã£ xÃ³a
                            if (Boolean.TRUE.equals(
                                    doc.getBoolean("isDeleted")
                            )) {
                                continue;
                            }

                            // Äá»c cáº£ bÃ i COMPLETED
                            SharenestProduct SharenestProduct = readMyPost(doc);

                            allPosts.add(SharenestProduct);
                        }
                    }

                    // BÃ i má»›i nháº¥t lÃªn trÆ°á»›c
                    allPosts.sort((a, b) -> {

                        long timeA = a.createdAt == null
                                ? 0
                                : a.createdAt.getTime();

                        long timeB = b.createdAt == null
                                ? 0
                                : b.createdAt.getTime();

                        return Long.compare(timeB, timeA);
                    });

                    render();
                });
    }

    private SharenestProduct readMyPost(DocumentSnapshot doc) {

        SharenestProduct SharenestProduct = new SharenestProduct();

        SharenestProduct.id = doc.getId();

        SharenestProduct.name = doc.getString("name");

        SharenestProduct.status = doc.getString("status");

        SharenestProduct.coverImageUrl = doc.getString(
                "coverImageUrl"
        );

        // Náº¿u chÆ°a cÃ³ coverImageUrl,
        // láº¥y áº£nh Ä‘áº§u tiÃªn trong imageUrls
        if (SharenestProduct.coverImageUrl == null
                || SharenestProduct.coverImageUrl.trim().isEmpty()) {

            Object images = doc.get("imageUrls");

            if (images instanceof List) {

                for (Object item : (List<?>) images) {

                    if (item instanceof String
                            && !((String) item).trim().isEmpty()) {

                        SharenestProduct.coverImageUrl = (String) item;

                        break;
                    }
                }
            }
        }

        if (doc.getTimestamp("createdAt") != null) {

            SharenestProduct.createdAt = doc
                    .getTimestamp("createdAt")
                    .toDate();
        }

        return SharenestProduct;
    }

    private void render() {

        if (adapter == null || txtSummary == null) {
            return;
        }

        List<SharenestProduct> shown = new ArrayList<>();

        for (SharenestProduct SharenestProduct : allPosts) {

            if (selectedStatus.equals(SharenestProduct.status)) {

                shown.add(SharenestProduct);
            }
        }

        // Cáº­p nháº­t RecyclerView
        adapter.submit(shown);

        txtSummary.setText(
                shown.size()
                        + " bÃ i Ä‘Äƒng Â· Cáº£m Æ¡n báº¡n Ä‘Ã£ sáº» chia"
        );

        txtEmpty.setVisibility(
                shown.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );

        if (shown.isEmpty()) {

            txtEmpty.setText(
                    "ChÆ°a cÃ³ bÃ i Ä‘Äƒng trong má»¥c nÃ y"
            );
        }
    }

    private void  deleteProduct(SharenestProduct SharenestProduct) {

        if (!"AVAILABLE".equals(SharenestProduct.status)) {

            Toast.makeText(
                    requireContext(),
                    "Chá»‰ xÃ³a bÃ i cÃ²n kháº£ dá»¥ng",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("XÃ³a bÃ i Ä‘Äƒng?")
                .setMessage(
                        "BÃ i sáº½ Ä‘Æ°á»£c áº©n khá»i danh sÃ¡ch, "
                                + "khÃ´ng xÃ³a áº£nh trÃªn Cloudinary."
                )
                .setNegativeButton("Há»§y", null)
                .setPositiveButton("XÃ³a", (dialog, which) -> {

                    FirebaseUser user = FirebaseAuth
                            .getInstance()
                            .getCurrentUser();

                    if (user == null) {
                        return;
                    }

                    db.collection("products")
                            .document(SharenestProduct.id)
                            .update("isDeleted", true)
                            .addOnFailureListener(e -> {

                                if (isAdded()) {

                                    Toast.makeText(
                                            requireContext(),
                                            "KhÃ´ng xÃ³a Ä‘Æ°á»£c: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                })
                .show();
    }

    @Override
    public void onDestroyView() {

        if (registration != null) {

            registration.remove();

            registration = null;
        }

        adapter = null;

        super.onDestroyView();
    }
}
