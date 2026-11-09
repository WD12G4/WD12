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
import java.time.format.DateTimeFormatter;

/**
 * Événement d'arrêt de processus.
 */
@Entity
@Table(name = "killevents",
        uniqueConstraints = @UniqueConstraint(name = "uk__killevent__process_name__uc__kill_date_time",
                columnNames = {"process_name", "uc_id", "kill_date_time"}))
@EqualsAndHashCode(callSuper = false, of = {"processName", "uc", "killDateTime"})
public class KillEvent extends AbstractPersistableWithIdSetter<Long> {

    @Getter
    @Setter
    @NotBlank(message = "Le nom du processus doit être renseigné")
    @Size(max = 100, message = "la taille du nom du processus ne doit pas dépasser 100 caractères")
    @Column(name = "process_name", nullable = false, length = 100)
    private String processName;

    /**
     * Poste où le processus tournait avant d'être arrêté.
     */
    @Getter
    @Setter
    @NotNull(message = "Le poste doit être renseigné")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uc_id", nullable = false, foreignKey = @ForeignKey(name = "fk__killevents__materials"))
    private Material uc;

    @Getter
    @Setter
    @NotNull(message = "L'heure et jour de l'arrêt du processus doit être renseignée")
    @Column(name = "kill_date_time", nullable = false)
    private LocalDateTime killDateTime;

    @Override
    public String toString() {
        return "KillEvent{" +
                "processName='" + processName + '\'' +
                ", uc_SN=" + (uc != null ? uc.getSerialNumber() : "NONE") +
                ", killDateTime=" + (killDateTime != null ? killDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "NONE") +
                '}';
    }
}
