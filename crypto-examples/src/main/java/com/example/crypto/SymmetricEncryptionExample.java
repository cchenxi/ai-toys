package com.example.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Demonstrates symmetric encryption and decryption using AES/GCM/NoPadding.
 * GCM (Galois/Counter Mode) is an authenticated encryption mode that provides both
 * confidentiality and data authenticity.
 */
public class SymmetricEncryptionExample {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    /**
     * Encrypts a plaintext using a secret key.
     *
     * @param plainText The plaintext to encrypt.
     * @param secretKey The secret key to use for encryption.
     * @return A Base64-encoded string containing the IV and the ciphertext.
     * @throws Exception if encryption fails.
     */
    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Prepend IV to ciphertext
        byte[] encryptedData = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, encryptedData, 0, iv.length);
        System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Decrypts a ciphertext using a secret key.
     *
     * @param cipherTextBase64 The Base64-encoded string (IV + ciphertext).
     * @param secretKey The secret key to use for decryption.
     * @return The original plaintext.
     * @throws Exception if decryption fails.
     */
    public static String decrypt(String cipherTextBase64, SecretKey secretKey) throws Exception {
        byte[] encryptedData = Base64.getDecoder().decode(cipherTextBase64);

        // Extract IV from the beginning of the encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, iv.length);

        byte[] cipherText = new byte[encryptedData.length - iv.length];
        System.arraycopy(encryptedData, iv.length, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] decryptedText = cipher.doFinal(cipherText);

        return new String(decryptedText, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        // 1. Generate a secret key
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256); // 128, 192, or 256 bits
        SecretKey secretKey = keyGenerator.generateKey();

        // 2. Define a plaintext message
        String originalMessage = "This is a secret message for symmetric encryption!";
        System.out.println("Original Message: " + originalMessage);

        // 3. Encrypt the message
        String encryptedMessage = encrypt(originalMessage, secretKey);
        System.out.println("Encrypted Message (Base64): " + encryptedMessage);

        // 4. Decrypt the message
        String decryptedMessage = decrypt(encryptedMessage, secretKey);
        System.out.println("Decrypted Message: " + decryptedMessage);

        // Verification
        System.out.println("\nVerification Successful: " + originalMessage.equals(decryptedMessage));
    }
} 