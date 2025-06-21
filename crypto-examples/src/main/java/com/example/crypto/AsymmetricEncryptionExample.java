package com.example.crypto;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Demonstrates asymmetric encryption and decryption using RSA.
 * In this system, a message is encrypted with a public key and can only be
 * decrypted with the corresponding private key.
 */
public class AsymmetricEncryptionExample {

    private static final String ALGORITHM = "RSA";

    /**
     * Encrypts a plaintext using a public key.
     *
     * @param plainText The plaintext to encrypt.
     * @param publicKey The public key to use for encryption.
     * @return A Base64-encoded string of the ciphertext.
     * @throws Exception if encryption fails.
     */
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipherText);
    }

    /**
     * Decrypts a ciphertext using a private key.
     *
     * @param cipherTextBase64 The Base64-encoded ciphertext.
     * @param privateKey The private key to use for decryption.
     * @return The original plaintext.
     * @throws Exception if decryption fails.
     */
    public static String decrypt(String cipherTextBase64, PrivateKey privateKey) throws Exception {
        byte[] cipherText = Base64.getDecoder().decode(cipherTextBase64);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedText = cipher.doFinal(cipherText);
        return new String(decryptedText, StandardCharsets.UTF_8);
    }


    public static void main(String[] args) throws Exception {
        // 1. Generate a key pair (public and private key)
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(2048); // Key size
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // 2. Define a plaintext message
        String originalMessage = "This is a secret message for asymmetric encryption!";
        System.out.println("Original Message: " + originalMessage);

        // 3. Encrypt the message using the public key
        String encryptedMessage = encrypt(originalMessage, publicKey);
        System.out.println("Encrypted Message (Base64): " + encryptedMessage);

        // 4. Decrypt the message using the private key
        String decryptedMessage = decrypt(encryptedMessage, privateKey);
        System.out.println("Decrypted Message: " + decryptedMessage);

        // Verification
        System.out.println("\nVerification Successful: " + originalMessage.equals(decryptedMessage));
    }
} 