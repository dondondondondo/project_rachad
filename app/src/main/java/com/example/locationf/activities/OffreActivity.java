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
        // Implement your Firestore search/filter logic here
        Query query = db.collection("users").document(userEmail).collection("offres")
                .orderBy("titre")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff");

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            // Update your RecyclerView with filtered results
            List<Offre> filteredList = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Offre offre = document.toObject(Offre.class);
                filteredList.add(offre);
            }
            adapter.updateList(filteredList);
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