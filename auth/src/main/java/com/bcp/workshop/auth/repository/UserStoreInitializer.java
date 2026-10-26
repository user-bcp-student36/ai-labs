package com.bcp.workshop.auth.repository;

import com.bcp.workshop.auth.security.PasswordHasher;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UserStoreInitializer {
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final String studentPassword;
    private final String trainerPassword;

    public UserStoreInitializer(UserRepository users,
                                PasswordHasher passwordHasher,
                                @ConfigProperty(name = "auth.seed.student-password") String studentPassword,
                                @ConfigProperty(name = "auth.seed.trainer-password") String trainerPassword) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.studentPassword = studentPassword;
        this.trainerPassword = trainerPassword;
    }

    void onStart(@Observes StartupEvent event) {
        users.createSchema();
        users.addIfAbsent("student", passwordHasher.hash(studentPassword), "Workshop Student", "STUDENT");
        users.addIfAbsent("trainer", passwordHasher.hash(trainerPassword), "Workshop Trainer", "TRAINER");
    }
}
