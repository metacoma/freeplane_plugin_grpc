#ifndef FREEPLANE_GRPC_ERROR_H
#define FREEPLANE_GRPC_ERROR_H

#include <exception>
#include <string>

namespace freeplane {
namespace grpc {

/**
 * Base exception for all Freeplane gRPC errors.
 */
class FreeplaneGrpcError : public std::exception {
public:
    explicit FreeplaneGrpcError(const std::string& message)
        : message_(message) {}
    
    const char* what() const noexcept override {
        return message_.c_str();
    }

protected:
    std::string message_;
};

/**
 * Raised when a connection to the Freeplane gRPC server fails.
 */
class FreeplaneConnectionError : public FreeplaneGrpcError {
public:
    explicit FreeplaneConnectionError(const std::string& message)
        : FreeplaneGrpcError(message) {}
};

/**
 * Raised when a gRPC operation fails (server reported failure).
 */
class FreeplaneOperationError : public FreeplaneGrpcError {
public:
    explicit FreeplaneOperationError(const std::string& message)
        : FreeplaneGrpcError(message) {}
};

/**
 * Raised when a requested node is not found.
 */
class NodeNotFoundError : public FreeplaneOperationError {
public:
    explicit NodeNotFoundError(const std::string& message)
        : FreeplaneOperationError(message) {}
};

/**
 * Raised when a map-level operation fails.
 */
class MindMapError : public FreeplaneOperationError {
public:
    explicit MindMapError(const std::string& message)
        : FreeplaneOperationError(message) {}
};

}  // namespace grpc
}  // namespace freeplane

#endif  // FREEPLANE_GRPC_ERROR_H
