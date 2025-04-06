package com.example.locationf.activities;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.locationf.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Authentification extends AppCompatActivity implements View.OnClickListener {

    // ====== Field Declarations ======
    ConstraintLayout layout1;
    ConstraintLayout layout2;
    ConstraintLayout layout3;

    TextView userinfo; //donc pour afficher un message
    EditText login;
    EditText password;
    EditText mdp;
    EditText email;
    EditText nom;
    EditText prenom;
    EditText pays;
    EditText adresse;
    EditText phone;
    EditText ville;
    RadioButton agentRadio;
    RadioButton clientRadio;
    RadioGroup radioGroup;

    Button buttonAuth;
    Button buttonCreer;//boutton d'inscription
    Button buttonConfirmer;//boutton de confirmation
    Button buttonAnnuler;


    String typeUser ; // true for agent, false for client

    private FirebaseAuth mAuth;
    FirebaseFirestore db;
    String nomUser;
    String prenomUser;

    // ====== START: onCreate method ======
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Enable Edge-to-Edge display and set layout
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_authentification);

        // Bind views using your attribute names
        layout1 = findViewById(R.id.layout1);
        layout2 = findViewById(R.id.layout2);
        layout3 = findViewById(R.id.layout3);

        userinfo = findViewById(R.id.userTextView);
        email = findViewById(R.id.emailEditText);
        password=findViewById(R.id.password);
        nom = findViewById(R.id.nomEditText);
        prenom = findViewById(R.id.prenomEditText);
        ville = findViewById(R.id.villeEditText);
        pays = findViewById(R.id.paysEditText);
        phone = findViewById(R.id.phoneEditText);

        buttonAuth = findViewById(R.id.buttonauth);
        buttonCreer = findViewById(R.id.buttonInscription);
        buttonConfirmer = findViewById(R.id.buttonconfirmation);
        buttonAnnuler = findViewById(R.id.buttonAnnuler);

        login = findViewById(R.id.login);
        mdp = findViewById(R.id.mdp);
        agentRadio = findViewById(R.id.agentRadio);
        agentRadio.setChecked(true);
        clientRadio = findViewById(R.id.clientRadio);
        radioGroup = findViewById(R.id.typeuserRadioGroup);

        // Set click listeners for buttons
        buttonAuth.setOnClickListener(this);
        buttonCreer.setOnClickListener(this);
        buttonConfirmer.setOnClickListener(this);
        buttonAnnuler.setOnClickListener(this);


        // (Optional) Set listener for radio group changes if needed
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (clientRadio.isChecked())
                typeUser=clientRadio.getText().toString();
            else if (agentRadio.isChecked())
                typeUser=agentRadio.getText().toString();
        });

        // Configure Edge-to-Edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    // ====== END: onCreate method ======

    // ====== START: onStart method ======
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        //FirebaseUser currentUser = mAuth.getCurrentUser();
        //updateUI(currentUser);
    }
    // ====== END: onStart method ======

    // ====== START: onClick method ======
    @Override
    public void onClick(View v) {
        if (v.getId() == buttonCreer.getId()) {//boutton d'inscription
            // Show account creation layout
            layout1.setVisibility(View.GONE);
            layout2.setVisibility(View.VISIBLE);
        } else if (v.getId() == buttonAuth.getId()) {
            // Modified login handling
            String email = login.getText().toString().trim();
            String password = mdp.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Email/mot de passe requis", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Fetch user role from Firestore
                                db.collection("users").document(user.getEmail())
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                String role = documentSnapshot.getString("role");
                                                if (role != null) {
                                                    if (role.equals("Agent")) {
                                                        // Redirect to OffreActivity for agents
                                                        Intent intent = new Intent(Authentification.this, OffreActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    } else if (role.equals("Client")) {
                                                        // Redirect to ClientOffersActivity for clients
                                                        Intent intent = new Intent(Authentification.this, ClientOffersActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                } else {
                                                    Toast.makeText(this, "Role not found", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to fetch user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            String error = "Connexion echouee: " + task.getException().getMessage();
                            Toast.makeText(Authentification.this, error, Toast.LENGTH_SHORT).show();
                            Log.e("AUTH", "Login error", task.getException());
                        }
                    });


        } else if (v.getId() == buttonConfirmer.getId()) {
            // Create new user account
            mAuth.createUserWithEmailAndPassword(email.getText().toString(),password.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                updateUI(user);
                            } else {
                                Log.w(TAG, "signInWithCustomToken:failure", task.getException());
                                updateUI(null);
                            }
                        }
                    });
            typeUser="";

            if (agentRadio.isChecked()) {
                typeUser = agentRadio.getText().toString();
            }else {
                typeUser=clientRadio.getText().toString();
            }
            // Create Firestore document for the new user
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("nom", nom.getText().toString());
            userMap.put("password",password.getText().toString());
            userMap.put("prenom", prenom.getText().toString());
            userMap.put("ville", ville.getText().toString());
            userMap.put("pays", pays.getText().toString());
            userMap.put("email", email.getText().toString());
            userMap.put("telephone", phone.getText().toString());
            userMap.put("role",typeUser);

            String userEmail=email.getText().toString();
            DocumentReference userRef= db.collection("users").document(userEmail); // c'est pour preciser la reference ou comme appelée dans les bases SQL ID.
            userRef.set(userMap).addOnSuccessListener(aVoid -> { Log.d(TAG, "Utilisateur ajouté dans 'users' avec succes !");});

        } else if (v.getId() == buttonAnnuler.getId()) {
            // Cancel account creation: revert to initial layout
            layout1.setVisibility(View.VISIBLE);
            layout2.setVisibility(View.GONE);
            layout3.setVisibility(View.GONE);
        }
    }
    // ====== END: onClick method ======

    // ====== START: updateUI method ======
    void updateUI(FirebaseUser currentUser) {
        if (currentUser != null) {
            // Directly redirect if user exists (for auto-login on app start)
            Intent intent = new Intent(Authentification.this, OffreActivity.class);
            startActivity(intent);
            finish();
        } else {
            layout1.setVisibility(View.VISIBLE);
            layout2.setVisibility(View.GONE);
            layout3.setVisibility(View.GONE);
        }
    }
    // ====== END: updateUI method ======

} // End of Authentification class


