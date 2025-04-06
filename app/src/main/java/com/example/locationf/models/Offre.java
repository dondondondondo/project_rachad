package com.example.locationf.models;

import java.util.ArrayList;
import java.util.List;

public class Offre {
    private String documentId;
    private String emailAgent;
    private List<String> photoNames;
    private String titre;
    private String description;
    private String superficie;
    private int pieces;
    private Double loyer;

    // Constructor with parameters
    public Offre(String titre, String description, String superficie,
                 int pieces, Double loyer, String emailAgent) {
        this.titre = titre;
        this.description = description;
        this.superficie = superficie;
        this.pieces = pieces;
        this.loyer = loyer;
        this.emailAgent = emailAgent;
        this.photoNames = new ArrayList<>(); // Initialize empty list for local image names
    }

    // Empty constructor required for Firestore
    public Offre() {}

    // Getters and setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSuperficie() { return superficie; }
    public void setSuperficie(String superficie) { this.superficie = superficie; }

    public int getPieces() { return pieces; }
    public void setPieces(int pieces) { this.pieces = pieces; }

    public Double getLoyer() { return loyer; }
    public void setLoyer(Double loyer) { this.loyer = loyer; }

    public String getEmailAgent() { return emailAgent; }
    public void setEmailAgent(String emailAgent) { this.emailAgent = emailAgent; }

    public List<String> getPhotoNames() { return photoNames; }
    public void setPhotoNames(List<String> photoNames) { this.photoNames = photoNames; }
}