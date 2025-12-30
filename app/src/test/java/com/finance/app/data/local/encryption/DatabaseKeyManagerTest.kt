package com.finance.app.data.local.encryption

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseKeyManagerTest {

    private lateinit var context: Context
    private lateinit var keyManager: DatabaseKeyManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        keyManager = DatabaseKeyManager(context)
    }

    @Test
    fun `getDatabaseKey generates valid key`() {
        // When
        val key = keyManager.getDatabaseKey()

        // Then
        assertThat(key).isNotNull()
        assertThat(key.size).isEqualTo(32) // 256 bits
    }

    @Test
    fun `getDatabaseKey returns same key on multiple calls`() {
        // When
        val key1 = keyManager.getDatabaseKey()
        val key2 = keyManager.getDatabaseKey()

        // Then
        assertThat(key1).isEqualTo(key2)
    }

    @Test
    fun `clearKey removes stored key`() {
        // Given
        val originalKey = keyManager.getDatabaseKey()

        // When
        keyManager.clearKey()
        val newKey = keyManager.getDatabaseKey()

        // Then
        assertThat(newKey).isNotEqualTo(originalKey)
        assertThat(newKey.size).isEqualTo(32)
    }
}