package com.example.locationf.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.locationf.R;
import com.example.locationf.models.Offre;
import com.example.locationf.models.PhotoAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddOffreActivity extends AppCompatActivity {

    private EditText editTitre, editDescription, editSuperficie, editPieces, editLoyer;
    private Button btnSelectPhotos, btnSave;
    private RecyclerView recyclerViewPhotos;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;
    private String userEmail;

    private List<Uri> selectedPhotoUris;
    private PhotoAdapter photoAdapter;
    private List<String> uploadedPhotoUrls;

    private ActivityResultLauncher<Intent> pickMultipleMediaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_add_offre);

        // Initialisation de Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userEmail = user.getEmail();

        // Initialisation des vues
        initializeViews();

        // Configuration de la sélection de photos
        setupPhotoSelection();
    }

    private void initializeViews() {
        editTitre = findViewById(R.id.edit_titre);
        editDescription = findViewById(R.id.edit_description);
        editSuperficie = findViewById(R.id.edit_superficie);
        editPieces = findViewById(R.id.edit_pieces);
        editLoyer = findViewById(R.id.edit_loyer);

        btnSelectPhotos = findViewById(R.id.btn_select_photos);
        btnSave = findViewById(R.id.btn_save);
        recyclerViewPhotos = findViewById(R.id.recycler_view_photos);

        // Initialisation des listes
        selectedPhotoUris = new ArrayList<>();
        uploadedPhotoUrls = new ArrayList<>();

        // Configuration du RecyclerView pour les photos
        photoAdapter = new PhotoAdapter(selectedPhotoUris, this::removePhoto);
        recyclerViewPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewPhotos.setAdapter(photoAdapter);

        // Click listener pour sauvegarder l'offre
        btnSave.setOnClickListener(v -> validateAndSaveOffer());
    }

    private void setupPhotoSelection() {
        // Configuration du launcher pour la sélection multiple de photos
        pickMultipleMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleSelectedPhotos(result.getData());
                    }
                }
        );

        btnSelectPhotos.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickMultipleMediaLauncher.launch(Intent.createChooser(intent, "Select Pictures"));
    }

    private void handleSelectedPhotos(Intent data) {
        if (data.getClipData() != null) {
            // Plusieurs images sélectionnées
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                selectedPhotoUris.add(imageUri);
            }
        } else if (data.getData() != null) {
            // Une seule image sélectionnée
            Uri imageUri = data.getData();
            selectedPhotoUris.add(imageUri);
        }
        photoAdapter.notifyDataSetChanged();
    }

    private void removePhoto(int position) {
        selectedPhotoUris.remove(position);
        photoAdapter.notifyItemRemoved(position);
    }

    private void validateAndSaveOffer() {
        // Vérification des champs requis
        String titre = editTitre.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String superficie = editSuperficie.getText().toString().trim();

        if (titre.isEmpty() || description.isEmpty() || superficie.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int pieces;
        double loyer;
        try {
            pieces = Integer.parseInt(editPieces.getText().toString().trim());
            loyer = Double.parseDouble(editLoyer.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Création de l'offre
        Offre offre = new Offre(titre, description, superficie, pieces, loyer, userEmail);
        handleImagesAndSaveOffer(offre);
    }

    /**
     * Convertit l'URI en bitmap, puis le compresse dans un fichier physique.
     * Cette approche permet de s'assurer que même une image provenant de Google Photos
     * soit convertie en fichier physique exploitable par Firebase Storage.
     */
    /**
     * Creates a physical file from the given URI by directly copying the InputStream.
     * This avoids issues with decoding bitmaps from virtual URIs (such as Google Photos).
     */
    private File getFileFromUri(Uri uri) throws IOException {
        // Open an InputStream from the URI
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Unable to open InputStream for URI: " + uri);
        }

        // Create (or get) an "images" folder in the app's external files directory
        File directory = new File(getExternalFilesDir(null), "images");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create a new file in that directory with a unique name
        File file = new File(directory, "temp_" + System.currentTimeMillis() + ".jpg");

        // Copy the InputStream directly to the file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
        } finally {
            inputStream.close();
        }

        // Log file creation details for debugging
        Log.d("FirebaseUpload", "Created file " + file.getAbsolutePath() +
                " (size: " + file.length() + " bytes)");
        return file;
    }



    private void handleImagesAndSaveOffer(Offre offre) {
        if (selectedPhotoUris.isEmpty()) {
            // If no photos, save the offer directly
            saveOfferToFirestore(offre);
            return;
        }

        List<String> imageNames = new ArrayList<>();

        for (Uri photoUri : selectedPhotoUris) {
            try {
                // Generate unique name for the image
                String imageName = "offre_" + UUID.randomUUID().toString() + ".jpg";

                // Convert URI to Bitmap
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);

                // Create directory in app's external files
                File directory = new File(getExternalFilesDir(null), "images");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // Save image to file
                File file = new File(directory, imageName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.flush();
                }

                imageNames.add(imageName);
                Log.d("SaveImage", "Image saved: " + file.getAbsolutePath());

            } catch (IOException e) {
                Log.e("SaveImage", "Save error for " + photoUri, e);
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        }

        // Set the list of image names to the offer
        offre.setPhotoNames(imageNames);

        // Save the offer with the image names
        saveOfferToFirestore(offre);
    }

    private void saveOfferToFirestore(Offre offre) {
        // The offer now contains image names instead of URLs
        db.collection("users").document(userEmail).collection("offres")
                .add(offre)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Offer saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save offer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}


