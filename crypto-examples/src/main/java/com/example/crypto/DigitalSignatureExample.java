package com.example.crypto;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Demonstrates how to create and verify a digital signature using RSA.
 * A digital signature ensures data integrity and authenticity.
 * The data is signed with a private key, and the signature is verified
 * with the corresponding public key.
 */
public class DigitalSignatureExample {

    private static final String SIGNING_ALGORITHM = "SHA256withRSA";

    /**
     * Creates a digital signature for the given data using a private key.
     *
     * @param plainText  The data to sign.
     * @param privateKey The private key to use for signing.
     * @return A Base64-encoded string of the signature.
     * @throws Exception if signing fails.
     */
    public static String sign(String plainText, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance(SIGNING_ALGORITHM);
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] signature = privateSignature.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    /**
     * Verifies the digital signature of the given data using a public key.
     *
     * @param plainText        The original data.
     * @param signatureBase64  The Base64-encoded signature to verify.
     * @param publicKey        The public key to use for verification.
     * @return True if the signature is valid, false otherwise.
     * @throws Exception if verification fails for reasons other than an invalid signature.
     */
    public static boolean verify(String plainText, String signatureBase64, PublicKey publicKey) throws Exception {
        byte[] signature = Base64.getDecoder().decode(signatureBase64);

        Signature publicSignature = Signature.getInstance(SIGNING_ALGORITHM);
        publicSignature.initVerify(publicKey);
        publicSignature.update(plainText.getBytes(StandardCharsets.UTF_8));

        return publicSignature.verify(signature);
    }

    public static void main(String[] args) throws Exception {
        // 1. Generate a key pair
        // 1. 生成密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // 2. Define the data to be signed
        // 2. 定义要签名的数据
        String originalMessage = "This message will be signed to ensure its integrity.";
        System.out.println("Original Message: " + originalMessage);

        // 3. Sign the data with the private key
        // 3. 使用私钥对数据进行签名
        String signature = sign(originalMessage, privateKey);
        System.out.println("Signature (Base64): " + signature);

        // 4. Verify the signature with the public key
        // 4. 使用公钥验证签名
        boolean isSignatureValid = verify(originalMessage, signature, publicKey);
        System.out.println("\nVerification with original data: " + isSignatureValid);

        // 5. Attempt to verify with tampered data (demonstrates failure)
        // 5. 尝试使用被篡改的数据验证签名（演示验证失败）
        String tamperedMessage = "This message has been tampered with!";
        boolean isTamperedSignatureValid = verify(tamperedMessage, signature, publicKey);
        System.out.println("Verification with tampered data: " + isTamperedSignatureValid);
    }
}