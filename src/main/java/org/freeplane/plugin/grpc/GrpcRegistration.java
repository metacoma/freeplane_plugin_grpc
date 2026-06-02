package org.freeplane.plugin.grpc;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import org.freeplane.features.mode.ModeController;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Freeplane gRPC plugin registration.
 * <p>
 * Starts a gRPC server that exposes Freeplane mindmap operations.
 * The actual gRPC service implementation is in {@link FreeplaneGrpcService}.
 */
public class GrpcRegistration {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(GrpcRegistration.class.getName());

    private Server server;
    private String listenAddress;
    private Integer bindPort;
    private final Integer defaultPort = 50051;

    public GrpcRegistration(ModeController modeController) {
        listenAddress = System.getenv("GRPC_LISTEN_ADDR");
        String portStr = System.getenv("GRPC_LISTEN_PORT");

        bindPort = (portStr != null && !portStr.isEmpty()) ? Integer.parseInt(portStr) : defaultPort;

        if (listenAddress == null || listenAddress.isEmpty()) {
            listenAddress = "0.0.0.0";
        }

        try {
            server = NettyServerBuilder.forAddress(new InetSocketAddress(listenAddress, bindPort))
                .addService(new FreeplaneGrpcService())
                .build()
                .start();
        } catch (IOException e) {
            LOG.warning("Failed to start gRPC server: " + e.getMessage());
        }
        LOG.info("Freeplane grpc plugin loaded and listen on " + listenAddress + ":" + bindPort);
    }
}
