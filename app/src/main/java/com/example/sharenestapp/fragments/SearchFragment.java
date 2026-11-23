package com.example.sharenestapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharenestapp.R;
import com.example.sharenestapp.activities.ProductDetailActivity;
import com.example.sharenestapp.activities.SelectLocationActivity;
import com.example.sharenestapp.adapters.SearchProductAdapter;
import com.example.sharenestapp.models.Product;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";
    private EditText edtSearch;
    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private TextView btnNearest, btnNewest, txtSearchLocation;
    private LinearLayout layoutCategories;
    private SearchProductAdapter adapter;

    private final List<Product> allProducts = new ArrayList<>();
    private String selectedCategory = "Tất cả";
    private boolean sortNewest = false;

    private Location selectedLocation;
    private int selectedRadiusKm = 5;
    private String selectedAddress = "Tất cả khu vực";
    private ActivityResultLauncher<Intent> locationPickerLauncher;

    private final List<String> categories = Arrays.asList(
            "Tất cả", "Quần áo", "Đồ gia dụng", "Sách",
            "Đồ dùng học tập", "Thực phẩm", "Khác"
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                    Intent data = result.getData();
                    double lat = data.getDoubleExtra("latitude", Double.NaN);
                    double lon = data.getDoubleExtra("longitude", Double.NaN);
                    if (Double.isNaN(lat) || Double.isNaN(lon)
                            || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                        if (isAdded()) Toast.makeText(requireContext(), "Tọa độ không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Location location = new Location("selected");
                    location.setLatitude(lat);
                    location.setLongitude(lon);
                    selectedLocation = location;
                    selectedRadiusKm = Math.max(1, data.getIntExtra("radiusKm", 5));
                    String address = data.getStringExtra("address");
                    selectedAddress = (address == null || address.trim().isEmpty())
                            ? "Vị trí đã chọn" : address.trim();
                    if (txtSearchLocation != null) txtSearchLocation.setText(selectedAddress);
                    applyFilters();
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.activity_search_fragment,
                container,
                false
        );

        edtSearch = view.findViewById(R.id.edtSearch);
        recyclerView = view.findViewById(R.id.rvSearchProducts);
        txtEmpty = view.findViewById(R.id.txtSearchEmpty);
        btnNearest = view.findViewById(R.id.btnSortNearest);
        btnNewest = view.findViewById(R.id.btnSortNewest);
        layoutCategories = view.findViewById(R.id.layoutCategories);
        txtSearchLocation = view.findViewById(R.id.txtSearchLocation);

        // Hiển thị địa chỉ đã chọn
        txtSearchLocation.setText(selectedAddress);

        // Nút chọn khu vực trên bản đồ
        View btnSearchLocation =
                view.findViewById(R.id.btnSearchLocation);

        btnSearchLocation.setOnClickListener(v -> {

            Intent intent = new Intent(
                    requireContext(),
                    SelectLocationActivity.class
            );

            // Truyền lại vị trí đã chọn trước đó
            if (selectedLocation != null) {

                intent.putExtra(
                        "latitude",
                        selectedLocation.getLatitude()
                );

                intent.putExtra(
                        "longitude",
                        selectedLocation.getLongitude()
                );

                intent.putExtra("address", selectedAddress);
                intent.putExtra("radiusKm", selectedRadiusKm);
            }

            // Mở bản đồ
            locationPickerLauncher.launch(intent);
        });

        // Adapter sản phẩm
        adapter = new SearchProductAdapter(product -> {

            Intent intent = new Intent(
                    requireContext(),
                    ProductDetailActivity.class
            );

            intent.putExtra("productId", product.id);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerView.setAdapter(adapter);

        setupCategories();
        setupSearch();
        setupSort();
        updateSortUI();

        setupOtherButtons(view);

        loadProducts();

        return view;
    }


    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategories() {
        layoutCategories.removeAllViews();
        for (String category : categories) {
            TextView chip = new TextView(requireContext());
            chip.setText(category);
            chip.setTextSize(12);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12), 0, dp(12), 0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
            params.setMargins(0, 0, dp(6), 0);
            chip.setLayoutParams(params);
            boolean selected = category.equals(selectedCategory);
            chip.setBackgroundResource(selected ? R.drawable.bg_filter_selected : R.drawable.bg_filter_normal);
            chip.setTextColor(selected ? 0xFF287C32 : 0xFF737A73);
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                setupCategories();
                applyFilters();
            });
            layoutCategories.addView(chip);
        }
    }

    private void setupSort() {
        btnNearest.setOnClickListener(v -> {
            sortNewest = false;
            updateSortUI();
            applyFilters();
        });
        btnNewest.setOnClickListener(v -> {
            sortNewest = true;
            updateSortUI();
            applyFilters();
        });
    }

    private void updateSortUI() {
        btnNearest.setBackgroundResource(sortNewest ? R.drawable.bg_filter_normal : R.drawable.bg_filter_selected);
        btnNewest.setBackgroundResource(sortNewest ? R.drawable.bg_filter_selected : R.drawable.bg_filter_normal);
        btnNearest.setTextColor(sortNewest ? 0xFF737A73 : 0xFF287C32);
        btnNewest.setTextColor(sortNewest ? 0xFF287C32 : 0xFF737A73);
    }

    private void setupOtherButtons(View view) {
        ImageView btnFilter = view.findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> Toast.makeText(requireContext(),
                    "Chọn danh mục bên dưới để lọc", Toast.LENGTH_SHORT).show());
        }
        View btnLocation = view.findViewById(R.id.btnSearchLocation);
        if (txtSearchLocation != null) txtSearchLocation.setText(selectedAddress);
        if (btnLocation != null) {
            btnLocation.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), SelectLocationActivity.class);
                locationPickerLauncher.launch(intent);
            });
        }
    }

    private void loadProducts() {
        FirebaseFirestore.getInstance().collection("products").get()
                .addOnSuccessListener(query -> {
                    if (!isAdded()) return;
                    allProducts.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        try {
                            if (Boolean.TRUE.equals(doc.getBoolean("isDeleted"))) continue;
                            String status = doc.getString("status");
                            if (!"AVAILABLE".equals(status)) continue;
                            Product product = Product.from(doc);
                            if (product != null) allProducts.add(product);
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Bỏ qua sản phẩm lỗi: " + doc.getId(), e);
                        }
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Không tải được sản phẩm: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void applyFilters() {
        if (edtSearch == null || adapter == null || txtEmpty == null) return;
        String keyword = edtSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            if (p == null) continue;
            String name = p.name == null ? "" : p.name.toLowerCase(Locale.ROOT);
            if (!name.contains(keyword)) continue;
            if (!"Tất cả".equals(selectedCategory)
                    && (p.category == null || !p.category.equalsIgnoreCase(selectedCategory))) continue;

            p.distanceKm = -1;
            if (selectedLocation != null) {
                if (p.latitude < -90 || p.latitude > 90 || p.longitude < -180 || p.longitude > 180
                        || (p.latitude == 0 && p.longitude == 0)) continue;
                float[] results = new float[1];
                Location.distanceBetween(selectedLocation.getLatitude(), selectedLocation.getLongitude(),
                        p.latitude, p.longitude, results);
                p.distanceKm = results[0] / 1000.0;
                if (p.distanceKm > selectedRadiusKm) continue;
            }
            filtered.add(p);
        }

        if (sortNewest) {
            filtered.sort((a, b) -> {
                if (a.createdAt == null && b.createdAt == null) return 0;
                if (a.createdAt == null) return 1;
                if (b.createdAt == null) return -1;
                return b.createdAt.compareTo(a.createdAt);
            });
        } else {
            filtered.sort((a, b) -> {
                boolean aKnown = a.distanceKm >= 0;
                boolean bKnown = b.distanceKm >= 0;
                if (aKnown && bKnown) return Double.compare(a.distanceKm, b.distanceKm);
                if (aKnown) return -1;
                if (bKnown) return 1;
                return 0;
            });
        }
        adapter.submit(filtered);
        txtEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        edtSearch = null;
        recyclerView = null;
        txtEmpty = null;
        btnNearest = null;
        btnNewest = null;
        txtSearchLocation = null;
        layoutCategories = null;
        adapter = null;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
