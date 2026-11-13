package net.ent.etnc.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.models.commons.AbstractPersistableWithIdSetter;

/**
 * Entité représentant une application (processus) recensée dans le parc ou gérée par liste noire.
 */
@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(name = "uk__application__name",
                columnNames = "name"))
@EqualsAndHashCode(callSuper = false, of = "name")
@ToString(callSuper = true, of = {"name", "blacklisted"})
public class Application extends AbstractPersistableWithIdSetter<Long> {

    @Getter
    @Setter
    @NotBlank(message = "Le nom de l'application doit être renseigné")
    @Size(max = 150, message = "La taille du nom de l'application ne doit pas dépasser 150 caractères")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Getter
    @Setter
    @Size(max = 150, message = "Le nom d'affichage ne doit pas dépasser 150 caractères")
    @Column(name = "display_name", length = 150)
    private String displayName;

    @Getter
    @Setter
    @Column(name = "icon_base64", columnDefinition = "TEXT")
    private String iconBase64;

    @Getter
    @Setter
    @Column(name = "blacklisted", nullable = false)
    private boolean blacklisted = false;
}
