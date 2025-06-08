package com.example.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ChatServer {
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    private Server server;
    private final int port;

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new ChatServiceImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    ChatServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }
        });
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private static class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
        @Override
        public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
            return new StreamObserver<ChatMessage>() {
                @Override
                public void onNext(ChatMessage message) {
                    logger.info("Received message from " + message.getUserId() + ": " + message.getContent());
                    // 回显消息给客户端
                    responseObserver.onNext(message);
                }

                @Override
                public void onError(Throwable t) {
                    logger.warning("Chat error: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        ChatServer server = new ChatServer(50051);
        server.start();
        server.blockUntilShutdown();
    }
} 