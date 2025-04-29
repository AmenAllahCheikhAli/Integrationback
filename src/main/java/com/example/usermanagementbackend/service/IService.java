// IService.java
package com.example.usermanagementbackend.service;

import com.example.usermanagementbackend.entity.Don;
import com.example.usermanagementbackend.entity.Reclamation;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IService {
    // Don CRUD
    Don addDon(Don don);
    Don updateDon(Long id, Don don);
    void deleteDon(Long id);
    Don getDonById(Long id);
    List<Don> getAllDons();

    // Reclamation CRUD with image
    Reclamation addReclamation(String titre, String description, String type, Long donId, MultipartFile image) throws IOException;
    Reclamation updateReclamation(Long id, String titre, String description, String type, MultipartFile image) throws IOException;
    void deleteReclamation(Long id);
    Reclamation getReclamationById(Long id);
    List<Reclamation> getAllReclamations();

    // New assignment methods
    /** Assign existing reclamation to a donation */
    Reclamation assignReclamationToDon(Long reclamationId, Long donId);

    /** Assign existing donation to a reclamation (adds listing) */
    Don assignDonToReclamation(Long donId, Long reclamationId);
}