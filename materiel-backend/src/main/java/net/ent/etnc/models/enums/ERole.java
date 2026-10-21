package net.ent.etnc.models.enums;

import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Différents rôles des comptes.
 */
public enum ERole {

    /**
     * Rôle utilisateur, permet de gérer une salle.
     */
    USER("utilisateur"),
    /**
     * Rôle de gestionnaire.
     * <ul>
     *     <li>Possède le rôle USER</li>
     *     <li>Permet d'affecter des matériels à des salles</li>
     * </ul>
     */
    MANAGER("Gestionnaire", USER),
    /**
     * Rôle administrateur.
     * <ul>
     *     <li>Possède les rôles USER et MANAGER</li>
     *     <li>Permet de créer, modifier, supprimer des matériels</li>
     * </ul>
     */
    ADMIN("Administrateur", USER, MANAGER);


    @Getter
    private final String libelle;

    /**
     * Rôles hérités par ce rôle.
     */
    @Getter
    private final Set<ERole> parents;
    
    ERole(final String libelle, final ERole... parents) {
        this.libelle = libelle;
        this.parents = Set.of(parents);
    }

    /**
     *
     * @return ce {@link ERole} et tous les {@link ERole} parents
     */
    public Set<ERole> getAllRoles() {
        Set<ERole> roles = new LinkedHashSet<>();
        collectRoles(roles);
        return roles;
    }

    /**
     * @param role {@link ERole} recherché
     * @return true si le {@link ERole} possède le {@link ERole} recherché
     */
    public boolean hasRole(ERole role) {
        return getAllRoles().contains(role);
    }

    /**
     * Recursively adds this role and parent roles to the target set.
     */
    private void collectRoles(Set<ERole> target) {
        if (!target.add(this)) return;
        parents.forEach(parent -> parent.collectRoles(target));
    }
}
