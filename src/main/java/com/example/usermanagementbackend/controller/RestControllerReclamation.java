package com.example.usermanagementbackend.controller;


import com.example.usermanagementbackend.entity.Don;
import com.example.usermanagementbackend.entity.Reclamation;
import com.example.usermanagementbackend.service.IService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@CrossOrigin
@RestController
@AllArgsConstructor
@RequestMapping("/reclamation")
public class RestControllerReclamation {

    @Autowired
    private IService reclamationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Reclamation addReclamation(
            @RequestParam("titre") String titre,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestParam(value = "donId", required = false) Long donId,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws IOException {
        return reclamationService.addReclamation(titre, description, type, donId, image);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Reclamation updateReclamation(
            @PathVariable Long id,
            @RequestParam("titre") String titre,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws IOException {
        return reclamationService.updateReclamation(id, titre, description, type, image);
    }
    @DeleteMapping("/{id}")
    public void deleteReclamation(@PathVariable Long id) {
        reclamationService.deleteReclamation(id);
    }

    @GetMapping("/{id}")
    public Reclamation getReclamationById(@PathVariable Long id) {
        return reclamationService.getReclamationById(id);
    }

    @GetMapping
    public List<Reclamation> getAllReclamations() {
        return reclamationService.getAllReclamations();
    }
    @PostMapping("/assign/{reclamationId}/to/{donId}")
    public Reclamation assignReclamationToDon(@PathVariable Long reclamationId, @PathVariable Long donId) {
        return reclamationService.assignReclamationToDon(reclamationId, donId);
    }

    @PostMapping("/don/{donId}/reclaim/{reclamationId}")
    public Don assignDonToReclamation(@PathVariable Long donId, @PathVariable Long reclamationId) {
        return reclamationService.assignDonToReclamation(donId, reclamationId);
    }
}