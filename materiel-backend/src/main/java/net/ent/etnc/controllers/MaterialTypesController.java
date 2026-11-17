package net.ent.etnc.controllers;

import net.ent.etnc.dtos.material.MaterialTypeResponse;
import net.ent.etnc.models.enums.EMaterialType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/material-types/")
public class MaterialTypesController {

    /**
     * Liste des types de matériels avec code et libellé (accessible à tout utilisateur connecté :
     * le front affiche ces libellés dans la liste du matériel, sa fiche et les remontées).
     */
    @GetMapping
    public List<MaterialTypeResponse> getAll() {
        return Arrays.stream(EMaterialType.values())
                .map(MaterialTypeResponse::from)
                .toList();
    }
}
