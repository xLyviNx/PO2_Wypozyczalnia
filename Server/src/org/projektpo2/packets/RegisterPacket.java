package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet rejestracji.
 */
public class RegisterPacket extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 9L;

    /** Login użytkownika. */
    public String login;
    /** Hasło użytkownika. */
    public String password;
    /** Powtórzone hasło użytkownika w celu potwierdzenia. */
    public String repeat_password;
    /** Imię użytkownika. */
    public String imie;
    /** Nazwisko użytkownika. */
    public String nazwisko;
    /** Numer telefonu użytkownika. */
    public int phonenumber;

    /**
     * Sprawdza, czy któreś z pól są puste.
     *
     * @return True, jeśli co najmniej jedno pole jest puste; w przeciwnym razie false.
     */
    public boolean anyEmpty() {
        return login.isEmpty() || password.isEmpty() || repeat_password.isEmpty() || imie.isEmpty() || nazwisko.isEmpty() || phonenumber == 0;
    }

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     */
    public RegisterPacket() {
        super(Operation.Register);
    }
}
