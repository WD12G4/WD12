package net.ent.etnc.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.models.commons.AbstractPersistableWithIdSetter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Constat de terrain : un numéro de série a été scanné dans une salle (PWA RECON).
 * Un seul constat par numéro et par salle : le dernier scan l'emporte. Le numéro peut être absent du parc
 * (matériel non recensé), c'est pourquoi il n'y a pas de lien vers {@link Material}.
 */
@Entity
@Table(name = "sightings",
        uniqueConstraints = @UniqueConstraint(name = "uk__sighting__serial_number__room",
                columnNames = {"serial_number", "room_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true, of = {"serialNumber", "seenAt"})
public class Sighting extends AbstractPersistableWithIdSetter<Long> {

    /**
     * Numéro de série normalisé (sans espaces autour, en majuscules).
     */
    @Getter
    @NotBlank(message = "Le numéro de série doit être renseigné")
    @Size(max = 100, message = "la taille du numéro de série ne doit pas dépasser 100 caractères")
    @Column(name = "serial_number", nullable = false, length = 100, updatable = false)
    private String serialNumber;

    /**
     * Salle où le matériel a été scanné. Supprimer la salle supprime ses constats.
     */
    @Getter
    @NotNull(message = "La salle doit être renseignée")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk__sightings__rooms"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    /**
     * Date du dernier scan.
     */
    @Getter
    @Setter
    @NotNull
    @Column(name = "seen_at", nullable = false)
    private Instant seenAt;

    /**
     * Identifiant de l'agent qui a fait le dernier scan.
     */
    @Getter
    @Setter
    @NotBlank
    @Column(name = "seen_by", nullable = false, length = 100)
    private String seenBy;

    /**
     * Nouveau constat, daté par {@link #setSeenAt(Instant)}.
     */
    public Sighting(String serialNumber, Room room) {
        this.serialNumber = serialNumber;
        this.room = room;
    }
}
