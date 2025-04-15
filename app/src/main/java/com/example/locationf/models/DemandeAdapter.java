package com.example.locationf.models;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.locationf.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DemandeAdapter extends RecyclerView.Adapter<DemandeAdapter.DemandeViewHolder> {

    private List<Demande> demandeList;
    private FirebaseFirestore db;
    private Context context;
    private FirebaseAuth mAuth;


    public DemandeAdapter(List<Demande> demandeList, Context context) {
        this.demandeList = demandeList;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.context=context;
    }

    @NonNull
    @Override
    public DemandeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_demande, parent, false);
        return new DemandeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DemandeViewHolder holder, int position) {
        Demande demande = demandeList.get(position);

        // Combine first and last name
        String fullName = demande.getNomClient() + " " + demande.getPrenomClient();
        holder.clientName.setText(fullName);
        holder.clientEmail.setText(demande.getEmailClient());
        holder.message.setText(demande.getMessage());

        holder.btnRefuser.setOnClickListener(v -> {
            deleteDemande(demande, position);
            sendRejectionEmail(demande);
        });

        // Accepter Button
        holder.btnAccepter.setOnClickListener(v -> {
            showDateTimePicker(demande);
        });
    }
    private void deleteDemande(Demande demande, int position) {
        db.collection("users").document(demande.getProfessionalEmail())
                .collection("offres").document(demande.getOfferId())
                .collection("demandes").document(demande.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    demandeList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Demande refusée", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendRejectionEmail(Demande demande) {
        String uriText = "mailto:" + Uri.encode(demande.getEmailClient()) +
                "?subject=" + Uri.encode("Demande refusée") +
                "&body=" + Uri.encode("Cher " + demande.getNomClient() + ",\n\n" +
                "Votre demande n'a pas été acceptée pour le moment. " +
                "Nous vous remercions de votre intérêt et vous encourageons à consulter " +
                "nos autres offres.\n\nCordialement,\nL'équipe Immobilier");

        Uri mailUri = Uri.parse(uriText);

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(mailUri);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Envoyer l'email..."));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, "Aucune application email installée", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDateTimePicker(Demande demande) {
        // First show date picker
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(context,
                (dateView, year, month, day) -> {
                    Calendar date = Calendar.getInstance();
                    date.set(year, month, day);

                    // Then show time picker
                    new TimePickerDialog(context,
                            (timeView, hour, minute) -> {
                                date.set(Calendar.HOUR_OF_DAY, hour);
                                date.set(Calendar.MINUTE, minute);
                                sendAcceptanceEmail(demande, date.getTime());
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true // 24-hour format
                    ).show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void sendAcceptanceEmail(Demande demande, Date visitDateTime) {
        // Format date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'à' HH'h'mm", Locale.FRENCH);
        String dateTimeStr = sdf.format(visitDateTime);

        // Build mailto URI with proper encoding
        String uriText = "mailto:" + Uri.encode(demande.getEmailClient()) +
                "?subject=" + Uri.encode("Demande acceptée - Rendez-vous") +
                "&body=" + Uri.encode(
                "Cher " + demande.getNomClient() + ",\n\n" +
                        "Votre demande a été acceptée !\n" +
                        "Nous vous proposons un rendez-vous le : " + dateTimeStr + "\n\n" +
                        "Veuillez confirmer votre disponibilité.\n\n" +
                        "Cordialement,\nL'équipe Immobilier"
        );

        Uri mailUri = Uri.parse(uriText);

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(mailUri);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Envoyer l'email..."));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, "Aucune application email installée", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateList(List<Demande> newList) {
        demandeList = newList;
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return demandeList.size();
    }

    static class DemandeViewHolder extends RecyclerView.ViewHolder {
        TextView clientName, clientEmail, message;
        Button btnRefuser, btnAccepter;

        public DemandeViewHolder(@NonNull View itemView) {
            super(itemView);
            clientName = itemView.findViewById(R.id.clientName);
            clientEmail = itemView.findViewById(R.id.clientEmail);
            message = itemView.findViewById(R.id.message);
            btnRefuser=itemView.findViewById(R.id.btnRefuser);
            btnAccepter=itemView.findViewById(R.id.btnAccepter);
        }
    }
}
