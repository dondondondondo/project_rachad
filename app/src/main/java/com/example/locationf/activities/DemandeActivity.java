package com.example.locationf.activities;

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
import com.example.locationf.models.Demande;
import com.example.locationf.models.DemandeAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DemandeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private List<Demande> demandeList;
    private DemandeAdapter adapter;
    private RecyclerView recyclerView;
    private FirebaseAuth mAuth;
    private String userEmail;
    private String offerId;

    private Toolbar toolbar;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_layout_demande);

        // Initialize Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize the toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Vérification de l'authentification
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            startActivity(new Intent(this, Authentification.class));
            finish();
            return;
        }

        userEmail = user.getEmail();
        offerId = getIntent().getStringExtra("OFFER_ID");
        if (offerId == null) {
            finish();
            return;
        }

        // Setup RecyclerView
        setupRecyclerView();
        loadDemandes();

        // Conserver la logique existante de chargement des demandes
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.agent_nav_menu, menu);

        // Configuration de la recherche
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
                filterDemandes(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void filterDemandes(String searchText) {
        if (userEmail == null || offerId == null) {
            Toast.makeText(this, "Configuration error", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Query query = db.collection("users")
                    .document(userEmail)
                    .collection("offres")
                    .document(offerId)
                    .collection("demandes")
                    .orderBy("clientName")
                    .startAt(searchText.toLowerCase())
                    .endAt(searchText.toLowerCase() + "\uf8ff");

            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Demande> filteredList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Demande demande = document.toObject(Demande.class);
                        filteredList.add(demande);
                    }
                    adapter.updateList(filteredList);
                } else {
                    Log.e("SEARCH_ERROR", "Search failed", task.getException());
                }
            });
        } catch (Exception e) {
            Log.e("SEARCH_EXCEPTION", "Search error", e);
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.demandesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        demandeList = new ArrayList<>();
        adapter = new DemandeAdapter(demandeList, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadDemandes() {
        db.collection("users")
                .document(userEmail)
                .collection("offres")
                .document(offerId)
                .collection("demandes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    demandeList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Demande demande = doc.toObject(Demande.class);
                        demandeList.add(demande);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading demands", Toast.LENGTH_SHORT).show();
                });
    }
}