package com.example.usermanagementbackend.service;

import com.example.usermanagementbackend.entity.*;
import com.example.usermanagementbackend.repository.*;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PromotionService implements IPromotionService {
    private static final Logger logger = LoggerFactory.getLogger(PromotionService.class);

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private PromotionUsageRepository promotionUsageRepository;

    @Override
    public List<Promotion> getAllPromotions() {
        List<Promotion> promotions = promotionRepository.findAll();
        promotions.forEach(promotion -> {
            Hibernate.initialize(promotion.getProduits());
            System.out.println("Promotion " + promotion.getId() + " - Produits : " + promotion.getProduits());
        });
        return promotions;
    }

    @Override
    public Optional<Promotion> getPromotionById(Integer id) {
        Optional<Promotion> promotion = promotionRepository.findById(id);
        promotion.ifPresent(p -> Hibernate.initialize(p.getProduits()));
        return promotion;
    }

    @Override
    @Transactional
    public Promotion createPromotion(Promotion promotion) {
        if (promotion.getId() != null) {
            throw new IllegalArgumentException("New promotion must not have an ID");
        }
        if (promotion.getDateDebut() == null || promotion.getDateFin() == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (promotion.getDateDebut().after(promotion.getDateFin())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (promotion.getProduits() == null) {
            promotion.setProduits(new ArrayList<>());
        }

        System.out.println("Produits associés à la promotion avant sauvegarde : " + promotion.getProduits());

        for (Produit produit : promotion.getProduits()) {
            if (produit.getPromotions() == null) {
                produit.setPromotions(new ArrayList<>());
            }
            if (!produit.getPromotions().contains(promotion)) {
                produit.getPromotions().add(promotion);
            }
        }

        Promotion savedPromotion = promotionRepository.save(promotion);
        produitRepository.saveAll(promotion.getProduits());

        Hibernate.initialize(savedPromotion.getProduits());

        System.out.println("Promotion sauvegardée : " + savedPromotion);
        System.out.println("Produits associés à la promotion sauvegardée : " + savedPromotion.getProduits());

        return savedPromotion;
    }

    @Override
    @Transactional
    public Promotion updatePromotion(Integer id, Promotion promotion) {
        return promotionRepository.findById(id).map(existing -> {
            if (promotion.getDateDebut().after(promotion.getDateFin())) {
                throw new IllegalArgumentException("End date must be after start date");
            }

            existing.setNom(promotion.getNom());
            existing.setPourcentageReduction(promotion.getPourcentageReduction());
            existing.setDateDebut(promotion.getDateDebut());
            existing.setDateFin(promotion.getDateFin());
            existing.setConditionPromotion(promotion.getConditionPromotion());
            existing.setProduits(promotion.getProduits());
            existing.setActive(promotion.isActive());

            if (existing.getProduits() != null) {
                for (Produit produit : existing.getProduits()) {
                    if (produit.getPromotions() == null) {
                        produit.setPromotions(new ArrayList<>());
                    }
                    if (!produit.getPromotions().contains(existing)) {
                        produit.getPromotions().add(existing);
                    }
                }
                produitRepository.saveAll(existing.getProduits());
            }

            Promotion updatedPromotion = promotionRepository.save(existing);
            Hibernate.initialize(updatedPromotion.getProduits());
            return updatedPromotion;
        }).orElseThrow(() -> new RuntimeException("Promotion not found"));
    }

    public Promotion toggleActiveStatus(Integer id, boolean active) {
        return promotionRepository.findById(id).map(existing -> {
            existing.setActive(active);
            Promotion updatedPromotion = promotionRepository.save(existing);
            Hibernate.initialize(updatedPromotion.getProduits());
            return updatedPromotion;
        }).orElseThrow(() -> new RuntimeException("Promotion not found"));
    }

    @Override
    public void deletePromotion(Integer id) {
        promotionRepository.deleteById(id);
    }

    public double appliquerPromotion(double montantTotal, Promotion promotion) {
        if (promotion == null || promotion.getConditionPromotion() == null || !promotion.isActive()) {
            return montantTotal;
        }

        String condition = promotion.getConditionPromotion();
        double reduction = promotion.getPourcentageReduction() / 100;
        double montantApresReduction = montantTotal;

        if ("ACHAT_GROUPE".equals(condition) && montantTotal >= 3) {
            montantApresReduction = montantTotal * (1 - reduction);
        } else if ("MONTANT_MIN".equals(condition) && montantTotal > 100) {
            montantApresReduction = montantTotal * (1 - reduction);
        } else if ("EXPIRATION_PRODUIT".equals(condition)) {
            montantApresReduction = montantTotal * (1 - reduction);
        } else if ("EXPIRATION_AND_LOW_SALES".equals(condition)) {
            montantApresReduction = montantTotal * (1 - reduction);
        }

        PromotionUsage usage = new PromotionUsage();
        usage.setPromotion(promotion);
        usage.setMontantInitial(montantTotal);
        usage.setMontantApresReduction(montantApresReduction);
        usage.setDateApplication(new Date());
        promotionUsageRepository.save(usage);

        return montantApresReduction;
    }

    public List<Promotion> getPromotionsActives() {
        List<Promotion> promotions = promotionRepository.findByActiveTrue();
        promotions.forEach(promotion -> Hibernate.initialize(promotion.getProduits()));
        return promotions;
    }

    // Méthode pour vérifier le chevauchement des intervalles
    private boolean intervalsOverlap(Date start1, Date end1, Date start2, Date end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return start1.getTime() <= end2.getTime() && start2.getTime() <= end1.getTime();
    }

    // Méthode pour vérifier si un produit est déjà dans une autre promotion active dans le même intervalle
    private boolean isProduitInOtherActivePromotion(Produit produit, Date newPromoStart, Date newPromoEnd, String excludeCondition) {
        List<Promotion> activePromotions = promotionRepository.findByActiveTrue();
        for (Promotion existingPromo : activePromotions) {
            if (excludeCondition != null && excludeCondition.equals(existingPromo.getConditionPromotion())) {
                continue;
            }
            if (intervalsOverlap(newPromoStart, newPromoEnd, existingPromo.getDateDebut(), existingPromo.getDateFin())) {
                if (produit.getPromotions().contains(existingPromo)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    @Override
    public void appliquerPromotionExpirationProduit() {
        System.out.println("Starting appliquerPromotionExpirationProduit...");
        List<Produit> produits = produitRepository.findAll();
        Date today = new Date();
        LocalDate todayLocal = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // Liste pour stocker les produits éligibles
        List<Produit> produitsEligibles = new ArrayList<>();

        for (Produit produit : produits) {
            if (produit.getDateExpiration() != null) {
                LocalDate expirationDate = LocalDate.of(
                        produit.getDateExpiration().getYear() + 1900,
                        produit.getDateExpiration().getMonth() + 1,
                        produit.getDateExpiration().getDate()
                );
                long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(todayLocal, expirationDate);

                // Critères : plus de 10 ventes et expiration dans 5 jours ou moins
                Integer salesCount = produit.getSalesCount() != null ? produit.getSalesCount() : 0;
                boolean highSales = salesCount > 10;
                boolean nearingExpiration = daysRemaining <= 5 && daysRemaining >= 0;

                if (highSales && nearingExpiration) {
                    produitsEligibles.add(produit);
                }
            }
        }

        // Créer ou mettre à jour une seule promotion pour tous les produits éligibles
        if (!produitsEligibles.isEmpty()) {
            Optional<Promotion> existingPromo = promotionRepository.findByConditionPromotionAndActiveTrue("EXPIRATION_PRODUIT");
            Promotion promo;
            Date startDate = today;
            LocalDate dateFinLocal = todayLocal.plusDays(5);
            Date endDate = Date.from(dateFinLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());

            if (existingPromo.isPresent()) {
                promo = existingPromo.get();
                promo.setPourcentageReduction(40);
                promo.setDateDebut(startDate);
                promo.setDateFin(endDate);
                promo.setActive(true);
            } else {
                promo = new Promotion();
                promo.setNom("Promotion Expiration Produit");
                promo.setPourcentageReduction(40);
                promo.setConditionPromotion("EXPIRATION_PRODUIT");
                promo.setDateDebut(startDate);
                promo.setDateFin(endDate);
                promo.setActive(true);
            }

            // Mettre à jour la liste des produits de la promotion
            promo.setProduits(new ArrayList<>());
            List<Produit> produitsToAdd = new ArrayList<>();
            for (Produit produit : produitsEligibles) {
                if (!isProduitInOtherActivePromotion(produit, startDate, endDate, "EXPIRATION_PRODUIT")) {
                    produitsToAdd.add(produit);
                } else {
                    System.out.println("Produit " + produit.getNom() + " est déjà dans une autre promotion active dans le même intervalle.");
                }
            }

            for (Produit produit : produitsToAdd) {
                appliquerPromotionSurProduit(produit, promo);
            }

            // Sauvegarder la promotion et les produits en une seule fois
            if (!produitsToAdd.isEmpty()) {
                promotionRepository.save(promo);
                produitRepository.saveAll(produitsToAdd);
            }
        }

        System.out.println("Finished appliquerPromotionExpirationProduit");
    }

    public void appliquerPromotionSurProduit(Produit produit, Promotion promo) {
        // Vérifier si le produit est déjà associé à cette promotion
        if (!produit.getPromotions().contains(promo)) {
            double prixInitial = produit.getPrix();
            double newPrice = prixInitial * (1 - promo.getPourcentageReduction() / 100);
            produit.setPrix(newPrice);

            produit.getPromotions().add(promo);
            if (!promo.getProduits().contains(produit)) {
                promo.getProduits().add(produit);
            }

            System.out.println("Produit: " + produit.getNom() + ", Prix initial: " + prixInitial + ", Prix réduit: " + newPrice);
            System.out.println("Promotion " + promo.getNom() + " appliquée au produit: " + produit.getNom());
        } else {
            System.out.println("Produit: " + produit.getNom() + " a déjà la promotion: " + promo.getNom());
        }
    }

    @Scheduled(cron = "0 0 0 25 11 ?")
    public void appliquerPromoBlackFriday() {
        Optional<Promotion> blackFridayPromoOpt = promotionRepository.findByNom("Black Friday");
        if (blackFridayPromoOpt.isPresent()) {
            Promotion blackFridayPromo = blackFridayPromoOpt.get();
            if (blackFridayPromo.isActive()) {
                List<Produit> produits = produitRepository.findAll();
                Date startDate = blackFridayPromo.getDateDebut();
                Date endDate = blackFridayPromo.getDateFin();
                for (Produit produit : produits) {
                    if (!isProduitInOtherActivePromotion(produit, startDate, endDate, "BLACK_FRIDAY")) {
                        double prixAvecReduction = produit.getPrix() * (1 - blackFridayPromo.getPourcentageReduction() / 100);
                        produit.setPrix(prixAvecReduction);
                        produit.getPromotions().add(blackFridayPromo);
                        produitRepository.save(produit);
                    }
                }
            }
        }
    }

    @Scheduled(cron = "0 0 0 28 11 ?")
    public void desactiverPromoBlackFriday() {
        Optional<Promotion> blackFridayPromoOpt = promotionRepository.findByNom("Black Friday");
        blackFridayPromoOpt.ifPresent(promo -> {
            promo.setActive(false);
            promotionRepository.save(promo);
        });
    }

    @Override
    public void bulkActivate(List<Integer> ids) {
        List<Promotion> promotions = promotionRepository.findAllById(ids);
        for (Promotion promo : promotions) {
            promo.setActive(true);
            promotionRepository.save(promo);
        }
    }

    @Override
    public void bulkDeactivate(List<Integer> ids) {
        List<Promotion> promotions = promotionRepository.findAllById(ids);
        for (Promotion promo : promotions) {
            promo.setActive(false);
            promotionRepository.save(promo);
        }
    }

    @Override
    public void bulkDelete(List<Integer> ids) {
        promotionRepository.deleteAllById(ids);
    }

    public Map<String, Object> getPromotionAnalytics() {
        List<PromotionUsage> usageList = promotionUsageRepository.findAll();
        logger.info("Nombre d'entrées dans promotion_usage: {}", usageList.size());
        Map<Integer, List<PromotionUsage>> usageByPromotion = usageList.stream()
                .collect(Collectors.groupingBy(usage -> usage.getPromotion().getId()));
        logger.info("Promotions avec utilisations (usage Furniture): {}", usageByPromotion.keySet());

        List<Promotion> activePromotions = promotionRepository.findByActiveTrue();
        logger.info("Nombre de promotions actives: {}", activePromotions.size());
        activePromotions.forEach(promo ->
                logger.info("Promotion active - ALERT: {}, Nom: {}, Active: {}", promo.getId(), promo.getNom(), promo.isActive())
        );

        Map<String, Object> analytics = new HashMap<>();
        List<Map<String, Object>> promotionStats = new ArrayList<>();

        for (Map.Entry<Integer, List<PromotionUsage>> entry : usageByPromotion.entrySet()) {
            Integer promoId = entry.getKey();
            List<PromotionUsage> usages = entry.getValue();
            Promotion promo = promotionRepository.findById(promoId).orElse(null);
            if (promo == null) {
                logger.warn("Promotion avec ID {} introuvable", promoId);
                continue;
            }

            double totalRevenueImpact = usages.stream()
                    .mapToDouble(usage -> usage.getMontantInitial() - usage.getMontantApresReduction())
                    .sum();
            long usageCount = usages.size();

            Map<String, Object> stat = new HashMap<>();
            stat.put("promotionId", promoId);
            stat.put("promotionName", promo.getNom());
            stat.put("usageCount", usageCount);
            stat.put("totalRevenueImpact", totalRevenueImpact);
            promotionStats.add(stat);
            logger.info("Stat ajoutée pour la promotion {}: usageCount={}, totalRevenueImpact={}",
                    promo.getNom(), usageCount, totalRevenueImpact);
        }

        for (Promotion promo : activePromotions) {
            boolean alreadyIncluded = promotionStats.stream()
                    .anyMatch(stat -> stat.get("promotionId").equals(promo.getId()));

            if (!alreadyIncluded) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("promotionId", promo.getId());
                stat.put("promotionName", promo.getNom());
                stat.put("usageCount", 0L);
                stat.put("totalRevenueImpact", 0.0);
                promotionStats.add(stat);
                logger.info("Promotion active mais non appliquée ajoutée - Nom: {}, ID: {}",
                        promo.getNom(), promo.getId());
            }
        }

        analytics.put("promotionStats", promotionStats);
        analytics.put("totalPromotionsApplied", usageList.size());
        logger.info("Réponse finale: {}", analytics);
        return analytics;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void suggestPromotions() {
        System.out.println("Starting suggestPromotions...");
        if (produitRepository == null) {
            throw new IllegalStateException("produitRepository is not initialized");
        }
        if (promotionRepository == null) {
            throw new IllegalStateException("promotionRepository is not initialized");
        }

        List<Produit> produits = produitRepository.findAll();
        System.out.println("Found " + produits.size() + " products");

        ZonedDateTime todayZoned = ZonedDateTime.now(ZoneId.systemDefault());
        LocalDate today = todayZoned.toLocalDate();
        System.out.println("Today's date: " + today);

        // Utiliser un Set pour éviter les doublons
        Set<Produit> produitsEligiblesSet = new HashSet<>();

        for (Produit produit : produits) {
            try {
                Hibernate.initialize(produit);
                System.out.println("Processing product: " + produit.getNom());
                Integer salesCount = produit.getSalesCount() != null ? produit.getSalesCount() : 0;
                boolean lowSales = salesCount < 10;
                System.out.println("Product: " + produit.getNom() + ", salesCount: " + salesCount + ", lowSales: " + lowSales);

                boolean nearingExpiration = false;
                if (produit.getDateExpiration() != null) {
                    LocalDate expirationDate;
                    if (produit.getDateExpiration() instanceof java.sql.Date) {
                        expirationDate = ((java.sql.Date) produit.getDateExpiration()).toLocalDate();
                    } else {
                        expirationDate = produit.getDateExpiration().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                    }
                    long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, expirationDate);
                    nearingExpiration = daysRemaining <= 10 && daysRemaining >= 0;
                    System.out.println("Product: " + produit.getNom() + ", daysRemaining: " + daysRemaining + ", nearingExpiration: " + nearingExpiration);
                } else {
                    System.out.println("Product: " + produit.getNom() + ", dateExpiration is null");
                }

                // Critères : moins de 10 ventes et expiration dans 10 jours ou moins
                if (lowSales && nearingExpiration) {
                    produitsEligiblesSet.add(produit);
                }
            } catch (Exception e) {
                System.err.println("Error processing product " + produit.getNom() + ": " + e.getClass().getName());
                e.printStackTrace();
                throw new RuntimeException("Failed to process product " + produit.getNom(), e);
            }
        }

        List<Produit> produitsEligibles = new ArrayList<>(produitsEligiblesSet);

        // Désactiver les anciennes promotions EXPIRATION_AND_LOW_SALES
        List<Promotion> oldLowSalesPromos = promotionRepository.findAllByConditionPromotionAndActiveTrue("EXPIRATION_AND_LOW_SALES");
        if (!oldLowSalesPromos.isEmpty()) {
            for (Promotion oldPromo : oldLowSalesPromos) {
                oldPromo.setActive(false);
                // Retirer les produits de l'ancienne promotion
                for (Produit produit : oldPromo.getProduits()) {
                    produit.getPromotions().remove(oldPromo);
                }
                oldPromo.setProduits(new ArrayList<>());
                promotionRepository.save(oldPromo);
                produitRepository.saveAll(oldPromo.getProduits());
            }
        }

        // Créer ou mettre à jour une seule promotion pour tous les produits éligibles
        if (!produitsEligibles.isEmpty()) {
            Promotion promo = new Promotion();
            promo.setNom("AI Suggested Promotion for Low Sales and Expiring Products");
            promo.setPourcentageReduction(45);
            promo.setConditionPromotion("EXPIRATION_AND_LOW_SALES");
            Date startDate = Date.from(todayZoned.toInstant());
            LocalDate dateFinLocal = today.plusDays(7);
            Date endDate = Date.from(dateFinLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
            promo.setDateDebut(startDate);
            promo.setDateFin(endDate);
            promo.setActive(true);

            // Mettre à jour les relations
            promo.setProduits(new ArrayList<>());
            List<Produit> produitsToAdd = new ArrayList<>();
            for (Produit produit : produitsEligibles) {
                if (!isProduitInOtherActivePromotion(produit, startDate, endDate, "EXPIRATION_AND_LOW_SALES")) {
                    produitsToAdd.add(produit);
                } else {
                    System.out.println("Produit " + produit.getNom() + " est déjà dans une autre promotion active dans le même intervalle.");
                }
            }

            for (Produit produit : produitsToAdd) {
                appliquerPromotionSurProduit(produit, promo);
            }

            // Sauvegarder la promotion et les produits en une seule fois
            if (!produitsToAdd.isEmpty()) {
                promotionRepository.save(promo);
                produitRepository.saveAll(produitsToAdd);
            }
        }

        System.out.println("Finished suggestPromotions");
    }

    @Override
    public List<Produit> getProduitsProchesExpiration() {
        Date today = new Date();
        LocalDate todayLocal = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDateLocal = todayLocal.plusDays(5);
        Date endDate = Date.from(endDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Produit> produitsProchesExpiration = produitRepository.findProduitsProchesExpiration(today, endDate);
        return produitsProchesExpiration;
    }

    @Override
    public Map<String, List<Map<String, Object>>> getDynamicPromotions() {
        Map<String, List<Map<String, Object>>> dynamicPromotions = new HashMap<>();

        // Récupérer la promotion Black Friday
        List<Map<String, Object>> blackFridayPromotions = new ArrayList<>();
        Optional<Promotion> blackFridayPromoOpt = promotionRepository.findByNom("Black Friday");
        blackFridayPromoOpt.ifPresent(promo -> {
            Hibernate.initialize(promo.getProduits());
            Map<String, Object> promoMap = new HashMap<>();
            promoMap.put("id", promo.getId());
            promoMap.put("nom", promo.getNom());
            promoMap.put("pourcentage_reduction", promo.getPourcentageReduction());
            promoMap.put("date_debut", promo.getDateDebut());
            promoMap.put("date_fin", promo.getDateFin());
            promoMap.put("condition_promotion", promo.getConditionPromotion());
            promoMap.put("active", promo.isActive());

            // Dédupliquer les produits par ID
            Set<Long> produitIds = new HashSet<>();
            List<Produit> uniqueProduits = promo.getProduits().stream()
                    .filter(produit -> produitIds.add(produit.getId()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> produitsList = new ArrayList<>();
            for (Produit produit : uniqueProduits) {
                Map<String, Object> produitMap = new HashMap<>();
                produitMap.put("id", produit.getId());
                produitMap.put("nom", produit.getNom());
                produitMap.put("prix", produit.getPrix());
                produitMap.put("prix_reduit", produit.getPrix() * (1 - promo.getPourcentageReduction() / 100.0));
                produitMap.put("devise", produit.getDevise() != null ? produit.getDevise() : "TND");
                produitMap.put("date_expiration", produit.getDateExpiration());
                produitMap.put("promotions", produit.getPromotions());
                produitsList.add(produitMap);
            }
            promoMap.put("produits", produitsList);

            if (!promo.isActive()) {
                LocalDate scheduledDate = LocalDate.of(LocalDate.now().getYear(), 11, 25);
                promoMap.put("date_activation_prevue", scheduledDate.toString());
            }

            blackFridayPromotions.add(promoMap);
        });

        if (blackFridayPromotions.isEmpty()) {
            Map<String, Object> placeholderPromo = new HashMap<>();
            placeholderPromo.put("nom", "Black Friday");
            placeholderPromo.put("pourcentage_reduction", 50.0);
            placeholderPromo.put("condition_promotion", "BLACK_FRIDAY");
            placeholderPromo.put("active", false);
            placeholderPromo.put("produits", new ArrayList<>());

            LocalDate scheduledDate = LocalDate.of(LocalDate.now().getYear(), 11, 25);
            placeholderPromo.put("date_debut", Date.from(scheduledDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            placeholderPromo.put("date_fin", Date.from(scheduledDate.plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            placeholderPromo.put("date_activation_prevue", scheduledDate.toString());

            blackFridayPromotions.add(placeholderPromo);
        }

        // Récupérer la promotion pour produits proches de l'expiration
        List<Map<String, Object>> expirationPromotions = new ArrayList<>();
        Optional<Promotion> expirationPromoOpt = promotionRepository.findByConditionPromotionAndActiveTrue("EXPIRATION_PRODUIT");
        expirationPromoOpt.ifPresent(promo -> {
            Hibernate.initialize(promo.getProduits());
            Map<String, Object> promoMap = new HashMap<>();
            promoMap.put("id", promo.getId());
            promoMap.put("nom", promo.getNom());
            promoMap.put("pourcentage_reduction", promo.getPourcentageReduction());
            promoMap.put("date_debut", promo.getDateDebut());
            promoMap.put("date_fin", promo.getDateFin());
            promoMap.put("condition_promotion", promo.getConditionPromotion());
            promoMap.put("active", promo.isActive());

            // Dédupliquer les produits par ID
            Set<Long> produitIds = new HashSet<>();
            List<Produit> uniqueProduits = promo.getProduits().stream()
                    .filter(produit -> produitIds.add(produit.getId()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> produitsList = new ArrayList<>();
            for (Produit produit : uniqueProduits) {
                Map<String, Object> produitMap = new HashMap<>();
                produitMap.put("id", produit.getId());
                produitMap.put("nom", produit.getNom());
                produitMap.put("prix", produit.getPrix());
                produitMap.put("prix_reduit", produit.getPrix() * (1 - promo.getPourcentageReduction() / 100.0));
                produitMap.put("devise", produit.getDevise() != null ? produit.getDevise() : "TND");
                produitMap.put("date_expiration", produit.getDateExpiration());
                produitMap.put("promotions", produit.getPromotions());
                produitsList.add(produitMap);
            }
            promoMap.put("produits", produitsList);

            expirationPromotions.add(promoMap);
        });

        // Récupérer les promotions pour faibles ventes ou expiration + faibles ventes
        List<Map<String, Object>> lowSalesPromotions = new ArrayList<>();
        List<Promotion> lowSalesPromos = promotionRepository.findByConditionPromotionInAndActiveTrue(
                List.of("MONTANT_MIN", "EXPIRATION_AND_LOW_SALES"));
        for (Promotion promo : lowSalesPromos) {
            Hibernate.initialize(promo.getProduits());
            Map<String, Object> promoMap = new HashMap<>();
            promoMap.put("id", promo.getId());
            promoMap.put("nom", promo.getNom());
            promoMap.put("pourcentage_reduction", promo.getPourcentageReduction());
            promoMap.put("date_debut", promo.getDateDebut());
            promoMap.put("date_fin", promo.getDateFin());
            promoMap.put("condition_promotion", promo.getConditionPromotion());
            promoMap.put("active", promo.isActive());

            // Dédupliquer les produits par ID
            Set<Long> produitIds = new HashSet<>();
            List<Produit> uniqueProduits = promo.getProduits().stream()
                    .filter(produit -> produitIds.add(produit.getId()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> produitsList = new ArrayList<>();
            for (Produit produit : uniqueProduits) {
                Map<String, Object> produitMap = new HashMap<>();
                produitMap.put("id", produit.getId());
                produitMap.put("nom", produit.getNom());
                produitMap.put("prix", produit.getPrix());
                produitMap.put("prix_reduit", produit.getPrix() * (1 - promo.getPourcentageReduction() / 100.0));
                produitMap.put("devise", produit.getDevise() != null ? produit.getDevise() : "TND");
                produitMap.put("date_expiration", produit.getDateExpiration());
                produitMap.put("promotions", produit.getPromotions());
                produitsList.add(produitMap);
            }
            promoMap.put("produits", produitsList);

            lowSalesPromotions.add(promoMap);
        }

        dynamicPromotions.put("blackFriday", blackFridayPromotions);
        dynamicPromotions.put("expiration", expirationPromotions);
        dynamicPromotions.put("lowSales", lowSalesPromotions);

        return dynamicPromotions;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Override
    public void verifierPromotionsActives() {
        List<Promotion> promotions = promotionRepository.findAll();
        Date today = new Date();

        for (Promotion promo : promotions) {
            // Désactiver les promotions expirées
            if (promo.getDateFin() != null && promo.getDateFin().before(today)) {
                promo.setActive(false);
                promotionRepository.save(promo);
            }
            // Désactiver les promotions sans produits
            if (promo.isActive() && (promo.getProduits() == null || promo.getProduits().isEmpty())) {
                promo.setActive(false);
                promotionRepository.save(promo);
                System.out.println("Promotion " + promo.getNom() + " désactivée car elle n'a plus de produits associés.");
            }
        }
    }
}