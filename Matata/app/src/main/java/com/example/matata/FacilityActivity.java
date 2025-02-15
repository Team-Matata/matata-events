//hello
package com.example.matata;

import static androidx.camera.core.CameraXThreads.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The {@code FacilityActivity} class allows users to view, edit, and save information about facilities.
 * This activity integrates with Firebase Firestore for data storage and retrieval, and uses Glide for
 * profile image handling. Users can enter or modify details such as facility name, address, capacity,
 * contact information, email, owner details, and profile picture. Additionally, it supports enabling
 * or disabling notifications for the facility.
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>View and edit facility details stored in Firebase Firestore.</li>
 *     <li>Upload and manage facility profile pictures.</li>
 *     <li>Enable or disable notifications for the facility.</li>
 *     <li>Handles frozen facility states with a dialog notification.</li>
 * </ul>
 */
public class FacilityActivity extends AppCompatActivity {

    /**
     * EditText for entering the facility name.
     */
    private EditText facilityName;

    /**
     * EditText for entering the facility address.
     */
    private EditText facilityAddress;

    /**
     * EditText for entering the facility capacity.
     */
    private EditText facilityCapacity;

    /**
     * EditText for entering the facility contact information.
     */
    private EditText facilityContact;

    /**
     * EditText for entering the facility email address.
     */
    private EditText facilityEmail;

    /**
     * EditText for entering the facility owner's name.
     */
    private EditText facilityOwner;

    /**
     * Switch for enabling or disabling notifications for the facility.
     */
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchNotification;

    /**
     * String representing the URI of the selected profile image.
     */
    private String imageUriString;

    /**
     * String representing the unique ID of the user.
     */
    private String USER_ID = "";

    /**
     * String representing the facility ID.
     */
    private String facilityId;

    /**
     * Instance of FirebaseFirestore for database operations.
     */
    private FirebaseFirestore db;

    /**
     * ImageView for displaying the facility profile picture.
     */
    private ImageView profileIcon;

    /**
     * Button for saving facility data to Firestore.
     */
    private Button saveButton;

    /**
     * Button for clearing all entered facility information.
     */
    private Button clearAllButton;

    /**
     * ImageView for handling the back button functionality.
     */
    private ImageView btnBackProfile;

    private PlacesClient placesClient;
    private RecyclerView suggestions;
    private PredictionAdapter predictionAdapter;
    private boolean isPreloading = true;
    private LatLng selectedLatLng;


    /**
     * ActivityResultLauncher for handling the result of profile picture selection.
     */


    private final ActivityResultLauncher<Intent> profilePicLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadFacilityPicture(getSharedPreferences("FacilityPrefs", MODE_PRIVATE)
                            .getString("profile_image_uri", null));
                }
            });

    /**
     * Called when the activity is created.
     * Initializes Firebase, sets up UI elements, and loads facility data if available.
     *
     * @param savedInstanceState Bundle containing the activity's previous state, if it exists.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_facility_profile);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(),"AIzaSyBrBi-UznGkZ2cluWnvg1ndThNw7EbYuYU");
        }
        placesClient = Places.createClient(this);


        db = FirebaseFirestore.getInstance();

        facilityId = getIntent().getStringExtra("FACILITY_ID");
        if (facilityId != null) {
            USER_ID = facilityId;
        } else {
            USER_ID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        facilityName = findViewById(R.id.facilityName);
        facilityAddress = findViewById(R.id.facilityAddress);
        facilityCapacity = findViewById(R.id.facilityCapacity);
        facilityContact = findViewById(R.id.facilityContact);
        facilityEmail = findViewById(R.id.facilityEmail);
        facilityOwner = findViewById(R.id.facilityOwner);
        switchNotification = findViewById(R.id.switch_notification);
        saveButton = findViewById(R.id.saveButton);
        clearAllButton = findViewById(R.id.clearAllButton);
        btnBackProfile = findViewById(R.id.btnBackProfile);
        profileIcon = findViewById(R.id.profileIcon);
        suggestions=findViewById(R.id.address_suggestions);


        suggestions.setVisibility(View.GONE);
        predictionAdapter = new PredictionAdapter(prediction -> {
            getPlaceDetails(prediction.getPlaceId());
            facilityAddress.clearFocus();

        });
        suggestions.setLayoutManager(new LinearLayoutManager(this));
        suggestions.setAdapter(predictionAdapter);



        btnBackProfile.setOnClickListener(v -> finish());

        imageUriString = getSharedPreferences("FacilityPrefs", MODE_PRIVATE)
                .getString("profile_image_uri", null);

        loadFacilityPicture(imageUriString);

        loadFacilityData();

        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(FacilityActivity.this, FacilityPicActivity.class);
            intent.putExtra("userId", USER_ID);
            profilePicLauncher.launch(intent);
        });

        facilityAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @SuppressLint("RestrictedApi")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) { // Remove isPreloading dependency
                    fetchAutocompletePredictions(s.toString());
                    suggestions.setVisibility(View.VISIBLE);
                } else {
                    predictionAdapter.updateData(new ArrayList<>());
                    suggestions.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        facilityAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                suggestions.setVisibility(View.VISIBLE);
            } else {
                suggestions.setVisibility(View.GONE);
            }
        });

        saveButton.setOnClickListener(v -> {
            String name = facilityName.getText().toString();
            String address = facilityAddress.getText().toString();
            String capacity = facilityCapacity.getText().toString();
            String contact = facilityContact.getText().toString();
            String email = facilityEmail.getText().toString();
            String owner = facilityOwner.getText().toString();
            boolean notificationsEnabled = switchNotification.isChecked();

            if (name.isEmpty()) {
                Toast.makeText(this, "No UserName Found", Toast.LENGTH_SHORT).show();
            } else if (address.isEmpty()) {
                Toast.makeText(this, "No Address Found", Toast.LENGTH_SHORT).show();
            } else if (contact.isEmpty()) {
                Toast.makeText(this, "No Contact Found", Toast.LENGTH_SHORT).show();
            } else if (capacity.isEmpty()) {
                Toast.makeText(this, "No Capacity Found", Toast.LENGTH_SHORT).show();
            } else if (email.isEmpty()) {
                Toast.makeText(this, "No Email Found", Toast.LENGTH_SHORT).show();
            } else if (owner.isEmpty()) {
                Toast.makeText(this, "No Owner Found", Toast.LENGTH_SHORT).show();
            } else {
                saveFacilityData(name, address, capacity, contact, email, owner, notificationsEnabled, imageUriString);
            }
        });

        clearAllButton.setOnClickListener(v -> {
            facilityName.setText("");
            facilityAddress.setText("");
            facilityCapacity.setText("");
            facilityContact.setText("");
            facilityEmail.setText("");
            facilityOwner.setText("");
            switchNotification.setChecked(false);

            String name = facilityName.getText().toString();
            String address = facilityAddress.getText().toString();
            String capacity = facilityCapacity.getText().toString();
            String contact = facilityContact.getText().toString();
            String email = facilityEmail.getText().toString();
            String owner = facilityOwner.getText().toString();
            boolean notificationsEnabled = switchNotification.isChecked();

            saveFacilityData(name, address, capacity, contact, email, owner, notificationsEnabled, imageUriString);
        });

    }

    /**
     * Saves facility data to Firebase Firestore with the provided details.
     *
     * @param name               Facility name.
     * @param address            Facility address.
     * @param capacity           Facility capacity.
     * @param contact            Facility contact information.
     * @param email              Facility email address.
     * @param owner              Facility owner's name.
     * @param notificationsEnabled Whether notifications are enabled for the facility.
     * @param imageUriString     URI string of the facility's profile picture.
     */
    private void saveFacilityData(String name, String address, String capacity, String contact, String email, String owner, boolean notificationsEnabled, String imageUriString) {
        loadFacilityPicture(imageUriString);

        Map<String, Object> facilityProfile = new HashMap<>();
        facilityProfile.put("name", name);
        facilityProfile.put("address", address);
        facilityProfile.put("email", email);
        facilityProfile.put("notifications", notificationsEnabled);
        facilityProfile.put("profileUri", imageUriString);
        facilityProfile.put("capacity", capacity);
        facilityProfile.put("contact", contact);
        facilityProfile.put("owner", owner);
        facilityProfile.put("freeze", "awake");

        if (selectedLatLng != null) {
            GeoPoint geoPoint = new GeoPoint(selectedLatLng.latitude, selectedLatLng.longitude);
            facilityProfile.put("location", geoPoint);
        } else {
            Log.w("GeoPoint", "No location selected, GeoPoint not saved.");
        }

        db.collection("FACILITY_PROFILES").document(USER_ID)
                .set(facilityProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(FacilityActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(FacilityActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup listeners or other resources associated with PlacesClient
        if (placesClient != null) {
            placesClient = null;
        }
    }

    /**
     * Loads the facility's profile picture from the provided URI.
     * If no URI is available, generates a default image using the facility's name initials.
     *
     * @param imageUriString URI of the facility's profile picture.
     */
    void loadFacilityPicture(String imageUriString) {
        if (imageUriString != null && !imageUriString.isEmpty()) {
            Uri imageUri = Uri.parse(imageUriString);
            Glide.with(this)
                    .load(imageUri)
                    .into(profileIcon);
        } else {
            String un = facilityName.getText().toString().trim();
            if (un.isEmpty()) {
                profileIcon.setImageResource(R.drawable.ic_facility_profile);
            } else {
                StringBuilder initials = new StringBuilder();
                String[] words = un.trim().split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        initials.append(Character.toUpperCase(word.charAt(0)));
                    }
                }
                String initial = initials.toString();
                Uri cimageUri = createImageFromString(this, initial);
                Glide.with(this).load(cimageUri).into(profileIcon);
            }
        }
    }

    /**
     * Loads facility data from Firebase Firestore and populates the respective fields.
     * Displays a frozen dialog if the facility is marked as "frozen" in the database.
     */
    private void loadFacilityData() {
        DocumentReference docRef = db.collection("FACILITY_PROFILES").document(USER_ID);
        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Load existing data into fields
                        String name = documentSnapshot.getString("name");
                        String address = documentSnapshot.getString("address");
                        String email = documentSnapshot.getString("email");
                        boolean notificationsChecked = Boolean.TRUE.equals(documentSnapshot.getBoolean("notifications"));
                        String sImageUri = documentSnapshot.getString("profileUri");
                        String capacity = documentSnapshot.getString("capacity");
                        String contact = documentSnapshot.getString("contact");
                        String owner = documentSnapshot.getString("owner");

                        facilityName.setText(name);
                        facilityAddress.setText(address);
                        facilityCapacity.setText(capacity);
                        facilityContact.setText(contact);
                        facilityEmail.setText(email);
                        facilityOwner.setText(owner);
                        switchNotification.setChecked(notificationsChecked);

                        loadFacilityPicture(sImageUri);
                    } else {
                        // Profile doesn't exist: ensure address suggestions work
                        facilityAddress.setEnabled(true); // Ensure the field is active
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(FacilityActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    // Allow address suggestions even on failure
                    facilityAddress.setEnabled(true);
                });
    }

    /**
     * Creates an image from a string of initials and returns a URI for the generated image.
     *
     * @param context The context in which the image is created.
     * @param text    The initials text to display in the image.
     * @return URI of the generated image.
     */
    public static Uri createImageFromString(Context context, String text) {
        int bitmapWidth = 400;
        int bitmapHeight = 400;
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(100);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);

        float textHeight = paint.descent() - paint.ascent();
        float textOffset = (textHeight / 2) - paint.descent();
        canvas.drawText(text, (float) bitmapWidth / 2, ((float) bitmapHeight / 2) + textOffset, paint);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        String base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        return Uri.parse("data:image/png;base64," + base64String);
    }
    @SuppressLint("RestrictedApi")
    private void fetchAutocompletePredictions(String q){
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(53.2, -113.7),
                new LatLng(53.8, -113.2)
        );
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(q)
                .setCountries("CA")
                .setLocationBias(bounds)
                .build();
        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                    if (!predictions.isEmpty()) {
                        predictionAdapter.updateData(predictions);
                        suggestions.setVisibility(View.VISIBLE);
                    } else {
                        suggestions.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.wtf(TAG, "Autocomplete prediction fetch failed: " + exception.getMessage());
                    suggestions.setVisibility(View.GONE);
                });
    }
    private void getPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        );
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    Log.d("Place Details", "Name: " + place.getName());
                    Log.d("Place Details", "Address: " + place.getAddress());
                    Log.d("Place Details", "LatLng: " + place.getLatLng());
                    facilityAddress.setText(place.getAddress());
                    selectedLatLng = place.getLatLng();
                })
                .addOnFailureListener(exception -> {
                    Log.e("TAG", "Error fetching place details: " + exception.getMessage());
                });
    }

}
