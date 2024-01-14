package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet żądania rezerwacji pojazdu.
 */
public class ReservationRequestPacket extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 11L;

    /** Identyfikator pojazdu do zarezerwowania. */
    public int id;
    /** Liczba dni rezerwacji. */
    public int days;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param id   Identyfikator pojazdu do zarezerwowania.
     * @param days Liczba dni rezerwacji.
     */
    public ReservationRequestPacket(int id, int days) {
        super(Operation.ReservationRequest);
        this.id = id;
        this.days = days;
    }
}
