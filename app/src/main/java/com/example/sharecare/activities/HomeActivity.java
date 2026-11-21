package com.example.sharecare.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
import androidx.fragment.app.Fragment;
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

    private LinearLayout categoryRow;
    private EditText searchInput;
    private TextView locationText;
    private TextView emptyText;
    private TextView nearbySubtitle;
    private ActivityResultLauncher<Intent> locationPickerLauncher;
    private int selectedRadiusKm = 5;
    private final List<SharenestProduct> allProducts = new ArrayList<>();

    private String selectedCategory = "Táº¥t cáº£";
    private Location currentLocation;
    private boolean userSelectedLocation = false;
    private final String[] categories = {
            "Táº¥t cáº£",
            "Quáº§n Ã¡o",
            "Äá»“ gia dá»¥ng",
            "SÃ¡ch",
            "Äá»“ Ä‘iá»‡n tá»­",
            "Thá»±c pháº©m",
            "KhÃ¡c",
            "Ná»™i tháº¥t"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK
                            || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

                    double latitude = data.getDoubleExtra(
                            "latitude", Double.NaN
                    );
                    double longitude = data.getDoubleExtra(
                            "longitude", Double.NaN
                    );

                    if (Double.isNaN(latitude)
                            || Double.isNaN(longitude)) {
                        return;
                    }

                    selectedRadiusKm = data.getIntExtra("radiusKm", 5);

                    Location selectedLocation = new Location("selected");
                    selectedLocation.setLatitude(latitude);
                    selectedLocation.setLongitude(longitude);

                    currentLocation = selectedLocation;

                    String address = data.getStringExtra("address");

                    locationText.setText(
                            address == null || address.isEmpty()
                                    ? "Vá»‹ trÃ­ Ä‘Ã£ chá»n"
                                    : address
                    );

                    filterProducts();
                }
        );

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

                    Location selectedLocation =
                            new Location("selected");

                    selectedLocation.setLatitude(latitude);
                    selectedLocation.setLongitude(longitude);

                    currentLocation = selectedLocation;
                    userSelectedLocation = true;
                    locationText.setText(
                            address == null ? "Vá»‹ trÃ­ Ä‘Ã£ chá»n" : address
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

    // Ãnh xáº¡ cÃ¡c thÃ nh pháº§n XML
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
                locationText == null || emptyText == null || nearbySubtitle == null) {
            throw new IllegalStateException(
                    "activity_home.xml thiáº¿u ID báº¯t buá»™c: nearbyRecycler, " +
                            "latestRecycler, categoryRow, searchInput, locationText, " +
                            "emptyText hoáº·c nearbySubtitle");
        }
    }

    // Cáº¥u hÃ¬nh danh sÃ¡ch sáº£n pháº©m 2 cá»™t
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

    // Táº¡o cÃ¡c nÃºt danh má»¥c
    private void setupCategories() {
        categoryRow.removeAllViews();

        for (String categoryName : categories) {

            TextView chip = new TextView(this);

            chip.setText(categoryName);
            chip.setTextSize(12);

            chip.setPadding(
                    dp(14),
                    dp(9),
                    dp(14),
                    dp(9)
            );

            boolean selected =
                    categoryName.equals(selectedCategory);

            if (selected) {
                chip.setBackgroundResource(
                        R.drawable.bg_chip_selected
                );

                chip.setTextColor(
                        ContextCompat.getColor(
                                this,
                                android.R.color.white
                        )
                );

            } else {

                chip.setBackgroundResource(
                        R.drawable.bg_chip
                );

                chip.setTextColor(
                        ContextCompat.getColor(
                                this,
                                R.color.ink
                        )
                );
            }

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(0, 0, dp(8), 0);

            categoryRow.addView(chip, params);

            chip.setOnClickListener(v -> {
                selectedCategory = categoryName;
                setupCategories();
                filterProducts();
            });
        }
    }

    // TÃ¬m kiáº¿m theo tÃªn, danh má»¥c, Ä‘á»‹a chá»‰
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

    // CÃ¡c nÃºt Ä‘iá»u hÆ°á»›ng
    private void setupNavigation() {
        String requestedTab = getIntent().getStringExtra("openTab");
        openTab(requestedTab);
        findViewById(R.id.navHome).setOnClickListener(v -> {

            showHomeContent();

            updateSelectedTab("home");
        });

        // TÃŒM KIáº¾M
        findViewById(R.id.navSearch).setOnClickListener(v -> {
            // ChÆ°a cÃ³ SearchFragment
            // Giá»¯ chá»©c nÄƒng tÃ¬m kiáº¿m hiá»‡n táº¡i á»Ÿ Home
            updateSelectedTab("search");
        });

        // ÄÄ‚NG BÃ€I
        findViewById(R.id.navAdd).setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    AddProductActivity.class
            );
            startActivity(intent);
        });

        findViewById(R.id.navMessages).setOnClickListener(v -> {

            showFragment(new MyPostsFragment());

            updateSelectedTab("requests");
        });

        // CÃ NHÃ‚N
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            updateSelectedTab("profile");
        });
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

        int green = Color.parseColor("#167950");
        int gray = Color.parseColor("#718079");
        int lightGreen = Color.parseColor("#E8F5ED");

        int[] backgrounds = {
                R.id.navHomeBackground,
                R.id.navSearchBackground,
                R.id.navRequestsBackground,
                R.id.navProfileBackground
        };

        int[] icons = {
                R.id.iconHome,
                R.id.iconSearch,
                R.id.iconRequests,
                R.id.iconProfile
        };

        int[] labels = {
                R.id.textHome,
                R.id.textSearch,
                R.id.textRequests,
                R.id.textProfile
        };

        String[] tabs = {
                "home",
                "search",
                "requests",
                "profile"
        };

        for (int i = 0; i < tabs.length; i++) {

            boolean selected = tabs[i].equals(selectedTab);

            View background = findViewById(backgrounds[i]);
            ImageView icon = findViewById(icons[i]);
            TextView label = findViewById(labels[i]);

            if (background != null) {

                if (selected) {

                    GradientDrawable bg = new GradientDrawable();

                    bg.setColor(lightGreen);
                    bg.setCornerRadius(dpToPx(15));

                    background.setBackground(bg);

                } else {

                    background.setBackground(null);
                }
            }

            if (icon != null) {
                icon.setColorFilter(selected ? green : gray);
            }

            if (label != null) {

                label.setTextColor(selected ? green : gray);

                label.setTypeface(
                        null,
                        selected ? Typeface.BOLD : Typeface.NORMAL
                );
            }
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void updateMenuChildren(View view, int color) {

        if (view instanceof ImageView) {

            ImageView imageView = (ImageView) view;
            imageView.setColorFilter(color);

        } else if (view instanceof TextView) {

            TextView textView = (TextView) view;
            textView.setTextColor(color);

        } else if (view instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {

                updateMenuChildren(
                        group.getChildAt(i),
                        color
                );
            }
        }
    }
    private void findOptionalView(int viewId, View.OnClickListener clickListener) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(clickListener);
        }
    }
    private void showHomeContent() {

        findViewById(R.id.fragmentContainer)
                .setVisibility(View.GONE);

        findViewById(R.id.homeContent)
                .setVisibility(View.VISIBLE);
    }
    // Äá»c dá»¯ liá»‡u tháº­t tá»« Firestore
    private void loadProducts() {

        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText("Äang táº£i váº­t pháº©m...");

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
                                "KhÃ´ng thá»ƒ táº£i dá»¯ liá»‡u: "
                                        + error.getMessage()
                        );

                        return;
                    }

                    allProducts.clear();

                    if (snapshot != null) {

                        for (DocumentSnapshot document
                                : snapshot.getDocuments()) {

                            try {
                                SharenestProduct SharenestProduct = SharenestProduct.from(document);
                                if (SharenestProduct != null) {
                                    allProducts.add(SharenestProduct);
                                }
                            } catch (RuntimeException e) {
                                Log.e(TAG, "Dá»¯ liá»‡u products/" + document.getId()
                                        + " khÃ´ng há»£p lá»‡", e);
                            }
                        }
                    }

                    filterProducts();

                });
    }

    // Lá»c theo danh má»¥c vÃ  tá»« khÃ³a
    private void filterProducts() {

        String keyword = normalize(
                searchInput.getText().toString()
        );

        List<SharenestProduct> filtered = new ArrayList<>();

        for (SharenestProduct SharenestProduct : allProducts) {

            if (!selectedCategory.equals("Táº¥t cáº£")) {

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

    // TÃ­nh khoáº£ng cÃ¡ch GPS
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

    // Hiá»ƒn thá»‹ Gáº§n báº¡n vÃ  Má»›i Ä‘Äƒng
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
                    "Theo khoáº£ng cÃ¡ch GPS"
            );

        } else {

            nearbySubtitle.setText(
                    "Báº­t vá»‹ trÃ­ Ä‘á»ƒ xem váº­t pháº©m gáº§n báº¡n"
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

        List<SharenestProduct> latestDisplay =
                new ArrayList<>(
                        filtered.subList(
                                0,
                                Math.min(
                                        12,
                                        filtered.size()
                                )
                        )
                );

        nearbyAdapter.submit(nearbyDisplay);
        latestAdapter.submit(latestDisplay);

        if (filtered.isEmpty()) {

            emptyText.setVisibility(View.VISIBLE);

            emptyText.setText(
                    "ChÆ°a cÃ³ váº­t pháº©m phÃ¹ há»£p"
            );

        } else {

            emptyText.setVisibility(View.GONE);
        }
    }

    // Chuáº©n hÃ³a tiáº¿ng Viá»‡t Ä‘á»ƒ tÃ¬m kiáº¿m khÃ´ng dáº¥u
    private String normalize(String value) {

        String result = Normalizer.normalize(
                value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return result
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .trim();
    }

    // YÃªu cáº§u quyá»n truy cáº­p vá»‹ trÃ­
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
                locationText.setText("ChÆ°a cáº¥p quyá»n vá»‹ trÃ­");
                return;
            }

            locationClient.getLastLocation()
                    .addOnSuccessListener(location -> {

                        if (location != null && !userSelectedLocation) {
                            currentLocation = location;
                            locationText.setText("Vá»‹ trÃ­ hiá»‡n táº¡i");

                            // TÃ­nh láº¡i khoáº£ng cÃ¡ch cá»§a cÃ¡c sáº£n pháº©m
                            filterProducts();

                        } else {
                            locationText.setText(
                                    "ChÆ°a xÃ¡c Ä‘á»‹nh Ä‘Æ°á»£c vá»‹ trÃ­"
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        locationText.setText("KhÃ´ng láº¥y Ä‘Æ°á»£c GPS");
                        Toast.makeText(
                                this,
                                "Lá»—i GPS: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    });

        } catch (SecurityException e) {
            Toast.makeText(
                    this,
                    "á»¨ng dá»¥ng chÆ°a cÃ³ quyá»n truy cáº­p vá»‹ trÃ­",
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
                    "ChÆ°a cáº¥p quyá»n vá»‹ trÃ­"
            );
        }
    }
    private void openTab(String tab) {
        if ("requests".equals(tab)) {
            showFragment(new MyPostsFragment());
            updateSelectedTab("requests");
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

    // Xá»­ lÃ½ khi báº¥m vÃ o váº­t pháº©m
    private void openProductDetail(SharenestProduct sharenestProduct) {

        // Sau nÃ y thay báº±ng Intent má»Ÿ SharenestProductDetailActivity
        // Truyá»n sharenestProduct.id Ä‘á»ƒ láº¥y Ä‘Ãºng document Firestore

        showMessage(
                sharenestProduct.name + " - ID: " + sharenestProduct.id
        );
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
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


