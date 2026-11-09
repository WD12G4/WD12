package net.ent.etnc.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.ent.etnc.models.commons.AbstractPersistableWithIdSetter;

import java.time.LocalDateTime;

/**
 * Classe représentant un signalement sur un {@link Material}.
 */
@Entity
@Getter
@Setter
@Table(name = "reports",
        uniqueConstraints = @UniqueConstraint(name = "uk__report__report_date_time__user__material",
                columnNames = {"material_id", "user_id", "report_date_time"}))
@EqualsAndHashCode(callSuper = false, of = {"material", "user", "reportDateTime"})
public class Report extends AbstractPersistableWithIdSetter<Long> {

    @NotBlank(message = "La description doit être renseignée")
    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "L'heure et jour du signalement doivent être renseignés")
    @Column(name = "report_date_time", nullable = false)
    private LocalDateTime reportDateTime;

    /**
     * Utilisateur ayant effectué le signalement.
     */
    @NotNull(message = "L'utilisateur doit être renseigné")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk__reports__users"))
    private User user;

    /**
     * Matériel concerné par le signalement.
     */
    @NotNull(message = "Le matériel doit être renseigné")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false, foreignKey = @ForeignKey(name = "fk__reports__materials"))
    private Material material;

    /**
     * Date et heure de traitement du signalement (renseigné uniquement si traité).
     */
    @Column(name = "handle_date_time")
    private LocalDateTime handleDateTime;

    @Column(name = "handled", nullable = false)
    private boolean handled = false;

    @Override
    public String toString() {
        return "Report{" +
                "id=" + getId() +
                ", description='" + description + '\'' +
                ", reportDateTime=" + reportDateTime +
                ", handled=" + handled +
                '}';
    }
}
