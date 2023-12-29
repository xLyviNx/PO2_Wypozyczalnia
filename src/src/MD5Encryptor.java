package src;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Encryptor {

    public static String encryptPassword(String password) {
        try {
            // Utwórz obiekt MessageDigest z algorytmem MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Konwertuj hasło na tablicę bajtów
            byte[] passwordBytes = password.getBytes();

            // Oblicz skrót MD5 dla hasła
            byte[] digest = md.digest(passwordBytes);

            // Konwertuj skrót na zapis szesnastkowy (hex)
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Obsłuż wyjątek w razie problemów z algorytmem MD5
            e.printStackTrace();
            return null;
        }
    }
}