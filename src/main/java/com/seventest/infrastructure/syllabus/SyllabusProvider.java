package com.seventest.infrastructure.syllabus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class SyllabusProvider {

    private final ResourceLoader resourceLoader;
    private String syllabusContent;

    public SyllabusProvider(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String getSyllabus() {
        if (syllabusContent != null) {
            return syllabusContent;
        }

        try {
            Resource resource = resourceLoader.getResource("classpath:syllabus/testing_de_aplicaciones.md");
            try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                syllabusContent = FileCopyUtils.copyToString(reader);
                log.info("Syllabus content loaded successfully from resources.");
                return syllabusContent;
            }
        } catch (Exception e) {
            log.error("Failed to load syllabus content from resources. Returning empty context.", e);
            return "";
        }
    }
}
