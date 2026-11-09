package net.ent.etnc.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.models.commons.AbstractPersistableWithIdSetter;

@Entity
@Table(name = "rooms",
        uniqueConstraints = @UniqueConstraint(name = "uk__room__building__number",
                columnNames = {"building_id", "number"}))
@EqualsAndHashCode(callSuper = false, of = {"building", "number"})
@ToString(callSuper = true, of = {"number"})
public class Room extends AbstractPersistableWithIdSetter<Long> {

    /**
     * Bâtiment auquel est rattachée la salle.
     */
    @Getter
    @Setter
    @NotNull(message = "Le bâtiment doit être renseigné")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false, foreignKey = @ForeignKey(name = "fk__rooms__buildings"))
    private Building building;

    @Getter
    @Setter
    @NotNull(message = "le numéro de salle doit être renseigné")
    @Min(value = 0, message = "le numéro de salle doit être positif")
    @Max(value = 999, message = "Le numéro de salle doit être inférieur à 999")
    @Column(name = "number", nullable = false)
    private Integer number;
}
