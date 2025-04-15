package com.example.locationf.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.locationf.R;
import com.example.locationf.models.Offre;
import com.example.locationf.models.OffreAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientOffersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OffreAdapter offreAdapter;
    private List<Offre> offreList;
    private List<Offre> allOffersFullList; // liste de sauvegarde pour le filtrage
    private List<Offre> favoritesList;
    private FirebaseFirestore db;
    private MaterialToolbar toolbar;
    private String currentUserEmail;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_client_offers);

        // Configuration du toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialisation du RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialisation des listes et de l'adapter
        offreList = new ArrayList<>();
        allOffersFullList = new ArrayList<>();
        favoritesList = new ArrayList<>();
        offreAdapter = new OffreAdapter(offreList, this);
        recyclerView.setAdapter(offreAdapter);

        // Initialisation de Firestore
        db = FirebaseFirestore.getInstance();

        // Récupérer l'utilisateur connecté
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserEmail = currentUser.getEmail();
            // Charger d'abord les favoris, puis toutes les offres
            loadFavorites();
        } else {
            // Si aucun utilisateur n'est connecté, charger toutes les offres
            loadAllOffers();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflatation du menu (assurez-vous que client_menu.xml contient action_search, action_profile et action_favorites)
        getMenuInflater().inflate(R.menu.client_menu, menu);

        // Configuration de la recherche
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
            setupSearchView();
        }
        return true;
    }

    private void setupSearchView() {
        if (searchView == null) return;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterOffres(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterOffres(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_favorites) {
            startActivity(new Intent(this, FavorisActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void filterOffres(String searchText) {
        if (allOffersFullList == null || allOffersFullList.isEmpty()) return;
        if (searchText == null || searchText.trim().isEmpty()) {
            // Si le texte de recherche est vide, on restaure la liste complète
            offreList.clear();
            offreList.addAll(allOffersFullList);
            offreAdapter.notifyDataSetChanged();
            return;
        }

        String searchLower = searchText.toLowerCase().trim();
        List<Offre> filteredList = new ArrayList<>();
        for (Offre offre : allOffersFullList) {
            boolean matchesTitre = offre.getTitre() != null && offre.getTitre().toLowerCase().contains(searchLower);
            boolean matchesSuperficie = offre.getSuperficie() != null && offre.getSuperficie().toLowerCase().contains(searchLower);
            boolean matchesLoyer = false;
            if (offre.getLoyer() != null) {
                try {
                    double searchNum = Double.parseDouble(searchText);
                    // Vérifie l'égalité numérique avec une petite marge d'erreur
                    matchesLoyer = Math.abs(offre.getLoyer() - searchNum) < 0.01;
                } catch (NumberFormatException e) {
                    // Sinon, vérifie la présence du texte dans la représentation en chaîne
                    matchesLoyer = offre.getLoyer().toString().contains(searchLower);
                }
            }
            if (matchesTitre || matchesSuperficie || matchesLoyer) {
                filteredList.add(offre);
            }
        }
        offreList.clear();
        offreList.addAll(filteredList);
        offreAdapter.notifyDataSetChanged();
    }

    private void loadFavorites() {
        if (currentUserEmail == null) {
            loadAllOffers();
            return;
        }
        db.collection("users").document(currentUserEmail)
                .collection("favoris")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoritesList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        loadAllOffers();
                        return;
                    }
                    final int[] favoritesCount = {queryDocumentSnapshots.size()};
                    final int[] loadedCount = {0};

                    for (QueryDocumentSnapshot favDoc : queryDocumentSnapshots) {
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
                                            favoritesList.add(offre);
                                        }
                                        loadedCount[0]++;
                                        if (loadedCount[0] >= favoritesCount[0]) {
                                            loadAllOffers();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("ClientOffersActivity", "Erreur lors du chargement d'un favori : " + e.getMessage());
                                        loadedCount[0]++;
                                        if (loadedCount[0] >= favoritesCount[0]) {
                                            loadAllOffers();
                                        }
                                    });
                        } else {
                            loadedCount[0]++;
                            if (loadedCount[0] >= favoritesCount[0]) {
                                loadAllOffers();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ClientOffersActivity", "Erreur lors du chargement des favoris : " + e.getMessage());
                    loadAllOffers();
                });
    }

    private void loadAllOffers() {
        // Récupère toutes les offres via une collectionGroup
        db.collectionGroup("offres")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Offre> allOffers = new ArrayList<>();
                    offreList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Offre offre = document.toObject(Offre.class);
                            offre.setDocumentId(document.getId());
                            // Extraction de l'email de l'agent à partir du chemin (users/email/offres/docId)
                            String path = document.getReference().getPath();
                            String[] pathElements = path.split("/");
                            if (pathElements.length >= 2) {
                                String emailAgent = pathElements[1];
                                offre.setEmailAgent(emailAgent);
                            }
                            allOffers.add(offre);
                        } catch (RuntimeException e) {
                            Log.e("FIRESTORE_ERROR", "Échec de la désérialisation du document : " + document.getId(), e);
                        }
                    }
                    // Application d'un système de recommandation si des favoris existent
                    if (!favoritesList.isEmpty()) {
                        LightweightRecommendationSystem recommendationSystem =
                                new LightweightRecommendationSystem(favoritesList, allOffers);
                        List<Offre> recommendations = recommendationSystem.recommanderOffres();
                        offreList.addAll(recommendations);
                        for (Offre offre : allOffers) {
                            if (!recommendations.contains(offre)) {
                                offreList.add(offre);
                            }
                        }
                    } else {
                        // Sinon, on mélange les offres
                        Collections.shuffle(allOffers);
                        offreList.addAll(allOffers);
                    }
                    // Sauvegarde de la liste complète pour le filtrage
                    allOffersFullList.clear();
                    allOffersFullList.addAll(offreList);
                    offreAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Échec du chargement des offres : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("CLIENT_OFFERS", "Erreur lors du chargement des offres", e);
                });
    }

    // Système de recommandation léger intégré dans l'activité
    private class LightweightRecommendationSystem {
        private List<Offre> offresFavorites;
        private List<Offre> offresDisponibles;

        public LightweightRecommendationSystem(List<Offre> offresFavorites, List<Offre> offresDisponibles) {
            this.offresFavorites = offresFavorites;
            this.offresDisponibles = offresDisponibles;
        }

        // Calcule la distance euclidienne entre les caractéristiques d'une offre et la moyenne des favoris
        private double computeDistance(Offre offre, double avgSuperficie, double avgPieces, double avgLoyer) {
            double diffSup = 0;
            try {
                double superficie = Double.parseDouble(offre.getSuperficie());
                diffSup = superficie - avgSuperficie;
            } catch (NumberFormatException e) {
                // En cas d'erreur, on considère la différence nulle
            }
            double diffPieces = offre.getPieces() - avgPieces;
            double diffLoyer = offre.getLoyer() - avgLoyer;
            return Math.sqrt(diffSup * diffSup + diffPieces * diffPieces + diffLoyer * diffLoyer);
        }

        // Recommande les offres dont la distance par rapport à la moyenne des offres favorites est inférieure à un seuil
        public List<Offre> recommanderOffres() {
            List<Offre> recommandations = new ArrayList<>();
            if (offresFavorites == null || offresFavorites.isEmpty()) {
                return recommandations;
            }
            double sumSuperficie = 0;
            double sumPieces = 0;
            double sumLoyer = 0;
            int count = 0;
            for (Offre offre : offresFavorites) {
                try {
                    sumSuperficie += Double.parseDouble(offre.getSuperficie());
                } catch (NumberFormatException e) {
                    // Ignore si la conversion échoue
                }
                sumPieces += offre.getPieces();
                sumLoyer += offre.getLoyer();
                count++;
            }
            double avgSuperficie = (count > 0) ? sumSuperficie / count : 0;
            double avgPieces = (count > 0) ? sumPieces / count : 0;
            double avgLoyer = (count > 0) ? sumLoyer / count : 0;
            double seuilDistance = 50.0;
            for (Offre offre : offresDisponibles) {
                boolean isAlreadyFavorite = false;
                for (Offre favorite : offresFavorites) {
                    if (favorite.getDocumentId().equals(offre.getDocumentId())) {
                        isAlreadyFavorite = true;
                        break;
                    }
                }
                if (!isAlreadyFavorite) {
                    double distance = computeDistance(offre, avgSuperficie, avgPieces, avgLoyer);
                    if (distance < seuilDistance) {
                        recommandations.add(offre);
                    }
                }
            }
            return recommandations;
        }
    }
}
