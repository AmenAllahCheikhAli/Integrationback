package com.example.usermanagementbackend.config;

import com.example.usermanagementbackend.entity.Organization;
import org.springframework.batch.item.ItemProcessor;

public class OrganizationProcessor implements ItemProcessor<Organization, Organization> {
    @Override
    public Organization process(Organization organization) {
        return organization;
    }
}
