package com.seventest.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private AiGrading aiGrading = new AiGrading();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class Security {
        private int maxLoginAttempts = 5;
        private int lockoutDurationMinutes = 15;
    }

    @Getter
    @Setter
    public static class AiGrading {
        private boolean enabled;
        private String apiKey = "";
        private String vipTeacherEmail = "pfarias@uade.edu.ar";
        private String model = "gpt-5.4-mini";
        private String materialVersion = "testing-apps-2026-06-10-v1";
        private String materialResource = "classpath:course-material/Todo_Testing_de_Apps.pdf";
        private String materialSha256;
        private String promptVersion = "testing-grading-v2";
        private String promptResource = "classpath:ai-grading/testing-grading-v2.txt";
        private int maxRelevantPages = 8;
        private int maxCharactersPerPage = 6000;

        public boolean isReady() {
            return enabled && apiKey != null && !apiKey.isBlank();
        }
    }
}
