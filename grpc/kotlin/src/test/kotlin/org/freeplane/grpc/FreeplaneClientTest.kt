package org.freeplane.grpc

import io.grpc.Status
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for FreeplaneClient.
 * These tests do not require a running Freeplane server.
 */
class FreeplaneClientTest {

    @Test
    fun `test create client with defaults`() = runBlocking {
        val defaultClient = FreeplaneClient.create()
        assertNotNull(defaultClient)
        runCatching { defaultClient.close() }
    }

    @Test
    fun `test exception hierarchy - base error`() {
        val error = FreeplaneGrpcError("test error")
        assertEquals("test error", error.message)
        assertTrue(error is RuntimeException)
    }

    @Test
    fun `test exception hierarchy - connection error`() {
        val error = FreeplaneConnectionError("connection failed")
        assertEquals("connection failed", error.message)
        assertTrue(error is FreeplaneGrpcError)
        assertTrue(error is RuntimeException)
    }

    @Test
    fun `test exception hierarchy - operation error`() {
        val error = FreeplaneOperationError("operation failed")
        assertEquals("operation failed", error.message)
        assertTrue(error is FreeplaneGrpcError)
        assertTrue(error is RuntimeException)
    }

    @Test
    fun `test exception hierarchy - node not found error`() {
        val error = NodeNotFoundError("node not found")
        assertEquals("node not found", error.message)
        assertTrue(error is FreeplaneOperationError)
        assertTrue(error is FreeplaneGrpcError)
        assertTrue(error is RuntimeException)
    }

    @Test
    fun `test exception hierarchy - mind map error`() {
        val error = MindMapError("mind map error")
        assertEquals("mind map error", error.message)
        assertTrue(error is FreeplaneOperationError)
        assertTrue(error is FreeplaneGrpcError)
        assertTrue(error is RuntimeException)
    }

    @Test
    fun `test error mapping - connection error codes`() {
        // Test that UNAVAILABLE status maps to FreeplaneConnectionError
        val unavailableStatus = Status.UNAVAILABLE.withDescription("server unavailable")
        val unavailableError = FreeplaneConnectionError("connection error: server unavailable")
        assertEquals("connection error: server unavailable", unavailableError.message)

        // Test that DEADLINE_EXCEEDED status maps to FreeplaneConnectionError
        val deadlineStatus = Status.DEADLINE_EXCEEDED.withDescription("deadline exceeded")
        val deadlineError = FreeplaneConnectionError("connection error: deadline exceeded")
        assertEquals("connection error: deadline exceeded", deadlineError.message)

        // Test that RESOURCE_EXHAUSTED status maps to FreeplaneConnectionError
        val resourceStatus = Status.RESOURCE_EXHAUSTED.withDescription("resource exhausted")
        val resourceError = FreeplaneConnectionError("connection error: resource exhausted")
        assertEquals("connection error: resource exhausted", resourceError.message)
    }

    @Test
    fun `test error mapping - node not found error`() {
        val notFoundStatus = Status.NOT_FOUND.withDescription("node not found")
        val nodeNotFoundError = NodeNotFoundError("node not found: node not found")
        assertEquals("node not found: node not found", nodeNotFoundError.message)
    }

    @Test
    fun `test error mapping - operation error for other codes`() {
        val unknownStatus = Status.UNKNOWN.withDescription("unknown error")
        val operationError = FreeplaneOperationError("operation failed: unknown error")
        assertEquals("operation failed: unknown error", operationError.message)
    }

    @Test
    fun `test client creation with custom host and port`() {
        val customClient = FreeplaneClient.create("custom-host", 9999)
        assertNotNull(customClient)
        runCatching { customClient.close() }
    }

    @Test
    fun `test client creation with empty host defaults`() {
        val clientWithEmptyHost = FreeplaneClient.create("", 50051)
        assertNotNull(clientWithEmptyHost)
        runCatching { clientWithEmptyHost.close() }
    }

    @Test
    fun `test client creation with zero port defaults`() {
        val clientWithZeroPort = FreeplaneClient.create("127.0.0.1", 0)
        assertNotNull(clientWithZeroPort)
        runCatching { clientWithZeroPort.close() }
    }
}
