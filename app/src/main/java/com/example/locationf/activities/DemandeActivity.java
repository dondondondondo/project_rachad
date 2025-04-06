package com.example.locationf.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.locationf.R;
import com.example.locationf.models.Demande;
import com.example.locationf.models.DemandeAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DemandeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DemandeAdapter adapter;
    private List<Demande> demandeList;
    private FirebaseFirestore db;
    private String offerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_demande);

        // Get offer ID from intent
        offerId = getIntent().getStringExtra("OFFER_ID");
        if(offerId == null) {
            finish();
            return;
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null || user.getEmail() == null) {
            finish();
            return;
        }

        recyclerView = findViewById(R.id.demandesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        demandeList = new ArrayList<>();
        adapter = new DemandeAdapter(demandeList, this);
        recyclerView.setAdapter(adapter);

        loadDemandes(user.getEmail());
    }

    private void loadDemandes(String userEmail) {
        db.collection("users")
                .document(userEmail)
                .collection("offres")
                .document(offerId)
                .collection("demandes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    demandeList.clear();
                    for(QueryDocumentSnapshot doc : queryDocumentSnapshots) {
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