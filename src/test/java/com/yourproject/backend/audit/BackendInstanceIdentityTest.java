package com.yourproject.backend.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackendInstanceIdentityTest {
    @TempDir
    Path tempDirectory;

    @Test
    void instanceIdIsCreatedOnceAndReusedAcrossRestarts() throws Exception {
        Path instanceFile = tempDirectory.resolve("backend-instance-id");

        BackendInstanceIdentity first = new BackendInstanceIdentity(instanceFile.toString());
        BackendInstanceIdentity second = new BackendInstanceIdentity(instanceFile.toString());

        assertFalse(first.getInstanceId().isBlank());
        assertEquals(first.getInstanceId(), second.getInstanceId());
        assertEquals(first.getInstanceId(), Files.readString(instanceFile));
    }
}
