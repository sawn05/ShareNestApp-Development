
package com.example.sharecare.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sharecare.R;
import com.example.sharecare.activities.SharenestProductDetailActivity;
import com.example.sharecare.models.SharenestProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SharenestProductAdapter
        extends RecyclerView.Adapter<SharenestProductAdapter.ViewHolder> {

    private final List<SharenestProduct> productList = new ArrayList<>();
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(SharenestProduct product);
    }

    public SharenestProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<SharenestProduct> products) {
        productList.clear();

        if (products != null) {
            productList.addAll(products);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_product_sharenest, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        SharenestProduct SharenestProduct = productList.get(position);

        // Tên sản phẩm lấy từ Firestore
        holder.productName.setText(
                SharenestProduct.name == null ? "" : SharenestProduct.name
        );

        // Trạng thái sản phẩm
        if ("RESERVED".equals(SharenestProduct.status)) {
            holder.productStatus.setText("● Đã được đặt");
        } else if ("COMPLETED".equals(SharenestProduct.status)) {
            holder.productStatus.setText("● Đã hoàn thành");
        } else {
            holder.productStatus.setText("● Còn khả dụng");
        }

        // Khoảng cách GPS
        String distanceText = "Chưa có vị trí";

        if (SharenestProduct.distanceKm >= 0) {

            if (SharenestProduct.distanceKm < 1) {
                long meters = Math.round(
                        SharenestProduct.distanceKm * 1000
                );

                distanceText = meters + " m";

            } else {
                distanceText = String.format(
                        new Locale("vi", "VN"),
                        "%.1f km",
                        SharenestProduct.distanceKm
                );
            }
        }

        // Thời gian đăng bài
        String timeText = "Chưa có thời gian";

        if (SharenestProduct.createdAt != null) {

            long createdTime = SharenestProduct.createdAt.getTime();
            long now = System.currentTimeMillis();

            long diff = Math.max(0, now - createdTime);

            long minutes = diff / 60000;
            long hours = diff / 3600000;
            long days = diff / 86400000;

            if (minutes < 1) {
                timeText = "Vừa đăng";
            } else if (minutes < 60) {
                timeText = minutes + " phút trước";
            } else if (hours < 24) {
                timeText = hours + " giờ trước";
            } else if (days < 30) {
                timeText = days + " ngày trước";
            } else if (days < 365) {
                timeText = (days / 30) + " tháng trước";
            } else {
                timeText = (days / 365) + " năm trước";
            }
        }

        // Hiển thị khoảng cách và thời gian
        holder.productInfo.setText(
                distanceText + " · " + timeText
        );

        // Lấy ảnh đại diện sản phẩm
        String imageUrl = SharenestProduct.coverImageUrl;

// Kiểm tra URL ảnh
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {

            Glide.with(holder.itemView.getContext())
                    .load(imageUrl.trim())
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(holder.productImage);

        } else {

            Glide.with(holder.itemView.getContext())
                    .clear(holder.productImage);

            holder.productImage.setImageResource(
                    R.drawable.bg_image_placeholder
            );
        }
        // Click xem chi tiết
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    v.getContext(),
                    SharenestProductDetailActivity.class
            );

            intent.putExtra("productId", SharenestProduct.getId());

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView productImage;
        TextView productName;
        TextView productStatus;
        TextView productInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            productImage =
                    itemView.findViewById(R.id.productImage);

            productName =
                    itemView.findViewById(R.id.productName);

            productStatus =
                    itemView.findViewById(R.id.productStatus);

            productInfo =
                    itemView.findViewById(R.id.productInfo);
        }
    }
}
