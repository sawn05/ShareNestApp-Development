
package com.example.sharenestapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sharenestapp.R;
import com.example.sharenestapp.models.Product;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchProductAdapter
        extends RecyclerView.Adapter<SearchProductAdapter.Holder> {

    public interface OnProductClick {
        void onClick(Product product);
    }

    private final List<Product> items = new ArrayList<>();
    private final OnProductClick listener;

    public SearchProductAdapter(OnProductClick listener) {
        this.listener = listener;
    }

    public void submit(List<Product> products) {
        items.clear();
        if (products != null) items.addAll(products);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_product, parent, false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Product p = items.get(position);

        h.name.setText(p.name == null ? "Vật phẩm" : p.name);

        h.status.setText("Còn khả dụng");

        String date = "";
        if (p.createdAt != null) {
            date = new SimpleDateFormat(
                    "dd/MM/yyyy",
                    new Locale("vi", "VN")
            ).format(p.createdAt);
        }

        String address = p.address == null ? "" : p.address;
        h.info.setText(address + (date.isEmpty() ? "" : " · " + date));

        Glide.with(h.itemView.getContext())
                .load(p.coverImageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .centerCrop()
                .into(h.image);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, status, info;

        Holder(View view) {
            super(view);

            image = view.findViewById(R.id.imgSearchProduct);
            name = view.findViewById(R.id.txtSearchProductName);
            status = view.findViewById(R.id.txtSearchProductStatus);
            info = view.findViewById(R.id.txtSearchProductInfo);
        }
    }
}
