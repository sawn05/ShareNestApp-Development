package com.example.sharecare.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharecare.R;
import com.example.sharecare.adapters.SharenestProductAdapter;
import com.example.sharecare.fragments.MyPostsFragment;
import com.example.sharecare.models.SharenestProduct;
import com.example.sharecare.utils.BottomNavHelper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
public class HomeActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST = 101;
    private static final String TAG = "ShareNestHome";

    private FirebaseFirestore firestore;
    private FusedLocationProviderClient locationClient;
    private ListenerRegistration productListener;

    private RecyclerView nearbyRecycler;
    private RecyclerView latestRecycler;

    private SharenestProductAdapter nearbyAdapter;
    private SharenestProductAdapter latestAdapter;

    private ChipGroup categoryRow;
    private EditText searchInput;
    private TextView locationText;
    private TextView emptyText;
    private TextView nearbySubtitle;
    private ActivityResultLauncher<Intent> locationPickerLauncher;
    private int selectedRadiusKm = 5;
    private final List<SharenestProduct> allProducts = new ArrayList<>();

    private String selectedCategory = "Tất cả";
    private Location currentLocation;
    private boolean userSelectedLocation = false;
    private final String[] categories = {
            "Tất cả",
            "Quần áo",
            "Đồ gia dụng",
            "Sách",
            "Đồ điện tử",
            "Thực phẩm",
            "Nội thất",
            "Khác"

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        firestore = FirebaseFirestore.getInstance();

        locationClient =
                LocationServices.getFusedLocationProviderClient(this);

        initViews();
        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK
                            || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

                    String address = data.getStringExtra("address");
                    double latitude = data.getDoubleExtra(
                            "latitude", Double.NaN
                    );
                    double longitude = data.getDoubleExtra(
                            "longitude", Double.NaN
                    );

                    if (Double.isNaN(latitude) ||
                            Double.isNaN(longitude)) {
                        return;
                    }

                    selectedRadiusKm = data.getIntExtra("radiusKm", 5);

                    Location selectedLocation =
                            new Location("selected");

                    selectedLocation.setLatitude(latitude);
                    selectedLocation.setLongitude(longitude);

                    currentLocation = selectedLocation;
                    userSelectedLocation = true;
                    locationText.setText(
                            address == null ? "Vị trí đã chọn" : address
                    );

                    filterProducts();
                }
        );
        setupRecyclerViews();
        setupCategories();
        setupSearch();
        setupNavigation();

        loadProducts();
        requestLocation();
    }

    // Ánh xạ các thành phần XML
    private void initViews() {

        nearbyRecycler = findViewById(R.id.nearbyRecycler);
        latestRecycler = findViewById(R.id.latestRecycler);

        categoryRow = findViewById(R.id.categoryRow);
        searchInput = findViewById(R.id.searchInput);

        locationText = findViewById(R.id.locationText);
        emptyText = findViewById(R.id.emptyText);
        nearbySubtitle = findViewById(R.id.nearbySubtitle);

        View locationHeader = findViewById(R.id.locationHeader);

        findViewById(R.id.btnAddProduct).setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    AddProductActivity.class
            );
            startActivity(intent);
        });

        if (locationHeader != null) {
            locationHeader.setOnClickListener(v -> {
                Intent intent = new Intent(
                        HomeActivity.this,
                        SelectLocationActivity.class
                );
                locationPickerLauncher.launch(intent);
            });
        }

        if (nearbyRecycler == null || latestRecycler == null ||
                categoryRow == null || searchInput == null ||
                locationText == null || emptyText == null ||
                nearbySubtitle == null) {
            throw new IllegalStateException(
                    "activity_home.xml thiếu ID bắt buộc: nearbyRecycler, " +
                            "latestRecycler, categoryRow, searchInput, locationText, " +
                            "emptyText hoặc nearbySubtitle");
        }
    }

    // Cấu hình danh sách sản phẩm 2 cột
    private void setupRecyclerViews() {

        nearbyRecycler.setLayoutManager(
                new GridLayoutManager(this, 2)
        );

        latestRecycler.setLayoutManager(
                new GridLayoutManager(this, 2)
        );

        nearbyRecycler.setNestedScrollingEnabled(false);
        latestRecycler.setNestedScrollingEnabled(false);

        nearbyAdapter = new SharenestProductAdapter(
                SharenestProduct -> openProductDetail(SharenestProduct)
        );

        latestAdapter = new SharenestProductAdapter(
                SharenestProduct -> openProductDetail(SharenestProduct)
        );

        nearbyRecycler.setAdapter(nearbyAdapter);
        latestRecycler.setAdapter(latestAdapter);
    }

    // Tạo các nút danh mục
    private void setupCategories() {
        categoryRow.removeAllViews();

        for (String categoryName : categories) {
            Chip chip = new Chip(this, null, R.style.Widget_ShareCare_FilterChip);
            chip.setText(categoryName);
            chip.setTextSize(12);
            chip.setChecked(categoryName.equals(selectedCategory));

            chip.setOnClickListener(v -> {
                selectedCategory = categoryName;
                setupCategories();
                filterProducts();
            });

            categoryRow.addView(chip);
        }
    }

    // Tìm kiếm theo tên, danh mục, địa chỉ
    private void setupSearch() {

        searchInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        filterProducts();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    private void setupNavigation() {
        BottomNavHelper.attach(this, BottomNavHelper.TAB_HOME);
        String requestedTab = getIntent().getStringExtra("openTab");
        openTab(requestedTab);
    }
    private void showFragment(Fragment fragment) {

        findViewById(R.id.homeContent)
                .setVisibility(View.GONE);

        findViewById(R.id.fragmentContainer)
                .setVisibility(View.VISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
    private void updateSelectedTab(String selectedTab) {
        BottomNavHelper.highlight(this, selectedTab);
    }
    private void showHomeContent() {

        findViewById(R.id.fragmentContainer)
                .setVisibility(View.GONE);

        findViewById(R.id.homeContent)
                .setVisibility(View.VISIBLE);
    }
    // Đọc dữ liệu thật từ Firestore
    private void loadProducts() {

        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText("Đang tải vật phẩm...");

        productListener = firestore
                .collection("products")
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .addSnapshotListener((snapshot, error) -> {


                    if (error != null) {

                        emptyText.setVisibility(View.VISIBLE);

                        emptyText.setText(
                                "Không thể tải dữ liệu: "
                                        + error.getMessage()
                        );

                        return;
                    }

                    allProducts.clear();

                    if (snapshot != null) {

                        for (DocumentSnapshot document
                                : snapshot.getDocuments()) {

                            try {
                                SharenestProduct SharenestProduct = com.example.sharecare.models.SharenestProduct.from(document);
                                if (SharenestProduct != null) {
                                    allProducts.add(SharenestProduct);
                                }
                            } catch (RuntimeException e) {
                                Log.e(TAG, "Dữ liệu products/" + document.getId()
                                        + " không hợp lệ", e);
                            }
                        }
                    }

                    filterProducts();

                });
    }

    // Lọc theo danh mục và từ khóa
    private void filterProducts() {

        String keyword = normalize(
                searchInput.getText().toString()
        );

        List<SharenestProduct> filtered = new ArrayList<>();

        for (SharenestProduct SharenestProduct : allProducts) {

            if (!selectedCategory.equals("Tất cả")) {

                if (SharenestProduct.category == null ||
                        !selectedCategory.equalsIgnoreCase(
                                SharenestProduct.category
                        )) {
                    continue;
                }
            }

            String name = SharenestProduct.name == null
                    ? "" : SharenestProduct.name;

            String category = SharenestProduct.category == null
                    ? "" : SharenestProduct.category;

            String address = SharenestProduct.address == null
                    ? "" : SharenestProduct.address;

            String searchable = normalize(
                    name + " " + category + " " + address
            );

            if (!keyword.isEmpty() &&
                    !searchable.contains(keyword)) {
                continue;
            }

            calculateDistance(SharenestProduct);

//            if (currentLocation != null) {
//                if (SharenestProduct.distanceKm < 0
//                        || SharenestProduct.distanceKm > selectedRadiusKm) {
//                    continue;
//                }
//            }

            filtered.add(SharenestProduct);
        }

        showProducts(filtered);
    }

    // Tính khoảng cách GPS
    private void calculateDistance(SharenestProduct SharenestProduct) {

        SharenestProduct.distanceKm = -1;

        if (currentLocation == null) {
            return;
        }

        if (SharenestProduct.latitude == 0 ||
                SharenestProduct.longitude == 0) {
            return;
        }

        float[] results = new float[1];

        Location.distanceBetween(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                SharenestProduct.latitude,
                SharenestProduct.longitude,
                results
        );

        SharenestProduct.distanceKm = results[0] / 1000.0;
    }

    // Hiển thị Gần bạn và Mới đăng
    private void showProducts(List<SharenestProduct> filtered) {

        List<SharenestProduct> nearbyProducts =
                new ArrayList<>(filtered);

        if (currentLocation != null) {

            Collections.sort(
                    nearbyProducts,
                    (a, b) -> {

                        double distanceA =
                                a.distanceKm < 0
                                        ? Double.MAX_VALUE
                                        : a.distanceKm;

                        double distanceB =
                                b.distanceKm < 0
                                        ? Double.MAX_VALUE
                                        : b.distanceKm;

                        return Double.compare(
                                distanceA,
                                distanceB
                        );
                    }
            );

            nearbySubtitle.setText(
                    "Theo khoảng cách GPS"
            );

        } else {

            nearbySubtitle.setText(
                    "Bật vị trí để xem vật phẩm gần bạn"
            );
        }

        List<SharenestProduct> nearbyDisplay =
                new ArrayList<>(
                        nearbyProducts.subList(
                                0,
                                Math.min(
                                        4,
                                        nearbyProducts.size()
                                )
                        )
                );

        nearbyAdapter.submit(nearbyDisplay);
        latestAdapter.submit(filtered);

        if (filtered.isEmpty()) {

            emptyText.setVisibility(View.VISIBLE);

            emptyText.setText(
                    "Chưa có vật phẩm phù hợp"
            );

        } else {

            emptyText.setVisibility(View.GONE);
        }
    }

    // Chuẩn hóa tiếng Việt để tìm kiếm không dấu
    private String normalize(String value) {

        String result = Normalizer.normalize(
                value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return result
                .replaceAll("\\p{M}+", "")
                .trim();
    }

    // Yêu cầu quyền truy cập vị trí
    private void requestLocation() {

        boolean finePermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarsePermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!finePermission && !coarsePermission) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST
            );

            return;
        }


        try {
            if (locationClient == null) {
                locationClient =
                        LocationServices.getFusedLocationProviderClient(this);
            }

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                locationText.setText("Chưa cấp quyền vị trí");
                return;
            }

            locationClient.getLastLocation()
                    .addOnSuccessListener(location -> {

                        if (location != null && !userSelectedLocation) {
                            currentLocation = location;
                            locationText.setText("Vị trí hiện tại");

                            // Tính lại khoảng cách của các sản phẩm
                            filterProducts();

                        } else {
                            locationText.setText(
                                    "Chưa xác định được vị trí"
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        locationText.setText("Không lấy được GPS");
                        Toast.makeText(
                                this,
                                "Lỗi GPS: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    });

        } catch (SecurityException e) {
            Toast.makeText(
                    this,
                    "Ứng dụng chưa có quyền truy cập vị trí",
                    Toast.LENGTH_SHORT
            ).show();
        }

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_REQUEST) {

            for (int result : grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    requestLocation();
                    return;
                }
            }

            locationText.setText(
                    "Chưa cấp quyền vị trí"
            );
        }
    }
    private void openTab(String tab) {
        if ("posts".equals(tab)) {
            showFragment(new MyPostsFragment());
            updateSelectedTab("posts");
        } else {
            showHomeContent();
            updateSelectedTab("home");
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openTab(intent.getStringExtra("openTab"));
    }

    private void openProductDetail(SharenestProduct sharenestProduct) {
        Intent intent = new Intent(
                HomeActivity.this,
                SharenestProductDetailActivity.class
        );
        intent.putExtra("productId", sharenestProduct.id);
        startActivity(intent);
    }

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density + 0.5f
        );
    }

    @Override
    protected void onDestroy() {

        if (productListener != null) {
            productListener.remove();
        }

        super.onDestroy();
    }
}
