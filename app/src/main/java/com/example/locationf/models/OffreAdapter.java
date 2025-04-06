package com.example.locationf.models;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.locationf.R;
import com.example.locationf.activities.ClientOffersActivity;
import com.example.locationf.activities.DemandeActivity;
import com.example.locationf.activities.SendDemandeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OffreAdapter extends RecyclerView.Adapter<OffreAdapter.OffreViewHolder> {

    private List<Offre> offreList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userEmail;
    private Context context;
    private boolean isClient; // New: Flag to check if the user is a client
    private Map<String, Boolean> favorisMap = new HashMap<>();


    public OffreAdapter(List<Offre> offreList, Context context) { // Updated: Added isClient parameter
        this.offreList = offreList;
        this.context = context;
        //this.isClient = isClient; // New: Set the flag
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null) {
            db.collection("users").document(user.getEmail())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if(documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            this.isClient = "Client".equals(role);
                            if(isClient) chargerFavorisInitiaux();
                        }
                    });
        }
    }

    private void chargerFavorisInitiaux() {
        FirebaseUser user = mAuth.getCurrentUser();
        db.collection("users").document(user.getEmail()).collection("favoris")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        favorisMap.put(document.getId(), true);
                    }
                    notifyDataSetChanged();
                });
    }

    @NonNull
    @Override
    public OffreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_offre, parent, false); // le layout item_offre nous indique la structure que va prendre un item
        return new OffreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OffreViewHolder holder, int position) {
        Offre offre = offreList.get(position);

        // Bind data to views
        holder.titreText.setText(offre.getTitre());
        holder.descriptionText.setText(offre.getDescription());
        holder.superficieText.setText("Superficie: " + offre.getSuperficie());
        holder.piecesText.setText("Pieces: " + offre.getPieces());
        holder.loyerText.setText("Loyer: " + offre.getLoyer() + " €");

        // Modified image loading section
        if (offre.getPhotoNames() != null && !offre.getPhotoNames().isEmpty()) {
            String firstPhotoName = offre.getPhotoNames().get(0);  // Get first photo name
            File imgFile = new File(context.getExternalFilesDir(null), "images/" + firstPhotoName);

            if (imgFile.exists()) {
                Glide.with(holder.itemView.getContext())
                        .load(imgFile)
                        .into(holder.imageViewPhoto);
            } else {
                Log.e("LoadImage", "File not found: " + imgFile.getAbsolutePath());
                holder.imageViewPhoto.setImageResource(R.drawable.placeholder_image_foreground);
            }
        } else {
            holder.imageViewPhoto.setImageResource(R.drawable.placeholder_image_background);
        }

        if (isClient) {
            holder.favorisButton.setVisibility(View.VISIBLE);
            boolean isFavorite = favorisMap.containsKey(offre.getDocumentId());
            holder.favorisButton.setImageResource(isFavorite ?
                    R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);

            holder.favorisButton.setOnClickListener(v -> toggleFavoris(offre, holder.favorisButton));
        } else {
            holder.favorisButton.setVisibility(View.GONE);
        }

        // Show/hide buttons based on user type
        if (isClient) { // Updated: Use isClient flag instead of checking context
            // Hide Delete and Demandes buttons for clients
            holder.deleteButton.setVisibility(View.GONE);
            holder.demandesButton.setVisibility(View.GONE);
            holder.messageButton.setVisibility(View.VISIBLE); // Show message button

            // Set click listener for the message button
            holder.messageButton.setOnClickListener(v -> {
                Log.d("OffreAdapter", "Envoyer un message button clicked for offer: " + offre.getTitre());
                sendMessage(offre);
            });
        } else {
            // Show Delete and Demandes buttons for agents
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.demandesButton.setVisibility(View.VISIBLE);
            holder.messageButton.setVisibility(View.GONE); // Hide message button

            // Set click listeners for Delete and Demandes buttons
            holder.deleteButton.setOnClickListener(v -> {
                // Confirm deletion
                new AlertDialog.Builder(context)
                        .setTitle("Delete Offer")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            deleteOffer(offre, position);
                        })
                        .setNegativeButton("No", null)
                        .show();
            });

            holder.demandesButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, DemandeActivity.class);
                intent.putExtra("OFFER_ID", offre.getDocumentId());
                context.startActivity(intent);
            });
        }

    }

    private void toggleFavoris(Offre offre, ImageButton favorisButton) {
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null) {
            Toast.makeText(context, "Connectez-vous pour utiliser les favoris", Toast.LENGTH_SHORT).show();
            return;
        }
        String docId = offre.getDocumentId();
        DocumentReference favRef = db.collection("users").document(user.getEmail())
                .collection("favoris").document(docId);

        if(favorisMap.containsKey(docId)) {
            // Supprimer des favoris
            favRef.delete().addOnSuccessListener(aVoid -> {
                favorisMap.remove(docId);
                favorisButton.setImageResource(R.drawable.ic_favorite_border);
                Toast.makeText(context, "Retiré des favoris", Toast.LENGTH_SHORT).show();
            });
        } else {
            // Ajouter aux favoris
            Map<String, Object> favData = new HashMap<>();
            favData.put("emailAgent", offre.getEmailAgent());
            favData.put("titre", offre.getTitre());
            favData.put("description", offre.getDescription());
            favData.put("superficie", offre.getSuperficie());
            favData.put("pieces", offre.getPieces());
            favData.put("loyer", offre.getLoyer());
            favData.put("timestamp", FieldValue.serverTimestamp());

            favRef.set(favData).addOnSuccessListener(aVoid -> {
                favorisMap.put(docId, true);
                favorisButton.setImageResource(R.drawable.ic_favorite_filled);
                Toast.makeText(context, "Ajouté aux favoris", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return offreList.size();
    }

    private void deleteOffer(Offre offre, int position) {
        FirebaseUser currentUser = mAuth.getCurrentUser(); // Récupérer l'utilisateur actuel
        if (currentUser == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        userEmail = currentUser.getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(context, "Invalid user email", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(userEmail).collection("offres").document(offre.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from list and update RecyclerView
                    offreList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendMessage(Offre offre) {
        // Launch SendDemandeActivity
        Intent intent = new Intent(context, SendDemandeActivity.class);
        intent.putExtra("OFFER_ID", offre.getDocumentId()); // Pass the offer ID
        intent.putExtra("emailAgent",offre.getEmailAgent());
        context.startActivity(intent);
    }

    public void updateList(List<Offre> newList) {
        offreList = newList;
        notifyDataSetChanged();
    }




    public static class OffreViewHolder extends RecyclerView.ViewHolder {
        TextView titreText, descriptionText, superficieText, piecesText, loyerText;
        Button deleteButton, demandesButton, messageButton, addPhotosButton;
        ImageView imageViewPhoto;
        ImageButton favorisButton;


        public OffreViewHolder(@NonNull View itemView) {
            super(itemView);
            titreText = itemView.findViewById(R.id.titre_text);
            descriptionText = itemView.findViewById(R.id.description_text);
            superficieText = itemView.findViewById(R.id.superficie_text);
            piecesText = itemView.findViewById(R.id.pieces_text);
            loyerText = itemView.findViewById(R.id.loyer_text);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            demandesButton = itemView.findViewById(R.id.demandesButton);
            messageButton = itemView.findViewById(R.id.messageButton); // Bind the new button
            imageViewPhoto = itemView.findViewById(R.id.imageViewPhoto);
            favorisButton = itemView.findViewById(R.id.favorisButton); // Ajouter cet ID dans votre layout

        }
    }
}
