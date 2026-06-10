package com.seventest.infrastructure.ai;

import com.google.genai.types.File;
import com.google.genai.types.FileState;
import com.google.genai.types.FileStatus;
import com.seventest.infrastructure.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseMaterialManagerTest {
    private final CourseMaterialManager manager = new CourseMaterialManager(new AppProperties(), null);

    @Test
    void returnsImmediatelyWhenFileIsActive() {
        File active = file(FileState.Known.ACTIVE);

        File result = manager.awaitActive(active, ignored -> {
            throw new AssertionError("No debe consultar otra vez un archivo activo");
        }, () -> {
            throw new AssertionError("No debe esperar por un archivo activo");
        }, 2);

        assertSame(active, result);
    }

    @Test
    void waitsUntilProcessingFileBecomesActive() {
        File processing = file(FileState.Known.PROCESSING);
        File active = file(FileState.Known.ACTIVE);
        AtomicInteger pauses = new AtomicInteger();

        File result = manager.awaitActive(processing, ignored -> active, pauses::incrementAndGet, 2);

        assertSame(active, result);
        assertEquals(1, pauses.get());
    }

    @Test
    void refreshesFileWhenUploadResponseHasNoStateYet() {
        File pending = File.builder().name("files/material").build();
        File active = file(FileState.Known.ACTIVE);

        File result = manager.awaitActive(pending, ignored -> active, () -> {}, 2);

        assertSame(active, result);
    }

    @Test
    void rejectsFailedFileWithSafeMaterialMessage() {
        File failed = File.builder().name("files/material").state(new FileState(FileState.Known.FAILED))
                .error(FileStatus.builder().message("processing failed").build()).build();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> manager.awaitActive(failed, ignored -> failed, () -> {}, 2));

        assertTrue(error.getMessage().contains("PDF oficial"));
    }

    @Test
    void stopsWaitingAfterConfiguredAttempts() {
        File processing = file(FileState.Known.PROCESSING);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> manager.awaitActive(processing, ignored -> processing, () -> {}, 2));

        assertTrue(error.getMessage().contains("a tiempo"));
    }

    private File file(FileState.Known state) {
        return File.builder().name("files/material").state(new FileState(state)).build();
    }
}
