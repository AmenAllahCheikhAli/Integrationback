package com.example.usermanagementbackend.controller;


import com.example.usermanagementbackend.entity.Don;
import com.example.usermanagementbackend.service.IService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin
@RestController
@AllArgsConstructor
@RequestMapping("/don")
public class RestControllerDon {
    @Autowired
    private IService donService;

    @PostMapping
    public Don addDon(@RequestBody Don don) {
        return donService.addDon(don);
    }

    @PutMapping("/{id}")
    public Don updateDon(@PathVariable Long id, @RequestBody Don don) {
        return donService.updateDon(id, don);
    }

    @DeleteMapping("/{id}")
    public void deleteDon(@PathVariable Long id) {
        donService.deleteDon(id);
    }

    @GetMapping("/{id}")
    public Don getDonById(@PathVariable Long id) {
        return donService.getDonById(id);
    }

    @GetMapping
    public List<Don> getAllDons() {
        return donService.getAllDons();
    }
}