package com.seventest.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.types.File;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.UploadFileConfig;
import com.seventest.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@RequiredArgsConstructor
public class CourseMaterialManager {
    private final AppProperties properties;
    private final ResourceLoader resourceLoader;
    private Client client;
    private File remoteFile;
    private Path localCopy;

    public synchronized Client client() {
        if (client == null) {
            client = Client.builder()
                    .apiKey(properties.getAiGrading().getApiKey())
                    .httpOptions(HttpOptions.builder()
                            .timeout(90)
                            .retryOptions(HttpRetryOptions.builder()
                                    .attempts(3)
                                    .httpStatusCodes(408, 429, 500, 502, 503, 504)
                                    .build())
                            .build())
                    .build();
        }
        return client;
    }

    public synchronized File materialFile() {
        if (!properties.getAiGrading().isReady()) {
            throw new IllegalStateException("Gemini no esta configurado");
        }
        if (remoteFile != null) {
            try {
                return client().files.get(remoteFile.name().orElseThrow(), null);
            } catch (Exception ignored) {
                remoteFile = null;
            }
        }
        remoteFile = client().files.upload(localMaterial().toString(),
                UploadFileConfig.builder().mimeType("application/pdf").build());
        return remoteFile;
    }

    public synchronized void invalidateRemoteFile() {
        remoteFile = null;
    }

    private Path localMaterial() {
        if (localCopy != null && Files.exists(localCopy)) return localCopy;
        Resource resource = resourceLoader.getResource(properties.getAiGrading().getMaterialResource());
        try {
            localCopy = Files.createTempFile("7test-course-material-", ".pdf");
            localCopy.toFile().deleteOnExit();
            try (var input = resource.getInputStream()) {
                Files.copy(input, localCopy, StandardCopyOption.REPLACE_EXISTING);
            }
            return localCopy;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo preparar el material oficial", ex);
        }
    }
}
