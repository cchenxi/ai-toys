package com.example.grpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.KeyManagerFactory;

public class ChatClient {
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());
    private final ManagedChannel channel;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private StreamObserver<ChatMessage> requestObserver;

    public ChatClient(String host, int port) throws Exception {
        logger.info("开始初始化ChatClient...");

        // 加载客户端证书、密钥以及CA证书
        InputStream clientCertIs = getClass().getClassLoader().getResourceAsStream("certs/client.crt");
        InputStream clientKeyIs = getClass().getClassLoader().getResourceAsStream("certs/client.key");
        InputStream caIs = getClass().getClassLoader().getResourceAsStream("certs/ca.crt");

        if (clientCertIs == null || clientKeyIs == null || caIs == null) {
            logger.severe("必要的证书文件(.crt, .key, ca.crt)未找到，请先运行 generate_certs.sh 脚本。");
            throw new FileNotFoundException("证书文件未找到！");
        }
        logger.info("所有证书文件加载成功");

        // 配置ALPN
        logger.info("配置ALPN...");
        ApplicationProtocolConfig alpnConfig = new ApplicationProtocolConfig(
            ApplicationProtocolConfig.Protocol.ALPN,
            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
            ApplicationProtocolNames.HTTP_2,
            ApplicationProtocolNames.HTTP_1_1);

        logger.info("创建SSL上下文...");
        SslContext sslContext = SslContextBuilder.forClient()
                .sslProvider(SslProvider.JDK)
                .keyManager(clientCertIs, clientKeyIs) // 客户端的证书和私钥
                .trustManager(caIs) // 使用CA证书验证服务端
                .applicationProtocolConfig(alpnConfig)
                .build();

        logger.info("创建gRPC通道...");
        channel = NettyChannelBuilder.forAddress(host, port)
                .sslContext(sslContext)
                .keepAliveTime(60, TimeUnit.SECONDS)  // 设置 keepalive 时间
                .keepAliveWithoutCalls(true)        // 允许在没有调用时发送 keepalive
                .build();
        logger.info("gRPC通道创建成功");
        asyncStub = ChatServiceGrpc.newStub(channel);
        logger.info("ChatClient初始化完成");
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void chat(String userId) {
        StreamObserver<ChatMessage> responseObserver = new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage message) {
                logger.info("Received message from " + message.getUserId() + ": " + message.getContent());
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

        try {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter message (or 'quit' to exit): ");
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