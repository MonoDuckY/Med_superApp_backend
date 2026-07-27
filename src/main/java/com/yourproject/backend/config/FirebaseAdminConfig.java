package com.yourproject.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@ConditionalOnExpression("'${app.firebase.service-account-path:}' != ''")
public class FirebaseAdminConfig {

    @Bean
    public FirebaseApp firebaseApp(FirebaseProperties firebaseProperties) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream serviceAccount = Files.newInputStream(Path.of(firebaseProperties.getServiceAccountPath()))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(firebaseProperties.getProjectId())
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }
}
