package com.example.usermanagementbackend.service;

import com.example.usermanagementbackend.entity.Evenement;
import com.example.usermanagementbackend.entity.User;
import com.example.usermanagementbackend.repository.EvenementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EvenementService {

    private final EvenementRepository evenementRepository;
    private final SendGridEmailService sendGridEmailService; // Service d'envoi d'email

    @Autowired
    public EvenementService(EvenementRepository evenementRepository, SendGridEmailService sendGridEmailService) {
        this.evenementRepository = evenementRepository;
        this.sendGridEmailService = sendGridEmailService;
    }

    public List<Evenement> getAllEvenements() {
        return evenementRepository.findAll();
    }

    public Optional<Evenement> getEvenementById(Long id) {
        return evenementRepository.findById(id);
    }

    public Evenement createEvenement(Evenement evenement) {
        evenement.setStatut(Evenement.StatutEvenement.PLANIFIE);
        Evenement savedEvent = evenementRepository.save(evenement);

        try {
            // Générer le corps de l'email texte
            String bodyText = genererContenuEmailTexte(savedEvent);

            // Envoyer l'email
            sendGridEmailService.envoyerEmail(
                    "aziz.souei@gmail.com", // Remplacer par ton email destinataire
                    "🌿 Nouveau Événement : " + savedEvent.getNom(),
                    bodyText
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return savedEvent;
    }

    public Evenement updateEvenement(Long id, Evenement evenementDetails) {
        return evenementRepository.findById(id)
                .map(evenement -> {
                    evenement.setNom(evenementDetails.getNom());
                    evenement.setDescription(evenementDetails.getDescription());
                    evenement.setDateDebut(evenementDetails.getDateDebut());
                    evenement.setDateFin(evenementDetails.getDateFin());
                    evenement.setLieu(evenementDetails.getLieu());
                    evenement.setCapaciteMax(evenementDetails.getCapaciteMax());
                    evenement.setOrganisateur(evenementDetails.getOrganisateur());
                    evenement.setImageUrl(evenementDetails.getImageUrl());
                    evenement.setStatut(evenementDetails.getStatut());
                    evenement.setCategories(evenementDetails.getCategories());
                    return evenementRepository.save(evenement);
                })
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
    }

    public void deleteEvenement(Long id) {
        evenementRepository.findById(id).ifPresent(evenement -> {
            // Supprimer toutes les participations liées d'abord
            if (evenement.getParticipants() != null) {
                evenement.getParticipants().forEach(user -> {
                    user.getEvenementsParticipes().remove(evenement);
                });
                evenement.getParticipants().clear(); // Vider les participants
            }

            // Supprimer l'image physique si elle existe
            if (evenement.getImageUrl() != null) {
                try {
                    Path imagePath = Paths.get("uploads", Paths.get(evenement.getImageUrl()).getFileName().toString());
                    Files.deleteIfExists(imagePath);
                } catch (IOException e) {
                    e.printStackTrace(); // Log mais ne pas bloquer
                }
            }

            evenementRepository.delete(evenement); // Maintenant, suppression OK
        });
    }



    // Générer le contenu d'email texte brut
    private String genererContenuEmailTexte(Evenement evenement) {
        return "Bonjour,\n\n" +
                "Un nouvel événement a été ajouté :\n\n" +
                "📋 Nom : " + evenement.getNom() + "\n" +
                "🗓 Date début : " + evenement.getDateDebut() + "\n" +
                "🗓 Date fin : " + evenement.getDateFin() + "\n" +
                "📍 Lieu : " + evenement.getLieu() + "\n" +
                "👥 Capacité Max : " + evenement.getCapaciteMax() + "\n" +
                "🧑‍💼 Organisateur : " + evenement.getOrganisateur() + "\n\n" +
                "Nous espérons vous voir nombreux !\n\n" +
                "---\n" +
                "Ceci est un email automatique, merci de ne pas répondre.";
    }
    // dans EvenementService
    public Set<User> getParticipants(Long evenementId) {
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new RuntimeException("Événement introuvable"));
        return evenement.getParticipants();
    }

}
