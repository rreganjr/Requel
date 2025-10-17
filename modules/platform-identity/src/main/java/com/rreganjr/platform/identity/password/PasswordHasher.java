package com.rreganjr.platform.identity.password;

import java.math.BigInteger;
import java.security.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.lang3.StringUtils;

/**
 * Centralises password hashing and salt generation so identity consumers do not duplicate logic.
 */
public final class PasswordHasher {

    public static final String PREFERRED_ALGORITHM = "PBKDF2WITHHMACSHA512";
    public static final int PREFERRED_ITERATIONS = 50000;
    public static final String DEFAULT_LEGACY_ALGORITHM = "MD5";
    public static final String DEFAULT_LEGACY_SALT = "";
    public static final int DEFAULT_SALT_BYTES = 64;

    private PasswordHasher() {
    }

    /**
     * Generate a cryptographically secure random salt encoded in hexadecimal form.
     */
    public static String generateSalt() {
        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] salt = new byte[DEFAULT_SALT_BYTES];
            sr.nextBytes(salt);
            return toHexString(salt);
        } catch (Exception e) {
            throw PasswordException.problemGeneratingPasswordSalt(e);
        }
    }

    /**
     * Hash a password according to the supplied algorithm and parameters.
     */
    public static String hash(String password, String algorithmName, String salt, Integer iterations) {
        String effectiveAlgorithm = StringUtils.defaultIfBlank(algorithmName, DEFAULT_LEGACY_ALGORITHM);
        String effectiveSalt = StringUtils.defaultString(salt, DEFAULT_LEGACY_SALT);
        int effectiveIterations = iterations != null ? iterations : PREFERRED_ITERATIONS;

        byte[] encodedPassword;
        try {
            if (isSecretKeyFactoryAlgorithm(effectiveAlgorithm)) {
                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), effectiveSalt.getBytes(), effectiveIterations, 512);
                SecretKeyFactory skf = SecretKeyFactory.getInstance(effectiveAlgorithm);
                encodedPassword = skf.generateSecret(spec).getEncoded();
            } else if (isMessageDigestAlgorithm(effectiveAlgorithm)) {
                encodedPassword = MessageDigest.getInstance(effectiveAlgorithm).digest(password.getBytes());
            } else {
                throw PasswordException.badAlgorithmName(effectiveAlgorithm);
            }
        } catch (GeneralSecurityException e) {
            throw PasswordException.problemEncryptingPassword(e);
        }
        return toHexString(encodedPassword);
    }

    /**
     * Convenience helper that hashes the password using the preferred algorithm with a new salt.
     */
    public static HashedPassword hashWithPreferredSettings(String rawPassword) {
        String salt = generateSalt();
        String hash = hash(rawPassword, PREFERRED_ALGORITHM, salt, PREFERRED_ITERATIONS);
        return new HashedPassword(hash, salt, PREFERRED_ALGORITHM, PREFERRED_ITERATIONS);
    }

    private static boolean isSecretKeyFactoryAlgorithm(String algorithmName) {
        return Security.getAlgorithms("SecretKeyFactory").contains(StringUtils.upperCase(algorithmName));
    }

    private static boolean isMessageDigestAlgorithm(String algorithmName) {
        return Security.getAlgorithms("MessageDigest").contains(StringUtils.upperCase(algorithmName));
    }

    private static String toHexString(byte[] binaryData) {
        BigInteger bigInteger = new BigInteger(1, binaryData);
        String hex = bigInteger.toString(16);
        int paddingLength = (binaryData.length * 2) - hex.length();
        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        }
        return hex;
    }

    /**
     * Holder for hashed password metadata.
     */
    public record HashedPassword(String value, String salt, String algorithm, int iterations) {
    }
}
