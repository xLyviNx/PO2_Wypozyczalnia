package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet żądania zarządzania rezerwacją.
 */
public class ManageReservationRequest extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 7L;
    /** Identyfikator rezerwacji. */
    public int id;
    /** Flaga potwierdzenia/anulowania rezerwacji. */
    public boolean confirm;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param id      Identyfikator rezerwacji.
     * @param confirm Flaga potwierdzenia/anulowania rezerwacji.
     */
    public ManageReservationRequest(int id, boolean confirm) {
        super(Operation.ManageReservation);
        this.id = id;
        this.confirm = confirm;
    }
}
