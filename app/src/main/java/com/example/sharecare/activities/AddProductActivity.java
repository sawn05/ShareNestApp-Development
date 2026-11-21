package com.example.sharecare.activities;

import static androidx.core.location.LocationManagerCompat.getCurrentLocation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sharecare.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddProductActivity extends AppCompatActivity {

    private static final int MAX_IMAGES = 5;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    // Pháº£i trÃ¹ng vá»›i collection mÃ  HomeActivity Ä‘ang Ä‘á»c.
    private static final String PRODUCT_COLLECTION = "products";

    private EditText edtProductName;
    private EditText edtQuantity;
    private EditText edtDescription;

    private Spinner spCategory;
    private Spinner spCondition;

    private TextView txtImageCount;
    private TextView txtProductAddress;

    private LinearLayout layoutImagePreview;
    private LinearLayout btnTakePhoto;
    private LinearLayout btnChooseImages;
    private LinearLayout btnUseCurrentLocation;
    private LinearLayout btnChooseLocation;

    private ImageView btnBack;
    private Button btnPublishProduct;

    private FirebaseFirestore firestore;

    private FirebaseAuth auth;
    private FusedLocationProviderClient locationClient;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final ArrayList<SelectedImage> selectedImages =
            new ArrayList<>();

    private double selectedLatitude = Double.NaN;
    private double selectedLongitude = Double.NaN;
    private String selectedAddress = "";

    private boolean isPublishing = false;

    private ActivityResultLauncher<String> locationPermissionLauncher;
    private ActivityResultLauncher<Intent> locationPickerLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    // Má»™t áº£nh cÃ³ thá»ƒ Ä‘áº¿n tá»« thÆ° viá»‡n hoáº·c tá»« camera.
    private static class SelectedImage {
        Uri uri;
        Bitmap bitmap;

        SelectedImage(Uri uri) {
            this.uri = uri;
        }

        SelectedImage(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }
    private static final String CLOUD_NAME = "ripafvgc";

    private static final String UPLOAD_PRESET = "sharenest_products";

    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/"
                    + CLOUD_NAME
                    + "/image/upload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_product);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        locationClient =
                LocationServices.getFusedLocationProviderClient(this);

        initViews();
        registerLaunchers();
        setupSpinners();
        setupListeners();
        updateImagePreview();
    }

    // =========================
    // 1. ÃNH Xáº  GIAO DIá»†N
    // =========================

    private void initViews() {

        edtProductName = findViewById(R.id.edtProductName);
        edtQuantity = findViewById(R.id.edtQuantity);
        edtDescription = findViewById(R.id.edtDescription);

        spCategory = findViewById(R.id.spCategory);
        spCondition = findViewById(R.id.spCondition);

        txtImageCount = findViewById(R.id.txtImageCount);
        txtProductAddress = findViewById(R.id.txtProductAddress);

        layoutImagePreview =
                findViewById(R.id.layoutImagePreview);

        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnChooseImages = findViewById(R.id.btnChooseImages);

        btnUseCurrentLocation =
                findViewById(R.id.btnUseCurrentLocation);

        btnChooseLocation =
                findViewById(R.id.btnChooseLocation);

        btnBack = findViewById(R.id.btnBack);
        btnPublishProduct =
                findViewById(R.id.btnPublishProduct);
    }

    // =========================
    // 2. ÄÄ‚NG KÃ ACTIVITY RESULT
    // =========================

    private void registerLaunchers() {

        // Xin quyá»n GPS.
        locationPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        granted -> {
                            if (granted) {
                                getCurrentLocation();
                            } else {
                                showToast("Báº¡n chÆ°a cáº¥p quyá»n vá»‹ trÃ­");
                            }
                        }
                );

        // Nháº­n káº¿t quáº£ tá»« SelectLocationActivity.
        locationPickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode() != Activity.RESULT_OK
                                    || result.getData() == null) {
                                return;
                            }

                            Intent data = result.getData();

                            double latitude = data.getDoubleExtra(
                                    "latitude",
                                    Double.NaN
                            );

                            double longitude = data.getDoubleExtra(
                                    "longitude",
                                    Double.NaN
                            );

                            if (!isValidCoordinates(latitude, longitude)) {
                                showToast("Tá»a Ä‘á»™ khÃ´ng há»£p lá»‡");
                                return;
                            }

                            selectedLatitude = latitude;
                            selectedLongitude = longitude;

                            String address =
                                    data.getStringExtra("address");

                            selectedAddress =
                                    address == null ? "" : address.trim();

                            if (selectedAddress.isEmpty()) {
                                selectedAddress = formatCoordinates(
                                        latitude,
                                        longitude
                                );
                            }

                            txtProductAddress.setText(selectedAddress);
                        }
                );

        // Chá»n nhiá»u áº£nh tá»« thÆ° viá»‡n.
        galleryLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetMultipleContents(),
                        uris -> {

                            if (uris == null || uris.isEmpty()) {
                                return;
                            }

                            int added = 0;

                            for (Uri uri : uris) {

                                if (selectedImages.size() >= MAX_IMAGES) {
                                    break;
                                }

                                if (uri != null && isValidImage(uri)) {
                                    selectedImages.add(
                                            new SelectedImage(uri)
                                    );
                                    added++;
                                }
                            }

                            if (added == 0) {
                                showToast("KhÃ´ng thÃªm Ä‘Æ°á»£c áº£nh há»£p lá»‡");
                            }

                            updateImagePreview();
                        }
                );

        // Chá»¥p áº£nh báº±ng á»©ng dá»¥ng camera.
        // TakePicturePreview tráº£ vá» áº£nh xem trÆ°á»›c.
        cameraLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.TakePicturePreview(),
                        bitmap -> {

                            if (bitmap == null) {
                                return;
                            }

                            if (selectedImages.size() >= MAX_IMAGES) {
                                showToast("Chá»‰ Ä‘Æ°á»£c chá»n tá»‘i Ä‘a 5 áº£nh");
                                return;
                            }

                            selectedImages.add(
                                    new SelectedImage(bitmap)
                            );

                            updateImagePreview();
                        }
                );
    }

    // =========================
    // 3. DANH Má»¤C VÃ€ TÃŒNH TRáº NG
    // =========================

    private void setupSpinners() {

        String[] categories = {
                "Chá»n danh má»¥c",
                "Quáº§n Ã¡o",
                "SÃ¡ch",
                "Thá»±c pháº©m",
                "Äá»“ gia dá»¥ng",
                "Ná»™i tháº¥t",
                "Äá»“ Ä‘iá»‡n tá»­",
                "KhÃ¡c"
        };

        String[] conditions = {
                "Chá»n tÃ¬nh tráº¡ng",
                "Má»›i",
                "CÃ²n tá»‘t",
                "ÄÃ£ qua sá»­ dá»¥ng"
        };

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> conditionAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        conditions
                );

        conditionAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spCondition.setAdapter(conditionAdapter);
    }

    // =========================
    // 4. Xá»¬ LÃ Sá»° KIá»†N
    // =========================

    private void setupListeners() {

        btnBack.setOnClickListener(v -> finish());

        btnTakePhoto.setOnClickListener(v -> openCamera());

        btnChooseImages.setOnClickListener(
                v -> openGallery()
        );

        layoutImagePreview.setOnClickListener(
                v -> openGallery()
        );

        btnUseCurrentLocation.setOnClickListener(
                v -> requestCurrentLocation()
        );

        btnChooseLocation.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AddProductActivity.this,
                    SelectLocationActivity.class
            );

            locationPickerLauncher.launch(intent);
        });

        btnPublishProduct.setOnClickListener(
                v -> showPublishConfirmDialog()
        );
    }

    // =========================
    // 5. CHá»ŒN áº¢NH
    // =========================

    private void openGallery() {

        if (selectedImages.size() >= MAX_IMAGES) {
            showToast("Báº¡n Ä‘Ã£ chá»n Ä‘á»§ 5 áº£nh");
            return;
        }

        galleryLauncher.launch("image/*");
    }

    private void openCamera() {

        if (selectedImages.size() >= MAX_IMAGES) {
            showToast("Báº¡n Ä‘Ã£ chá»n Ä‘á»§ 5 áº£nh");
            return;
        }

        cameraLauncher.launch(null);
    }

    private boolean isValidImage(Uri uri) {
        try {
            String mimeType = getContentResolver().getType(uri);

            if (!"image/jpeg".equals(mimeType)
                    && !"image/png".equals(mimeType)) {
                return false;
            }

            try (InputStream stream =
                         getContentResolver().openInputStream(uri)) {

                if (stream == null) return false;

                byte[] buffer = new byte[8192];
                int count;
                long total = 0;

                while ((count = stream.read(buffer)) != -1) {
                    total += count;

                    if (total > MAX_IMAGE_BYTES) {
                        return false;
                    }
                }

                return total > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // =========================
    // 6. HIá»‚N THá»Š áº¢NH ÄÃƒ CHá»ŒN
    // =========================


    private void updateImagePreview() {

        txtImageCount.setText(
                selectedImages.size() + "/" + MAX_IMAGES + " áº£nh"
        );

        layoutImagePreview.removeAllViews();

        // ChÆ°a chá»n áº£nh: hiá»ƒn thá»‹ icon vÃ  chá»¯ á»Ÿ giá»¯a
        if (selectedImages.isEmpty()) {

            layoutImagePreview.setOrientation(LinearLayout.VERTICAL);
            layoutImagePreview.setGravity(android.view.Gravity.CENTER);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.gallery);

            LinearLayout.LayoutParams iconParams =
                    new LinearLayout.LayoutParams(
                            dp(32),
                            dp(32)
                    );

            iconParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            icon.setLayoutParams(iconParams);

            TextView label = new TextView(this);
            label.setText("ThÃªm áº£nh");
            label.setTextColor(0xFF247B53);
            label.setTextSize(13);
            label.setGravity(android.view.Gravity.CENTER);

            LinearLayout.LayoutParams labelParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            labelParams.topMargin = dp(6);
            label.setLayoutParams(labelParams);

            layoutImagePreview.addView(icon);
            layoutImagePreview.addView(label);

            return;
        }

        // ÄÃ£ chá»n áº£nh: hiá»ƒn thá»‹ danh sÃ¡ch áº£nh
        layoutImagePreview.setOrientation(LinearLayout.HORIZONTAL);
        layoutImagePreview.setGravity(android.view.Gravity.CENTER_VERTICAL);

        for (int i = 0; i < selectedImages.size(); i++) {

            final int index = i;
            SelectedImage item = selectedImages.get(i);

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(android.view.Gravity.CENTER);

            LinearLayout.LayoutParams itemParams =
                    new LinearLayout.LayoutParams(
                            dp(82),
                            dp(110)
                    );

            itemParams.setMargins(dp(3), 0, dp(3), 0);
            itemLayout.setLayoutParams(itemParams);

            ImageView imageView = new ImageView(this);

            imageView.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            dp(76),
                            dp(76)
                    )
            );

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (item.uri != null) {
                imageView.setImageURI(item.uri);
            } else if (item.bitmap != null) {
                imageView.setImageBitmap(item.bitmap);
            }

            ImageView removeButton = new ImageView(this);

            removeButton.setImageResource(
                    android.R.drawable.ic_menu_close_clear_cancel
            );

            removeButton.setContentDescription("XÃ³a áº£nh");

            LinearLayout.LayoutParams removeParams =
                    new LinearLayout.LayoutParams(
                            dp(22),
                            dp(22)
                    );

            removeParams.topMargin = dp(5);
            removeButton.setLayoutParams(removeParams);

            removeButton.setOnClickListener(v -> {
                selectedImages.remove(index);
                updateImagePreview();
            });

            itemLayout.addView(imageView);
            itemLayout.addView(removeButton);

            layoutImagePreview.addView(itemLayout);
        }
    }


    // =========================
    // 7. Láº¤Y GPS
    // =========================

    private void requestCurrentLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

            getCurrentLocation();

        } else {

            locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
            );
        }
    }

    private void getCurrentLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        txtProductAddress.setText(
                "Äang láº¥y vá»‹ trÃ­ hiá»‡n táº¡i..."
        );

        locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                new CancellationTokenSource().getToken()
        ).addOnSuccessListener(location -> {

            if (location == null) {
                txtProductAddress.setText(
                        "KhÃ´ng láº¥y Ä‘Æ°á»£c vá»‹ trÃ­. HÃ£y thá»­ láº¡i."
                );
                return;
            }

            selectedLatitude = location.getLatitude();
            selectedLongitude = location.getLongitude();

            selectedAddress = formatCoordinates(
                    selectedLatitude,
                    selectedLongitude
            );

            txtProductAddress.setText(selectedAddress);

            reverseGeocode(
                    selectedLatitude,
                    selectedLongitude
            );

        }).addOnFailureListener(e -> {
            showToast("Lá»—i GPS: " + e.getMessage());
        });
    }

    // =========================
    // 8. Äá»”I Tá»ŒA Äá»˜ THÃ€NH Äá»ŠA CHá»ˆ
    // =========================

    private void reverseGeocode(
            double latitude,
            double longitude
    ) {

        executor.execute(() -> {

            String address = formatCoordinates(
                    latitude,
                    longitude
            );

            try {
                Geocoder geocoder =
                        new Geocoder(
                                this,
                                new Locale("vi", "VN")
                        );

                List<Address> results =
                        geocoder.getFromLocation(
                                latitude,
                                longitude,
                                1
                        );

                if (results != null && !results.isEmpty()) {

                    String line =
                            results.get(0).getAddressLine(0);

                    if (line != null && !line.isEmpty()) {
                        address = line;
                    }
                }

            } catch (Exception ignored) {
                // Náº¿u Geocoder lá»—i, váº«n dÃ¹ng tá»a Ä‘á»™.
            }

            String finalAddress = address;

            runOnUiThread(() -> {

                // TrÃ¡nh ghi Ä‘Ã¨ khi ngÆ°á»i dÃ¹ng Ä‘Ã£ chá»n
                // má»™t vá»‹ trÃ­ khÃ¡c trong lÃºc Geocoder cháº¡y.
                if (Math.abs(selectedLatitude - latitude) > 0.000001
                        || Math.abs(selectedLongitude - longitude) > 0.000001) {
                    return;
                }

                selectedAddress = finalAddress;
                txtProductAddress.setText(finalAddress);
            });
        });
    }

    // =========================
    // 9. KIá»‚M TRA FORM
    // =========================

    private boolean validateForm() {

        String name =
                edtProductName.getText().toString().trim();

        String quantityText =
                edtQuantity.getText().toString().trim();

        String description =
                edtDescription.getText().toString().trim();

        if (name.isEmpty()) {
            edtProductName.setError("Nháº­p tÃªn váº­t pháº©m");
            edtProductName.requestFocus();
            return false;
        }

        if (spCategory.getSelectedItemPosition() == 0) {
            showToast("HÃ£y chá»n danh má»¥c");
            return false;
        }

        if (quantityText.isEmpty()) {
            edtQuantity.setError("Nháº­p sá»‘ lÆ°á»£ng");
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityText);

            if (quantity <= 0) {
                edtQuantity.setError(
                        "Sá»‘ lÆ°á»£ng pháº£i lá»›n hÆ¡n 0"
                );
                return false;
            }

        } catch (NumberFormatException e) {
            edtQuantity.setError("Sá»‘ lÆ°á»£ng khÃ´ng há»£p lá»‡");
            return false;
        }

        if (spCondition.getSelectedItemPosition() == 0) {
            showToast("HÃ£y chá»n tÃ¬nh tráº¡ng");
            return false;
        }

        if (description.isEmpty()) {
            edtDescription.setError("Nháº­p mÃ´ táº£");
            return false;
        }

        if (selectedImages.isEmpty()) {
            showToast("HÃ£y thÃªm Ã­t nháº¥t 1 áº£nh");
            return false;
        }

        if (!isValidCoordinates(
                selectedLatitude,
                selectedLongitude
        )) {
            showToast("HÃ£y chá»n vá»‹ trÃ­ váº­t pháº©m");
            return false;
        }

//        if (auth.getCurrentUser() == null) {
//            showToast("Báº¡n cáº§n Ä‘Äƒng nháº­p trÆ°á»›c khi Ä‘Äƒng bÃ i");
//            return false;
//        }

        return true;
    }

    // =========================
    // 10. ÄÄ‚NG BÃ€I
    // =========================

    private void publishProduct() {

        if (isPublishing) return;

        if (!validateForm()) return;

        isPublishing = true;

        btnPublishProduct.setEnabled(false);
        btnPublishProduct.setText("Äang chuáº©n bá»‹ Ä‘Äƒng bÃ i...");

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            startPublishing(user.getUid());
            return;
        }

        // ChÆ°a Ä‘Äƒng nháº­p thÃ¬ tá»± Ä‘Äƒng nháº­p áº©n danh.
        auth.signInAnonymously()
                .addOnSuccessListener(result -> {

                    FirebaseUser anonymousUser = result.getUser();

                    if (anonymousUser == null) {
                        uploadFailed("KhÃ´ng táº¡o Ä‘Æ°á»£c tÃ i khoáº£n áº©n danh");
                        return;
                    }

                    startPublishing(anonymousUser.getUid());
                })
                .addOnFailureListener(e -> {

                    uploadFailed(
                            "Lá»—i Ä‘Äƒng nháº­p áº©n danh: " + e.getMessage()
                    );
                });
    }

    private void startPublishing(String ownerId) {

        btnPublishProduct.setText("Äang táº£i áº£nh...");

        String productId = firestore
                .collection(PRODUCT_COLLECTION)
                .document()
                .getId();

        ArrayList<String> imageUrls = new ArrayList<>();

        // Upload táº¥t cáº£ áº£nh trÆ°á»›c khi lÆ°u Firestore
        uploadNextImage(
                productId,
                ownerId,
                0,
                imageUrls
        );
    }
    // =========================
    // 11. UPLOAD Tá»ªNG áº¢NH
    // =========================

    private void uploadNextImage(
            String productId,
            String ownerId,
            int index,
            ArrayList<String> imageUrls
    ) {

        if (index >= selectedImages.size()) {

            // Táº¥t cáº£ áº£nh Ä‘Ã£ upload thÃ nh cÃ´ng
            saveProductToFirestore(
                    productId,
                    ownerId,
                    imageUrls
            );

            return;
        }

        btnPublishProduct.setText(
                "Äang táº£i áº£nh "
                        + (index + 1)
                        + "/"
                        + selectedImages.size()
        );

        SelectedImage item = selectedImages.get(index);

        // Upload áº£nh trong luá»“ng ná»n
        executor.execute(() -> {

            try {

                String imageUrl = uploadImageToCloudinary(item);

                runOnUiThread(() -> {

                    imageUrls.add(imageUrl);

                    Log.d(
                            "CLOUDINARY",
                            "Upload thÃ nh cÃ´ng: " + imageUrl
                    );

                    // Upload áº£nh tiáº¿p theo
                    uploadNextImage(
                            productId,
                            ownerId,
                            index + 1,
                            imageUrls
                    );
                });

            } catch (Exception e) {

                Log.e(
                        "CLOUDINARY",
                        "Upload áº£nh tháº¥t báº¡i",
                        e
                );

                runOnUiThread(() ->
                        uploadFailed(
                                "Lá»—i táº£i áº£nh "
                                        + (index + 1)
                                        + ": "
                                        + e.getMessage()
                        )
                );
            }
        });
    }

    private String uploadImageToCloudinary(
            SelectedImage item
    ) throws Exception {

        byte[] imageBytes;

        if (item.uri != null) {

            try (InputStream input =
                         getContentResolver().openInputStream(item.uri);
                 ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                if (input == null) {
                    throw new Exception("KhÃ´ng Ä‘á»c Ä‘Æ°á»£c áº£nh");
                }

                byte[] buffer = new byte[8192];
                int length;

                while ((length = input.read(buffer)) != -1) {

                    output.write(buffer, 0, length);

                    if (output.size() > MAX_IMAGE_BYTES) {
                        throw new Exception("áº¢nh vÆ°á»£t quÃ¡ 10 MB");
                    }
                }

                imageBytes = output.toByteArray();
            }

        } else if (item.bitmap != null) {

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            boolean compressed = item.bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    85,
                    output
            );

            if (!compressed) {
                throw new Exception("KhÃ´ng nÃ©n Ä‘Æ°á»£c áº£nh");
            }

            imageBytes = output.toByteArray();

        } else {
            throw new Exception("áº¢nh khÃ´ng há»£p lá»‡");
        }

        if (imageBytes.length == 0
                || imageBytes.length > MAX_IMAGE_BYTES) {

            throw new Exception("Dung lÆ°á»£ng áº£nh khÃ´ng há»£p lá»‡");
        }

        String boundary =
                "ShareNest" + UUID.randomUUID();

        HttpURLConnection connection = null;

        try {

            URL url = new URL(CLOUDINARY_UPLOAD_URL);

            connection = (HttpURLConnection)
                    url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(60000);

            connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
            );

            String mimeType = "image/jpeg";

            if (item.uri != null) {
                String detected =
                        getContentResolver().getType(item.uri);

                if ("image/png".equals(detected)) {
                    mimeType = "image/png";
                }
            }

            String extension = "image/png".equals(mimeType)
                    ? ".png" : ".jpg";

            try (OutputStream output =
                         connection.getOutputStream()) {

                String presetPart =
                        "--" + boundary + "\r\n"
                                + "Content-Disposition: form-data; "
                                + "name=\"upload_preset\"\r\n\r\n"
                                + UPLOAD_PRESET + "\r\n";

                output.write(
                        presetPart.getBytes(StandardCharsets.UTF_8)
                );

                String filePart =
                        "--" + boundary + "\r\n"
                                + "Content-Disposition: form-data; "
                                + "name=\"file\"; filename=\""
                                + UUID.randomUUID()
                                + extension
                                + "\"\r\n"
                                + "Content-Type: "
                                + mimeType
                                + "\r\n\r\n";

                output.write(
                        filePart.getBytes(StandardCharsets.UTF_8)
                );

                output.write(imageBytes);

                output.write(
                        "\r\n".getBytes(StandardCharsets.UTF_8)
                );

                String ending =
                        "--" + boundary + "--\r\n";

                output.write(
                        ending.getBytes(StandardCharsets.UTF_8)
                );
            }

            int responseCode =
                    connection.getResponseCode();

            InputStream responseStream =
                    responseCode >= 200 && responseCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            String response;

            try (InputStream input = responseStream;
                 ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                if (input == null) {
                    throw new Exception(
                            "Cloudinary khÃ´ng tráº£ vá» dá»¯ liá»‡u"
                    );
                }

                byte[] buffer = new byte[4096];
                int length;

                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }

                response = output.toString("UTF-8");
            }

            if (responseCode < 200 || responseCode >= 300) {

                String message = response;

                try {
                    JSONObject errorJson =
                            new JSONObject(response);

                    message = errorJson
                            .getJSONObject("error")
                            .optString("message", response);

                } catch (Exception ignored) {
                }

                throw new Exception(
                        "Cloudinary HTTP "
                                + responseCode
                                + ": "
                                + message
                );
            }

            JSONObject json = new JSONObject(response);

            String secureUrl =
                    json.optString("secure_url", "");

            if (!secureUrl.startsWith("https://")) {

                throw new Exception(
                        "Cloudinary khÃ´ng tráº£ vá» URL áº£nh há»£p lá»‡"
                );
            }

            return secureUrl;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    // =========================
    // 12. LÆ¯U FIRESTORE
    // =========================

    private void saveProductToFirestore(
            String productId,
            String ownerId,
            ArrayList<String> imageUrls
    ) {

        btnPublishProduct.setText("Äang Ä‘Äƒng bÃ i...");

        String name =
                edtProductName.getText().toString().trim();

        String category =
                spCategory.getSelectedItem().toString();

        int quantity = Integer.parseInt(
                edtQuantity.getText().toString().trim()
        );

        String description =
                edtDescription.getText().toString().trim();

        String condition;

        switch (spCondition.getSelectedItemPosition()) {
            case 1:
                condition = "NEW";
                break;

            case 2:
                condition = "GOOD";
                break;

            case 3:
                condition = "USED";
                break;

            default:
                condition = "GOOD";
                break;
        }

        Map<String, Object> SharenestProduct = new HashMap<>();

        SharenestProduct.put("name", name);
        SharenestProduct.put("category", category);
        SharenestProduct.put("quantity", quantity);
        SharenestProduct.put("quantityAvailable", quantity);
        SharenestProduct.put("quantityReserved", 0);
        SharenestProduct.put("quantityCompleted", 0);

        SharenestProduct.put("condition", condition);
        SharenestProduct.put("description", description);

        SharenestProduct.put("address", selectedAddress);
        SharenestProduct.put("latitude", selectedLatitude);
        SharenestProduct.put("longitude", selectedLongitude);

        SharenestProduct.put("imageUrls", imageUrls);
        SharenestProduct.put(
                "coverImageUrl",
                imageUrls.isEmpty() ? "" : imageUrls.get(0)
        );

        SharenestProduct.put("ownerId", ownerId);
        SharenestProduct.put("status", "AVAILABLE");
        SharenestProduct.put("moderationStatus", "VISIBLE");
        SharenestProduct.put("isDeleted", false);

        SharenestProduct.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        SharenestProduct.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        firestore.collection(PRODUCT_COLLECTION)
                .document(productId)
                .set(SharenestProduct)
                .addOnSuccessListener(unused -> {

                    isPublishing = false;

                    Intent intent = new Intent(
                            AddProductActivity.this,
                            PublishSuccessActivity.class
                    );

                    intent.putExtra("productId", productId);

                    startActivity(intent);
                    finish();

                })
                .addOnFailureListener(e -> {

                    isPublishing = false;

                    btnPublishProduct.setEnabled(true);
                    btnPublishProduct.setText("ÄÄƒng bÃ i");

                    showToast(
                            "Lá»—i lÆ°u Firestore: " + e.getMessage()
                    );
                });
    }

    private void uploadFailed(String message) {

        isPublishing = false;

        btnPublishProduct.setEnabled(true);
        btnPublishProduct.setText("ÄÄƒng bÃ i");

        showToast(message);
    }
    private void showPublishConfirmDialog() {
        if (!validateForm()) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("XÃ¡c nháº­n Ä‘Äƒng bÃ i")
                .setMessage(
                        "Báº¡n cÃ³ cháº¯c cháº¯n muá»‘n Ä‘Äƒng váº­t pháº©m nÃ y?\n\n" +
                                "Sau khi Ä‘Äƒng, má»i ngÆ°á»i cÃ³ thá»ƒ nhÃ¬n tháº¥y " +
                                "váº­t pháº©m vÃ  gá»­i yÃªu cáº§u nháº­n."
                )
                .setNegativeButton("Há»§y", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("ÄÄƒng bÃ i", (dialog, which) -> {
                    publishProduct();
                })
                .show();
    }
    // =========================
    // 13. HÃ€M Há»– TRá»¢
    // =========================

    private boolean isValidCoordinates(
            double latitude,
            double longitude
    ) {
        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    private String formatCoordinates(
            double latitude,
            double longitude
    ) {
        return String.format(
                Locale.US,
                "%.6f, %.6f",
                latitude,
                longitude
        );
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics()
                        .density + 0.5f
        );
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
