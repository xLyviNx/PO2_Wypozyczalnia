package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet informujący o widoczności przycisku dodawania ofert.
 */
public class AddOfferButtonVisibility extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 2L;
    /** Flaga określająca widoczność przycisku. */
    public boolean isVisible;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param vis Flaga określająca widoczność przycisku.
     */
    public AddOfferButtonVisibility(boolean vis) {
        super(Operation.addButton);
        this.isVisible = vis;
    }
}
