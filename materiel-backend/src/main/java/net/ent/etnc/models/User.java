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
import net.ent.etnc.models.enums.ERole;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk__users__username", columnNames = "username")
})
@EqualsAndHashCode(callSuper = false, of = "username")
@ToString(callSuper = true, of = {"username", "role"})
public class User extends AbstractPersistableWithIdSetter<Long> implements UserDetails {

    /**
     * Login name matching the Keycloak preferred_username.
     */
    @Getter
    @Setter
    @NotBlank
    @Size(max = 100)
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /**
     * Highest role explicitly assigned to the account.
     */
    @Getter
    @Setter
    @NotNull(message = "Le rôle doit être renseigné")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private ERole role = ERole.USER;

    /**
     * Whether the account is allowed to authenticate.
     */
    @Getter
    @Setter
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAllRoles().stream()
                .map(granted -> new SimpleGrantedAuthority("ROLE_" + granted.name()))
                .toList();
    }

    /**
     * Non utilisé : l'authentification et la gestion des mots de passe sont déléguées à Keycloak.
     */
    @Override
    public @Nullable String getPassword() {
        return null;
    }
}
