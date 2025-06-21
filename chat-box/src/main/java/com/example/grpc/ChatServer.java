package com.example.grpc;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

public class ChatServer {
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    private Server server;
    private final int port;

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        // 加载服务端证书、密钥以及CA证书
        InputStream serverCertIs = getClass().getClassLoader().getResourceAsStream("certs/server.crt");
        InputStream serverKeyIs = getClass().getClassLoader().getResourceAsStream("certs/server.key");
        InputStream caIs = getClass().getClassLoader().getResourceAsStream("certs/ca.crt");

        if (serverCertIs == null || serverKeyIs == null || caIs == null) {
            throw new FileNotFoundException("必要的证书文件(.crt, .key, ca.crt)未找到，请先运行 generate_certs.sh 脚本。");
        }

        // 配置ALPN
        ApplicationProtocolConfig alpnConfig = new ApplicationProtocolConfig(
            ApplicationProtocolConfig.Protocol.ALPN,
            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
            ApplicationProtocolNames.HTTP_2,
            ApplicationProtocolNames.HTTP_1_1);

        SslContext sslContext = SslContextBuilder.forServer(serverCertIs, serverKeyIs)
                .sslProvider(SslProvider.JDK)
                .trustManager(caIs) // 使用CA证书来验证客户端
                .clientAuth(ClientAuth.REQUIRE) // 强制要求客户端认证
                .applicationProtocolConfig(alpnConfig)
                .build();

        server = NettyServerBuilder.forPort(port)
                .addService(new ChatServiceImpl())
                .sslContext(sslContext)
                .build()
                .start();
        logger.info("Server started with TLS, listening on " + port);

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