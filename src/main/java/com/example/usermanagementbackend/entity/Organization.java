package com.example.usermanagementbackend.entity;
import com.opencsv.bean.CsvBindByName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity

public class Organization {
    private String name;
    @Id
    private Long id;
    private String phone;
    private String email;

    public Organization() {}
    public Organization(String name, Long id, String phone, String email) {
        this.name = name;
        this.id = id;
        this.phone = phone;
        this.email = email;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }


}