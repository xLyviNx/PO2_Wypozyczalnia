package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca element rezerwacji.
 */
public class ReservationElement extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 10L;

    /** Identyfikator rezerwacji. */
    public int reserveId;
    /** Liczba dni rezerwacji. */
    public int reserveDays;
    /** Marka pojazdu. */
    public String brand;
    /** Model pojazdu. */
    public String model;
    /** Rok produkcji pojazdu. */
    public int productionYear;
    /** Identyfikator pojazdu. */
    public int carId;
    /** Dzienna cena wynajmu pojazdu. */
    public float dailyPrice;
    /** Login użytkownika dokonującego rezerwacji. */
    public String login;
    /** Imię użytkownika dokonującego rezerwacji. */
    public String firstName;
    /** Nazwisko użytkownika dokonującego rezerwacji. */
    public String lastName;
    /** Numer telefonu użytkownika dokonującego rezerwacji. */
    public int phoneNumber;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     */
    public ReservationElement() {
        super(Operation.ReservationElement);
    }
}
