package com.actionanand.carechime.app

import org.junit.Assert.assertEquals
import org.junit.Test

class StarterTest {
    @Test fun stableNamespace() {
        assertEquals("com.actionanand.carechime.app", BuildConfig.APPLICATION_ID.removeSuffix(".debug"))
    }
}
