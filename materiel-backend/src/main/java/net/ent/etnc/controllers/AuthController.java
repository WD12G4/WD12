package net.ent.etnc.controllers;


import net.ent.etnc.dtos.assemblers.UserDtoAssembler;
import net.ent.etnc.dtos.user.UserResponse;
import net.ent.etnc.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. Login, token renewal and logout are handled by Keycloak
 * (MinDef Connect): the application only exposes the profile of the authenticated account.
 */
@RestController
@RequestMapping("/api/v1/auth/")
public class AuthController {
    /**
     * Current user lookup.
     */
    private final UserService userService;
    /**
     * User response assembler.
     */
    private final UserDtoAssembler userDtoAssembler;

    /**
     * Creates the auth controller.
     */
    @Autowired
    public AuthController(UserService userService, UserDtoAssembler userDtoAssembler) {
        this.userService = userService;
        this.userDtoAssembler = userDtoAssembler;
    }

    /**
     * Returns the authenticated user profile (account matching the Keycloak token).
     */
    @GetMapping("me/")
    public UserResponse me() {
        return userDtoAssembler.toDto(userService.currentUser());
    }
}
