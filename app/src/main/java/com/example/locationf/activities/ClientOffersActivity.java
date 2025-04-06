package com.example.locationf.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.locationf.R;
import com.example.locationf.models.Offre;
import com.example.locationf.models.OffreAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClientOffersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OffreAdapter offreAdapter;
    private List<Offre> offreList;
    private FirebaseFirestore db;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_client_offers);

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize list and adapter
        offreList = new ArrayList<>();
        offreAdapter = new OffreAdapter(offreList, this);  // Utiliser le constructeur existant
        recyclerView.setAdapter(offreAdapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Load all offers from all agents
        loadAllOffers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu with profile and favorites options
        getMenuInflater().inflate(R.menu.client_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            // Navigate to profile
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_favorites) {
            // Navigate to favorites
            startActivity(new Intent(this, FavorisActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadAllOffers() {
        db.collectionGroup("offres") // Query all subcollections named "offres"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    offreList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Log the data being retrieved
                        Log.d("FIRESTORE_DATA", document.getData().toString());

                        try {
                            // Deserialize the document into an Offre object
                            Offre offre = document.toObject(Offre.class);
                            offre.setDocumentId(document.getId());

                            // Récupérer le chemin parent pour identifier l'agent
                            String path = document.getReference().getPath();
                            String emailAgent = path.split("/")[1]; // Format: users/email@example.com/offres/docId
                            offre.setEmailAgent(emailAgent);

                            offreList.add(offre);
                        } catch (RuntimeException e) {
                            // Log the error if deserialization fails
                            Log.e("FIRESTORE_ERROR", "Failed to deserialize document: " + document.getId(), e);
                        }
                    }
                    // Shuffle the list to display offers randomly
                    Collections.shuffle(offreList);
                    offreAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load offers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("CLIENT_OFFERS", "Error loading offers", e);
                });
    }
}