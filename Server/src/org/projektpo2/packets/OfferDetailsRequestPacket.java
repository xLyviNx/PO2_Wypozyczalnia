package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet żądania szczegółów oferty.
 */
public class OfferDetailsRequestPacket extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 8L;

    /** Identyfikator oferty. */
    public int id;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param id Identyfikator oferty.
     */
    public OfferDetailsRequestPacket(int id) {
        super(Operation.OfferDetails);
        this.id = id;
    }
}
