package com.seventest.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.types.File;
import com.google.genai.types.FileState;
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
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class CourseMaterialManager {
    private static final int PROCESSING_ATTEMPTS = 60;

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
                remoteFile = awaitActive(client().files.get(remoteFile.name().orElseThrow(), null));
                return remoteFile;
            } catch (Exception ignored) {
                remoteFile = null;
            }
        }
        remoteFile = client().files.upload(localMaterial().toString(),
                UploadFileConfig.builder().mimeType("application/pdf").build());
        remoteFile = awaitActive(remoteFile);
        return remoteFile;
    }

    public synchronized void invalidateRemoteFile() {
        remoteFile = null;
    }

    File awaitActive(File file) {
        return awaitActive(file, name -> client().files.get(name, null), this::pauseBeforePolling, PROCESSING_ATTEMPTS);
    }

    File awaitActive(File file, Function<String, File> refresh, Runnable pause, int maxAttempts) {
        String name = file.name().orElseThrow(() -> new IllegalStateException("Gemini no identifico el PDF oficial"));
        File current = file;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            FileState.Known state = current.state().map(FileState::knownEnum).orElse(FileState.Known.PROCESSING);
            if (state == FileState.Known.ACTIVE) return current;
            if (state == FileState.Known.FAILED) {
                String detail = current.error().flatMap(status -> status.message()).orElse("sin detalle");
                throw new IllegalStateException("Gemini no pudo procesar el PDF oficial: " + detail);
            }
            pause.run();
            current = refresh.apply(name);
        }
        throw new IllegalStateException("Gemini no termino de procesar el PDF oficial a tiempo");
    }

    private void pauseBeforePolling() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpio la preparacion del PDF oficial", ex);
        }
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
