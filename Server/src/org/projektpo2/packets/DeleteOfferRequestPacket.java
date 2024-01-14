package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet żądania usunięcia oferty.
 */
public class DeleteOfferRequestPacket extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 4L;
    /** Identyfikator oferty do usunięcia. */
    public int id;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param id Identyfikator oferty do usunięcia.
     */
    public DeleteOfferRequestPacket(int id) {
        super(Operation.DeleteOffer);
        this.id = id;
    }
}
