package com.example.locationf.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.locationf.R;
import com.example.locationf.models.Offre;
import com.example.locationf.models.OffreAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OffreActivity extends AppCompatActivity {

    // Declare components
    private FirebaseFirestore db;
    private CollectionReference offresRef;
    private List<Offre> offreList;
    private OffreAdapter adapter;
    private RecyclerView recyclerView;
    private FirebaseAuth mAuth;
    String userEmail;

    private Toolbar toolbar;
    private SearchView searchView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_layout_offre);

        // Initialize Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth=FirebaseAuth.getInstance();

        //Initialize the toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //verifier l'existence d'un utilisateur
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            startActivity(new Intent(this, Authentification.class));
            finish();
            return;
        }



        //FirebaseUser currentUser = mAuth.getCurrentUser();
        userEmail = user.getEmail(); // Get email properly

        offresRef = db.collection("users").document(userEmail).collection("offres");

        // Setup RecyclerView
        setupRecyclerView();

        // Load data
        loadOffres();

        FloatingActionButton fabAddOffer = findViewById(R.id.fab_add_offer);
        fabAddOffer.setOnClickListener(v -> {
            // Navigate to AddOffreActivity
            Intent intent = new Intent(OffreActivity.this, AddOffreActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.agent_nav_menu, menu);

        // Setup search functionality
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        setupSearchView();

        return true;
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterOffres(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            // Handle profile click
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void filterOffres(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // If search text is empty, load all offers
            loadOffres();
            return;
        }

        // Convert search text to lowercase for case-insensitive comparison
        String searchLower = searchText.toLowerCase().trim();

        // We need to query the collection and filter in the app code
        // since Firestore doesn't support OR conditions across multiple fields directly
        offresRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Offre> filteredList = new ArrayList<>();

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Offre offre = document.toObject(Offre.class);

                // Check if any of the relevant fields contains the search text (case insensitive)
                boolean matchesTitre = offre.getTitre() != null &&
                        offre.getTitre().toLowerCase().contains(searchLower);

                boolean matchesSuperficie = offre.getSuperficie() != null &&
                        offre.getSuperficie().toLowerCase().contains(searchLower);

                // For numeric fields, check if the search text can be parsed to a number
                boolean matchesLoyer = false;
                if (offre.getLoyer() != null) {
                    try {
                        double searchNum = Double.parseDouble(searchText);
                        // Check if the loyer equals the search number
                        matchesLoyer = Math.abs(offre.getLoyer() - searchNum) < 0.01;
                    } catch (NumberFormatException e) {
                        // If search text is not a number, check if the string representation contains it
                        matchesLoyer = offre.getLoyer().toString().contains(searchLower);
                    }
                }

                // Add the offer to filtered list if any field matches
                if (matchesTitre || matchesSuperficie || matchesLoyer) {
                    filteredList.add(offre);
                }
            }

            // Update the adapter with the filtered list
            adapter.updateList(filteredList);
        }).addOnFailureListener(e -> {
            Log.e("FIREBASE_ERROR", "Error filtering offers", e);
            Toast.makeText(OffreActivity.this, "Failed to filter offers", Toast.LENGTH_SHORT).show();
        });
    }


    private void setupRecyclerView() {
        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize list and adapter
        offreList = new ArrayList<>();
        adapter = new OffreAdapter(offreList, OffreActivity.this);//la classe qui va gérer le recycler_view
        recyclerView.setAdapter(adapter);
    }

    private void loadOffres() {
        offresRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            offreList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Offre offre = document.toObject(Offre.class); //grace à cette fonction on retire un element de la base de données appelées "document" et le convertir en un objet metier
                offre.setDocumentId(document.getId());
                offreList.add(offre);
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            Log.e("FIREBASE_ERROR", "Error loading offers", e);
            Toast.makeText(OffreActivity.this, "Failed to load offers", Toast.LENGTH_SHORT).show();
        });
    }
}