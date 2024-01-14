package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet żądania logowania.
 */
public class LoginPacket extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 6L;
    /** Nazwa użytkownika. */
    public String login;
    /** Hasło użytkownika. */
    public String password;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param log Nazwa użytkownika.
     * @param pwd Hasło użytkownika.
     */
    public LoginPacket(String log, String pwd) {
        super(Operation.Login);
        login = log;
        password = pwd;
    }

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     */
    public LoginPacket() {
        super(Operation.Login);
    }
}
