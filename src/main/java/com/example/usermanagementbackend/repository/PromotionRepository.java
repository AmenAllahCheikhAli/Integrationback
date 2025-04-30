package com.example.usermanagementbackend.repository;

import com.example.usermanagementbackend.entity.Produit;
import com.example.usermanagementbackend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    List<Promotion> findByActiveTrue();
    Optional<Promotion> findByNom(String nom);
    Optional<Promotion> findByConditionPromotionAndActiveTrue(String condition);
    List<Promotion> findByConditionPromotionInAndActiveTrue(List<String> conditionPromotions);
    List<Promotion> findAllByConditionPromotionAndActiveTrue(String condition);
    @Query("SELECT p FROM Promotion p JOIN p.produits pr WHERE pr = :produit")
    List<Promotion> findByProduitsContaining(@Param("produit") Produit produit);
}