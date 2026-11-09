package net.ent.etnc.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.models.enums.EMaterialType;

import java.time.LocalDate;

/**
 * Matériel.
 */
@Entity
@Table(name = "materials",
        uniqueConstraints = @UniqueConstraint(name = "uk__material__serial_number",
                columnNames = "serial_number"))
@EqualsAndHashCode(callSuper = false, of = "serialNumber")
@ToString(callSuper = true, of = {"serialNumber"})
public class Material extends AbstractPersistableWithIdSetter<Long> {

    @Getter
    @Setter
    @NotBlank(message = "Le numéro de série doit être renseigné")
    @Size(max = 100, message = "la taille du numéro de série ne doit pas dépasser 100 caractères")
    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    @Getter
    @Setter
    @NotNull(message = "Le type de matériel doit être renseigné")
    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false, length = 30)
    private EMaterialType materialType;

    @Getter
    @Setter
    @NotNull(message = "La date de fin de disponibilité doit être renseignée")
    @Column(name = "end_availability_date", nullable = false)
    private LocalDate endAvailabilityDate;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", foreignKey = @ForeignKey(name = "fk__materials__rooms"))
    private Room room;

    @Getter
    @Setter
    @Column(name = "archived", nullable = false)
    private boolean archived = false;
}
