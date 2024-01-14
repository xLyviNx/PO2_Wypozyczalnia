package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet błędu.
 */
public class ErrorPacket extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 5L;
    /** Komunikat błędu. */
    public String ErrorMessage;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param err Komunikat błędu.
     */
    public ErrorPacket(String err) {
        super(Operation.Unspecified);
        this.ErrorMessage = err;
    }
}
