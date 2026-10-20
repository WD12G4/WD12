package net.ent.etnc.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.models.commons.AbstractPersistableWithIdSetter;

@Entity
@Table(name = "buildings",
        uniqueConstraints = @UniqueConstraint(name = "uk__building__number",
                columnNames = "number"))
@EqualsAndHashCode(callSuper = false, of = "number")
@ToString(callSuper = true, of = "number")
public class Building extends AbstractPersistableWithIdSetter<Long> {

    @Getter
    @Setter
    @NotNull(message = "le numéro de bâtiment doit être renseigné")
    @Min(value = 0, message = "le numéro de bâtiment doit être positif")
    @Max(value = 999, message = "Le numéro de bâtiment doit être inférieur à 999")
    private Integer number;
}
