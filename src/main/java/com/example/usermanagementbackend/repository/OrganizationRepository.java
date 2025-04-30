package com.example.usermanagementbackend.repository;
import com.example.usermanagementbackend.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
