package net.ent.etnc.controllers;

import net.ent.etnc.models.enums.EMaterialType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/material-types/")
public class MaterialTypesController {


    /**
     * Liste des types de matériels (accessible aux administrateurs et gestionnaires).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public List<String> getAll() {
        return Arrays.stream(EMaterialType.values()).map(Enum::name).toList();
    }
}
