package com.meteorevents;

import com.meteorevents.compat.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlatformTest {

    @BeforeEach
    public void setup() {
        Platform.resetForTesting();
    }

    @Test
    public void testDetectionMethodsDoNotThrow() {
        assertDoesNotThrow(Platform::isFolia);
        assertDoesNotThrow(Platform::hasPacketEvents);
        assertDoesNotThrow(Platform::hasWorldGuard);
    }
}
