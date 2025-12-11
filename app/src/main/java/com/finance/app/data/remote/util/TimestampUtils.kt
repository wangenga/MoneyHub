package com.finance.app.data.remote.util

import com.google.firebase.Timestamp

/**
 * Utility functions for converting between Firebase Timestamp and Long
 */
object TimestampUtils {
    
    /**
     * Convert Firebase Timestamp to Long (milliseconds since epoch)
     */
    fun timestampToLong(timestamp: Timestamp): Long {
        return timestamp.toDate().time
    }
    
    /**
     * Convert Long (milliseconds since epoch) to Firebase Timestamp
     */
    fun longToTimestamp(millis: Long): Timestamp {
        return Timestamp(millis / 1000, ((millis % 1000) * 1000000).toInt())
    }
    
    /**
     * Get current timestamp
     */
    fun now(): Timestamp {
        return Timestamp.now()
    }
}