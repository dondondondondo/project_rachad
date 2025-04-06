package com.example.locationf.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.locationf.R;
import com.example.locationf.models.Demande;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SendDemandeActivity extends AppCompatActivity {

    private EditText messageEditText;
    private Button submitButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String offerId;       // Offer ID from intent
    private String clientEmail;   // Client's email (used as user document ID)
    private String nomClient;     // From Firestore
    private String prenomClient;  // From Firestore
    private String emailAgent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_send_demande);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // Get offer ID from intent
        offerId = getIntent().getStringExtra("OFFER_ID");
        if (offerId == null) {
            Toast.makeText(this, "Offer ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        emailAgent = getIntent().getStringExtra("emailAgent");
        if (emailAgent == null) {
            Toast.makeText(this, "Agent's Email missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        messageEditText = findViewById(R.id.messageEditText);
        submitButton = findViewById(R.id.submitButton);
        submitButton.setEnabled(false); // Disable until data is ready

        // Fetch client details
        clientEmail = user.getEmail();// pour extraire l'email du client pour le mettre dans la demande
        fetchAgentDetails(emailAgent);
        fetchClientDetails(clientEmail);

        // Submit button click listener
        submitButton.setOnClickListener(v -> submitDemande(emailAgent, clientEmail));
    }

    private void fetchAgentDetails(String emailAgent) {
        db.collection("users").document(emailAgent)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nomAgent = documentSnapshot.getString("nom");
                        String prenomAgent = documentSnapshot.getString("prenom");

                        if (nomAgent == null || prenomAgent == null) {
                            Toast.makeText(this, "Name fields missing", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            submitButton.setEnabled(true); // Enable when ready
                        }
                    } else {
                        Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
    private void fetchClientDetails(String clientEmail){
        db.collection("users").document(clientEmail).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                 nomClient = documentSnapshot.getString("nom");
                prenomClient = documentSnapshot.getString("prenom");

                if (nomClient == null || prenomClient == null) {
                    Toast.makeText(this, "Name fields missing", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    submitButton.setEnabled(true); // Enable when ready
                }
            } else {
                Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }



    private void submitDemande(String emailAgent, String ClientEmail) {
        String message = messageEditText.getText().toString().trim();
        fetchClientDetails(clientEmail);

        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create demande object
        Map<String, Object> demande = new HashMap<>();
        demande.put("nomClient", nomClient);
        demande.put("prenomClient", prenomClient);
        demande.put("emailClient", ClientEmail);
        demande.put("message", message);
        demande.put("timestamp", FieldValue.serverTimestamp());

        // Add to Firestore using specified path
        db.collection("users").document(emailAgent)
                .collection("offres").document(offerId)
                .collection("demandes")
                .add(demande)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FIREBASE_SUCCESS", "Document written with ID: " + documentReference.getId());
                    Toast.makeText(this, "Demande sent successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_ERROR", "Error writing document", e);
                    Toast.makeText(this, "Error sending demande: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}