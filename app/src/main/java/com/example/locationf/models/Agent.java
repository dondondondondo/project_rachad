package com.example.locationf.models;

public class Agent {
    private String nom;
    private String prenom;
    private String ville;
    private String adresse;
    private String pays;
    private String adresseAgence;
    private String emailAgnece;
    private String telephoneAgence;

    Agent(String nom, String prenom ){
        this.nom=nom;
        //...
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public String getAdresseAgence() {
        return adresseAgence;
    }

    public void setAdresseAgence(String adresseAgence) {
        this.adresseAgence = adresseAgence;
    }

    public String getEmailAgnece() {
        return emailAgnece;
    }

    public void setEmailAgnece(String emailAgnece) {
        this.emailAgnece = emailAgnece;
    }

    public String getTelephoneAgence() {
        return telephoneAgence;
    }

    public void setTelephoneAgence(String telephoneAgence) {
        this.telephoneAgence = telephoneAgence;
    }
}
