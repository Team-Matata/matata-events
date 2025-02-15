package com.example.matata;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/**
 * The `TestFacilityPicActivity` class is a simplified version of the `FacilityPicActivity`
 * designed for UI testing purposes. It provides basic functionality for selecting, uploading,
 * and deleting a facility profile picture, without Firebase or other external dependencies.
 *
 * <h2>Features:</h2>
 * <ul>
 *     <li>Load and display an existing profile picture from local storage.</li>
 *     <li>Allow the user to select a new profile picture from the gallery.</li>
 *     <li>Save the selected profile picture's URI in SharedPreferences for persistence.</li>
 *     <li>Delete the profile picture and reset the display to a default placeholder.</li>
 * </ul>
 */
public class TestFacilityPicActivity extends AppCompatActivity {

    /**
     * ImageView for displaying the facility profile picture.
     */
    private ImageView ivProfilePicture;

    /**
     * URI for storing the selected profile picture from the gallery.
     */
    private Uri selectedImageUri;

    /**
     * Launcher for handling the result of the profile picture selection process.
     */
    final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    saveProfilePictureUri(selectedImageUri);
                    loadProfilePicture();
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show();
                }
            });

    /**
     * Called when the activity is created. Sets up UI components, loads the existing profile picture,
     * and handles button interactions for uploading, deleting, and navigating back.
     *
     * @param savedInstanceState The activity's previously saved state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.facility_pic);

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        ImageView btnBack = findViewById(R.id.btnBack);
        Button btnUploadPicture = findViewById(R.id.btnUploadPicture);
        Button btnDeletePicture = findViewById(R.id.btnDeletePicture);

        loadProfilePicture();

        btnBack.setOnClickListener(v -> finish());
        ivProfilePicture.setOnClickListener(v -> openImagePicker());
        btnUploadPicture.setOnClickListener(v -> uploadProfilePicture());
        btnDeletePicture.setOnClickListener(v -> deleteProfilePicture());
    }

    /**
     * Opens the image picker for the user to select a profile picture.
     * The selected image is handled by the `pickImageLauncher`.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    /**
     * Saves the URI of the selected profile picture to SharedPreferences.
     *
     * @param uri The URI of the selected profile picture.
     */
    private void saveProfilePictureUri(Uri uri) {
        getSharedPreferences("FacilityPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("profile_image_uri", uri.toString())
                .apply();
    }

    /**
     * Loads the profile picture from SharedPreferences and displays it in the `ImageView`.
     * If no profile picture is saved, a default placeholder image is displayed.
     */
    private void loadProfilePicture() {
        String imageUriString = getSharedPreferences("FacilityPrefs", Context.MODE_PRIVATE)
                .getString("profile_image_uri", null);
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            ivProfilePicture.setImageURI(imageUri);
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_upload);
        }
    }

    /**
     * Uploads the selected profile picture by saving its URI to SharedPreferences
     * and displaying a success message. If no image is selected, a warning is shown.
     */
    private void uploadProfilePicture() {
        if (selectedImageUri != null) {
            saveProfilePictureUri(selectedImageUri);
            Toast.makeText(this, "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No image selected to upload", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes the profile picture by removing its URI from SharedPreferences, resetting
     * the `ImageView` to a default placeholder, and displaying a success message.
     */
    private void deleteProfilePicture() {
        ivProfilePicture.setImageResource(R.drawable.ic_upload);
        selectedImageUri = null;

        getSharedPreferences("FacilityPrefs", Context.MODE_PRIVATE)
                .edit()
                .remove("profile_image_uri")
                .apply();

        Toast.makeText(this, "Profile picture deleted", Toast.LENGTH_SHORT).show();
    }
}
