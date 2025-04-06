package com.example.locationf.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.locationf.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Button buttonLogout;

    // UI components
    private MaterialToolbar toolbar;
    private TextView tvName, tvEmail, tvPhone, tvCountry, tvCity, tvRole, tvPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvCountry = findViewById(R.id.tvCountry);
        tvCity = findViewById(R.id.tvCity);
        tvRole = findViewById(R.id.tvRole);
        buttonLogout = findViewById(R.id.buttonLogout);
        //tvPassword = findViewById(R.id.tvPassword);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Load user data
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showError("User not authenticated");
            return;
        }

        String userEmail = currentUser.getEmail();
        if (userEmail == null) {
            showError("Email not found");
            return;
        }

        db.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        displayUserData(documentSnapshot);
                    } else {
                        showError("User data not found");
                    }
                })
                .addOnFailureListener(e -> showError("Error: " + e.getMessage()));

        buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ProfileActivity.this, Authentification.class));
            finish();
        });
    }

    private void displayUserData(DocumentSnapshot document) {
        // Personal Info
        String firstName = document.getString("prenom");
        String lastName = document.getString("nom");
        tvName.setText(String.format("%s %s", firstName, lastName));

        tvEmail.setText(document.getString("email"));
        tvPhone.setText(document.getString("telephone"));

        // Address
        tvCountry.setText(document.getString("pays"));
        tvCity.setText(document.getString("ville"));

        // Account Info
        tvRole.setText(document.getString("role"));
        //tvPassword.setText(maskPassword(document.getString("password")));
    }

    //private String maskPassword(String password) {
      //  if (password == null || password.isEmpty()) return "";
        //return new String(new char[password.length()]).replace('\0', '•');
    //}

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}