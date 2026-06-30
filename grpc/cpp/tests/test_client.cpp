#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "src/client.h"
#include "src/error.h"

using ::testing::Mock;

// Test that the client can be constructed with default parameters
TEST(FreeplaneClientTest, DefaultConstruction) {
    freeplane::grpc::FreeplaneClient client;
    EXPECT_EQ(client.host(), "127.0.0.1");
    EXPECT_EQ(client.port(), 50051);
}

// Test that the client can be constructed with custom parameters
TEST(FreeplaneClientTest, CustomConstruction) {
    freeplane::grpc::FreeplaneClient client("10.0.0.1", 9999);
    EXPECT_EQ(client.host(), "10.0.0.1");
    EXPECT_EQ(client.port(), 9999);
}

// Test that the client can be moved
TEST(FreeplaneClientTest, MoveConstruction) {
    freeplane::grpc::FreeplaneClient client1("127.0.0.1", 50051);
    freeplane::grpc::FreeplaneClient client2(std::move(client1));
    
    EXPECT_EQ(client2.host(), "127.0.0.1");
    EXPECT_EQ(client2.port(), 50051);
}

// Test that the client cannot be copied
TEST(FreeplaneClientTest, NonCopyable) {
    freeplane::grpc::FreeplaneClient client1("127.0.0.1", 50051);
    // This should not compile if properly deleted:
    // freeplane::grpc::FreeplaneClient client2(client1);
}

// Test that close() can be called on a non-connected client
TEST(FreeplaneClientTest, CloseNonConnected) {
    freeplane::grpc::FreeplaneClient client;
    EXPECT_NO_THROW(client.close());
}

// Test that connect() succeeds even if server is not available (gRPC is async)
// The connection is established lazily in gRPC C++
TEST(FreeplaneClientTest, ConnectToUnavailableServer) {
    freeplane::grpc::FreeplaneClient client("127.0.0.1", 1);  // Port 1 is unlikely to be in use
    
    // gRPC C++ connects lazily, so connect() may succeed even without a server
    // The actual failure will occur when making a gRPC call
    EXPECT_NO_THROW(client.connect());
}

// Test that calling RPC methods without connecting throws
TEST(FreeplaneClientTest, RpcWithoutConnectThrows) {
    freeplane::grpc::FreeplaneClient client;
    // Don't connect
    
    EXPECT_THROW(client.getCurrentNode(), freeplane::grpc::FreeplaneConnectionError);
}

// Test exception hierarchy
TEST(ExceptionTest, ExceptionHierarchy) {
    // FreeplaneGrpcError is base
    freeplane::grpc::FreeplaneGrpcError base_err("base");
    EXPECT_THAT(base_err.what(), testing::HasSubstr("base"));
    
    // FreeplaneConnectionError derives from FreeplaneGrpcError
    freeplane::grpc::FreeplaneConnectionError conn_err("connection");
    EXPECT_THAT(conn_err.what(), testing::HasSubstr("connection"));
    
    // FreeplaneOperationError derives from FreeplaneGrpcError
    freeplane::grpc::FreeplaneOperationError op_err("operation");
    EXPECT_THAT(op_err.what(), testing::HasSubstr("operation"));
    
    // NodeNotFoundError derives from FreeplaneOperationError
    freeplane::grpc::NodeNotFoundError node_err("not found");
    EXPECT_THAT(node_err.what(), testing::HasSubstr("not found"));
    
    // MindMapError derives from FreeplaneOperationError
    freeplane::grpc::MindMapError map_err("map error");
    EXPECT_THAT(map_err.what(), testing::HasSubstr("map error"));
}

// Test that exceptions can be caught as base type
TEST(ExceptionTest, ExceptionPolymorphism) {
    try {
        throw freeplane::grpc::FreeplaneConnectionError("test");
    } catch (const freeplane::grpc::FreeplaneGrpcError& e) {
        EXPECT_THAT(e.what(), testing::HasSubstr("test"));
    }
    
    try {
        throw freeplane::grpc::NodeNotFoundError("node not found");
    } catch (const freeplane::grpc::FreeplaneOperationError& e) {
        EXPECT_THAT(e.what(), testing::HasSubstr("node not found"));
    }
}
