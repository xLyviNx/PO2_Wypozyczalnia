package org.projektpo2;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.*;

/**
 * Klasa do szyfrowania hasła za pomocą algorytmu MD5.
 */
public class MD5Encryptor {

    /** Obiekt do logowania zdarzeń. */
    private static final Logger logger = Utilities.getLogger(MD5Encryptor.class);

    /**
     * Metoda szyfrująca hasło za pomocą algorytmu MD5.
     *
     * @param password Hasło do zaszyfrowania.
     * @return Zaszyfrowane hasło w postaci ciągu znaków szesnastkowych.
     */
    public static String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] passwordBytes = password.getBytes();
            byte[] digest = md.digest(passwordBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, e.getClass().toString(), e);
            return null;
        }
    }
}
