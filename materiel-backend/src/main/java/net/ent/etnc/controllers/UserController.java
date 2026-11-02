package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.assemblers.UserDtoAssembler;
import net.ent.etnc.dtos.user.UserCreateRequest;
import net.ent.etnc.dtos.user.UserResponse;
import net.ent.etnc.dtos.user.UserUpdateRequest;
import net.ent.etnc.models.User;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée REST pour la gestion des utilisateurs.
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final UserDtoAssembler userAssembler;

    @Autowired
    public UserController(UserService userService, UserDtoAssembler userAssembler) {
        this.userService = userService;
        this.userAssembler = userAssembler;
    }

    /**
     * Liste paginée des utilisateurs (réservé aux administrateurs).
     */
    @GetMapping
    public Page<UserResponse> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.findAll(pageable)
                .map(userAssembler::toDto);
    }

    /**
     * Recherche un utilisateur par son identifiant (réservé aux administrateurs).
     */
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(userAssembler::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));
    }

    /**
     * Crée un nouvel utilisateur (réservé aux administrateurs).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        User entity = userAssembler.toEntity(request);
        User saved = userService.create(entity);
        return userAssembler.toDto(saved);
    }

    /**
     * Met à jour un utilisateur existant (réservé aux administrateurs).
     */
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User existing = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));

        userAssembler.updateEntity(existing, request);
        User updated = userService.update(existing);
        return userAssembler.toDto(updated);
    }

    /**
     * Supprime un utilisateur par son identifiant (réservé aux administrateurs).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id);
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
