package com.example.usermanagementbackend.repository;

import com.example.usermanagementbackend.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {
}