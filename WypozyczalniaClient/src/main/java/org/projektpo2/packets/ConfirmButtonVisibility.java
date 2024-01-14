package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet sterujący widocznością przycisku potwierdzenia.
 */
public class ConfirmButtonVisibility extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 3L;
    /** Flaga określająca widoczność przycisku potwierdzenia. */
    public boolean isVisible;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param op  Typ Operacji.
     * @param vis Flaga określająca widoczność przycisku potwierdzenia.
     */
    public ConfirmButtonVisibility(Operation op, boolean vis) {
        super(op);
        isVisible = vis;
    }
}
