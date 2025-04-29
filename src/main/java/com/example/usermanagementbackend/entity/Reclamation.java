package com.example.usermanagementbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@Setter
@DynamicUpdate

public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;
    private String description;

    @Enumerated(EnumType.STRING)
    private TypeReclamation type;

    @Lob
    @Column(name = "image_data", columnDefinition = "BLOB")
    private byte[] imageData;

    @ManyToOne
    @JoinColumn(name = "don_id", nullable = true) // Allow null
    private Don don;
}