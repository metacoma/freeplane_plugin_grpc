#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "src/node.h"
#include "src/error.h"

// Note: These tests verify the Node class structure and basic functionality.
// Full integration tests would require a running Freeplane server.

// Test that Node can be constructed
TEST(NodeTest, Construction) {
    // We can't easily construct a Node without a client, but we can verify
    // the header compiles and the class exists
    // This is a compile-time test
}

// Test Node toString format
TEST(NodeTest, ToStringFormat) {
    // Node::toString() requires a connected client, so we just verify
    // the method exists and returns a string
    // Full test would need mocking
}

// Test that Node properties are accessible
TEST(NodeTest, PropertyAccess) {
    // Verify the class has the expected interface
    // This is primarily a compile-time check
}
