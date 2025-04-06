package com.example.locationf.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.locationf.R;
import com.example.locationf.models.Offre;
import com.example.locationf.models.OffreAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavorisActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OffreAdapter adapter;
    private List<Offre> offreList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoris);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OffreAdapter(offreList, this);
        recyclerView.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        if (currentUserEmail != null) {
            db.collection("users").document(currentUserEmail)
                    .collection("favoris")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot favDoc : queryDocumentSnapshots) {
                            // Utiliser l'ID du document comme offreId
                            String offreId = favDoc.getId();
                            String agentEmail = favDoc.getString("emailAgent");

                            if (agentEmail != null) {
                                db.collection("users").document(agentEmail)
                                        .collection("offres")
                                        .document(offreId)
                                        .get()
                                        .addOnSuccessListener(offreDoc -> {
                                            Offre offre = offreDoc.toObject(Offre.class);
                                            if (offre != null) {
                                                offre.setDocumentId(offreDoc.getId());
                                                offreList.add(offre);
                                                adapter.notifyDataSetChanged();
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e("FavorisActivity", "Erreur de récupération de l'offre: " + e.getMessage()));
                            } else {
                                Log.e("FavorisActivity", "agentEmail est nul pour le favori avec id: " + offreId);
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e("FavorisActivity", "Erreur de récupération des favoris: " + e.getMessage()));
        } else {
            Log.e("FavorisActivity", "currentUserEmail est nul");
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}