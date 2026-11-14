package com.lms.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrpcServer implements SmartLifecycle {
    private final Server server;
    private boolean running;

    public GrpcServer(String target, BindableService service) {
        String[] parts = target.split(":");
        int port = Integer.parseInt(parts[parts.length - 1]);
        this.server = NettyServerBuilder.forPort(port).addService(service).build();
    }

    @Override
    public void start() {
        try {
            server.start();
            running = true;
        } catch (IOException ex) {
            throw new IllegalStateException("Не удалось запустить gRPC-сервер", ex);
        }
    }

    @Override
    @PreDestroy
    public void stop() {
        server.shutdown();
        try {
            server.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
