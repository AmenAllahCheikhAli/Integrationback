package com.example.usermanagementbackend.repository;

import com.example.usermanagementbackend.entity.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReclamationRepo extends JpaRepository<Reclamation, Long> {
}
