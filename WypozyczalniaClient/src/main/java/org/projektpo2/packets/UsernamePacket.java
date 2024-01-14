package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet z informacją o nazwie użytkownika.
 */
public class UsernamePacket extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 12L;

    /** Flaga określająca, czy użytkownik jest zalogowany. */
    public boolean isSignedIn;
    /** Nazwa użytkownika. */
    public String username;

    /**
     * Konstruktor inicjalizujący obiekt klasy dla zalogowanego użytkownika.
     *
     * @param username Nazwa użytkownika.
     */
    public UsernamePacket(String username) {
        super(Operation.OfferUsername);
        isSignedIn = true;
        this.username = username;
    }

    /**
     * Konstruktor inicjalizujący obiekt klasy dla niezalogowanego użytkownika.
     */
    public UsernamePacket() {
        super(Operation.OfferUsername);
        isSignedIn = false;
        username = "NIE ZALOGOWANO";
    }
}
