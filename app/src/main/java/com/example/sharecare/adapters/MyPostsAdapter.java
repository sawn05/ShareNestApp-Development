
package com.example.sharecare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sharecare.R;
import com.example.sharecare.models.SharenestProduct;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MyPostsAdapter extends RecyclerView.Adapter<MyPostsAdapter.Holder> {

    public interface OnAction {
        void onAction(SharenestProduct SharenestProduct, String action);
    }

    private final List<SharenestProduct> items = new ArrayList<>();
    private final OnAction callback;

    public MyPostsAdapter(OnAction callback) {
        this.callback = callback;
    }

    public void submit(List<SharenestProduct> posts) {
        items.clear();

        if (posts != null) {
            items.addAll(posts);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_post, parent, false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder h,
            int position
    ) {

        SharenestProduct p = items.get(position);

        // TÃŠN Váº¬T PHáº¨M
        h.name.setText(
                p.name == null ? "Váº­t pháº©m" : p.name
        );

        // TRáº NG THÃI
        String status = p.status == null
                ? "AVAILABLE"
                : p.status;

        String label;

        switch (status) {
            case "AVAILABLE":
                label = "â— CÃ²n kháº£ dá»¥ng";
                break;

            case "RESERVED":
                label = "â— ÄÃ£ cÃ³ ngÆ°á»i nháº­n";
                break;

            case "COMPLETED":
                label = "â— ÄÃ£ hoÃ n táº¥t";
                break;

            default:
                label = "â— " + status;
                break;
        }

        h.status.setText(label);

        h.status.setTextColor(
                h.itemView.getContext().getColor(
                        "COMPLETED".equals(status)
                                ? R.color.posts_muted
                                : R.color.posts_green
                )
        );

        // NGÃ€Y ÄÄ‚NG
        if (p.createdAt != null) {

            SimpleDateFormat sdf = new SimpleDateFormat(
                    "'ÄÄƒng ngÃ y' dd/MM/yyyy 'lÃºc' HH:mm",
                    new Locale("vi", "VN")
            );

            sdf.setTimeZone(
                    TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            );

            h.date.setText(
                    sdf.format(p.createdAt)
            );

        } else {
            h.date.setText("Vá»«a Ä‘Äƒng");
        }

        // GHI CHÃš
        String note;

        switch (status) {
            case "AVAILABLE":
                note = "Váº­t pháº©m Ä‘ang chá» ngÆ°á»i nháº­n";
                break;

            case "RESERVED":
                note = "ÄÃ£ cÃ³ ngÆ°á»i nháº­n Â· Äang chá» trao";
                break;

            case "COMPLETED":
                note = "ÄÃ£ trao táº·ng váº­t pháº©m";
                break;

            default:
                note = "";
                break;
        }

        h.note.setText(note);

        // áº¢NH Váº¬T PHáº¨M
        Glide.with(h.itemView.getContext())
                .load(p.coverImageUrl)
                .placeholder(R.drawable.posts_image_placeholder)
                .error(R.drawable.posts_image_placeholder)
                .centerCrop()
                .into(h.image);

        // CHá»ˆ CHO PHÃ‰P Sá»¬A/XÃ“A KHI CÃ’N KHáº¢ Dá»¤NG
        boolean editable = "AVAILABLE".equals(status);

        h.btnEditPost.setVisibility(
                editable ? View.VISIBLE : View.GONE
        );

        h.btnDeletePost.setVisibility(
                editable ? View.VISIBLE : View.GONE
        );

        // CHá»ˆNH Sá»¬A BÃ€I ÄÄ‚NG
        h.btnEditPost.setOnClickListener(v -> {
            if (callback != null) {
                callback.onAction(p, "edit");
            }
        });

        // XÃ“A BÃ€I ÄÄ‚NG
        h.btnDeletePost.setOnClickListener(v -> {
            deleteProduct(p, v.getContext());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ======================================
    // XÃ“A BÃ€I ÄÄ‚NG TRÃŠN FIREBASE
    // ======================================

    private void deleteProduct(
            SharenestProduct SharenestProduct,
            Context context
    ) {

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(
                    context,
                    "Báº¡n cáº§n Ä‘Äƒng nháº­p Ä‘á»ƒ xÃ³a bÃ i Ä‘Äƒng",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (SharenestProduct.id == null || SharenestProduct.id.trim().isEmpty()) {
            Toast.makeText(
                    context,
                    "KhÃ´ng tÃ¬m tháº¥y ID bÃ i Ä‘Äƒng",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("XÃ³a bÃ i Ä‘Äƒng")
                .setMessage(
                        "Báº¡n cÃ³ cháº¯c cháº¯n muá»‘n xÃ³a bÃ i Ä‘Äƒng nÃ y khÃ´ng? " +
                                "HÃ nh Ä‘á»™ng nÃ y khÃ´ng thá»ƒ hoÃ n tÃ¡c."
                )
                .setNegativeButton("Há»§y", null)
                .setPositiveButton("XÃ³a", (dialog, which) -> {

                    FirebaseFirestore.getInstance()
                            .collection("products")
                            .document(SharenestProduct.id)
                            .delete()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        context,
                                        "ÄÃ£ xÃ³a bÃ i Ä‘Äƒng trÃªn Firebase",
                                        Toast.LENGTH_SHORT
                                ).show();

                                // Cáº­p nháº­t giao diá»‡n sau khi
                                // Firebase xÃ¡c nháº­n xÃ³a thÃ nh cÃ´ng
                                int index = items.indexOf(SharenestProduct);

                                if (index != -1) {
                                    items.remove(index);
                                    notifyItemRemoved(index);
                                }

                                // ThÃ´ng bÃ¡o cho Fragment náº¿u cáº§n
                                // cáº­p nháº­t sá»‘ lÆ°á»£ng bÃ i Ä‘Äƒng
                                if (callback != null) {
                                    callback.onAction(SharenestProduct, "deleted");
                                }

                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(
                                        context,
                                        "XÃ³a tháº¥t báº¡i: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                            });
                })
                .show();
    }

    // ======================================
    // VIEW HOLDER
    // ======================================

    static class Holder extends RecyclerView.ViewHolder {

        ImageView image;

        TextView name;
        TextView status;
        TextView date;
        TextView note;

        View btnEditPost;
        View btnDeletePost;

        Holder(@NonNull View root) {
            super(root);

            image = root.findViewById(R.id.postImage);
            name = root.findViewById(R.id.postName);
            status = root.findViewById(R.id.postStatus);
            date = root.findViewById(R.id.postDate);
            note = root.findViewById(R.id.postNote);

            btnEditPost = root.findViewById(R.id.btnEditPost);
            btnDeletePost = root.findViewById(R.id.btnDeletePost);
        }
    }
}
