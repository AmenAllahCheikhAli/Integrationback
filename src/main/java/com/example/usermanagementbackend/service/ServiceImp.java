package com.example.usermanagementbackend.service;


import com.example.usermanagementbackend.entity.Don;
import com.example.usermanagementbackend.entity.Reclamation;
import com.example.usermanagementbackend.entity.TypeReclamation;
import com.example.usermanagementbackend.repository.DonRepository;
import com.example.usermanagementbackend.repository.ReclamationRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceImp implements IService {
    @Autowired private DonRepository donRepository;
    @Autowired private ReclamationRepo reclamationRepo;

    @Override
    public Don addDon(Don don) {
        if (don.getReclamations() != null) {
            don.getReclamations().forEach(r -> r.setDon(don));
        }
        return donRepository.save(don);
    }

    @Override
    public Don updateDon(Long id, Don don) {
        return donRepository.findById(id)
                .map(existing -> {
                    existing.setTitre(don.getTitre());
                    existing.setDescription(don.getDescription());
                    existing.setMontant(don.getMontant());
                    existing.setStatus(don.getStatus());
                    return donRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Don not found: " + id));
    }

    @Override
    public void deleteDon(Long id) {
        if (!donRepository.existsById(id)) throw new RuntimeException("Don not found: " + id);
        donRepository.deleteById(id);
    }

    @Override
    public Don getDonById(Long id) {
        return donRepository.findById(id).orElseThrow(() -> new RuntimeException("Don not found: " + id));
    }

    @Override
    public List<Don> getAllDons() {
        return donRepository.findAll();
    }

    @Override
    public Reclamation addReclamation(String titre, String description, String type, Long donId, MultipartFile image) throws IOException {
        Reclamation rec = new Reclamation();
        rec.setTitre(titre);
        rec.setDescription(description);
        rec.setType(TypeReclamation.valueOf(type));
        if (donId != null && donId > 0) {
            Don don = getDonById(donId);
            rec.setDon(don);
        }
        if (image != null && image.getSize() > 0) {
            rec.setImageData(image.getBytes());
        }
        return reclamationRepo.save(rec);
    }

    @Override
    public Reclamation updateReclamation(Long id, String titre, String description, String type, MultipartFile image) throws IOException {
        Reclamation rec = reclamationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamation not found: " + id));
        rec.setTitre(titre);
        rec.setDescription(description);
        rec.setType(TypeReclamation.valueOf(type));
        if (image != null && image.getSize() > 0) rec.setImageData(image.getBytes());
        return reclamationRepo.save(rec);
    }

    @Override
    public void deleteReclamation(Long id) {
        if (!reclamationRepo.existsById(id)) throw new RuntimeException("Reclamation not found: " + id);
        reclamationRepo.deleteById(id);
    }

    @Override
    public Reclamation getReclamationById(Long id) {
        return reclamationRepo.findById(id).orElseThrow(() -> new RuntimeException("Reclamation not found: " + id));
    }

    @Override
    public List<Reclamation> getAllReclamations() {
        return reclamationRepo.findAll();
    }

    @Override
    public Reclamation assignReclamationToDon(Long reclamationId, Long donId) {
        Reclamation rec = getReclamationById(reclamationId);
        Don don = getDonById(donId);
        rec.setDon(don);
        return reclamationRepo.save(rec);
    }

    @Override
    public Don assignDonToReclamation(Long donId, Long reclamationId) {
        Don don = getDonById(donId);
        Reclamation rec = getReclamationById(reclamationId);
        rec.setDon(don);
        reclamationRepo.save(rec);
        // ensure bi-directional
        List<Reclamation> list = don.getReclamations();
        list.add(rec);
        don.setReclamations(list);
        return donRepository.save(don);
    }
}
