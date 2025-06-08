package com.example.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

public class ChatClient {
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());
    private final ManagedChannel channel;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private StreamObserver<ChatMessage> requestObserver;
    private ScheduledFuture<?> heartbeatFuture;

    public ChatClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)  // 设置 keepalive 时间
                .keepAliveWithoutCalls(true)        // 允许在没有调用时发送 keepalive
                .build();
        asyncStub = ChatServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
        scheduler.shutdown();
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private void startHeartbeat() {
        heartbeatFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                ChatMessage heartbeat = ChatMessage.newBuilder()
                        .setUserId("heartbeat")
                        .setContent("heartbeat")
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                requestObserver.onNext(heartbeat);
                logger.info("Heartbeat sent");
            } catch (Exception e) {
                logger.warning("Failed to send heartbeat: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    public void chat(String userId) {
        StreamObserver<ChatMessage> responseObserver = new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage message) {
                if (!"heartbeat".equals(message.getUserId())) {
                    logger.info("Received message from " + message.getUserId() + ": " + message.getContent());
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Chat error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("Chat completed");
            }
        };

        requestObserver = asyncStub.chat(responseObserver);
        startHeartbeat();  // 启动心跳

        try {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter message (or 'quit' to exit): ");
                String content = scanner.nextLine();
                if ("quit".equalsIgnoreCase(content)) {
                    break;
                }

                ChatMessage message = ChatMessage.newBuilder()
                        .setUserId(userId)
                        .setContent(content)
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                requestObserver.onNext(message);
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
        requestObserver.onCompleted();
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient("localhost", 50051);
        try {
            client.chat("user1");
        } finally {
            client.shutdown();
        }
    }
} 