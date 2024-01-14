package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Klasa reprezentująca pakiet z informacjami o pojeździe.
 */
public class VehiclePacket extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 13L;

    /** Marka pojazdu. */
    public String brand;
    /** Model pojazdu. */
    public String model;
    /** Silnik pojazdu. */
    public String engine;
    /** Rok produkcji pojazdu. */
    public int year;
    /** Cena wynajmu pojazdu. */
    public float price;
    /** Identyfikator w bazie danych. */
    public int databaseId;
    /** Opis pojazdu. */
    public String description;
    /** Flaga określająca, czy pojazd jest wynajęty. */
    public boolean isRented = false;
    /** Liczba pozostałych dni wynajmu. */
    public int daysLeft = 0;
    /** Miniatura pojazdu w formie tablicy bajtów. */
    public byte[] thumbnail;
    /** Lista obrazów pojazdu w formie tablic bajtów. */
    public ArrayList<byte[]> images;
    /** Flaga określająca, czy pojazd może zostać usunięty. */
    public boolean canBeDeleted = false;
    /** Ścieżka do miniatury pojazdu. */
    public String thumbnailPath;
    /** Lista ścieżek do obrazów pojazdu. */
    public ArrayList<String> imagePaths;
    /** Pojemność silnika pojazdu. */
    public int engineCap;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     */
    public VehiclePacket() {
        super(Operation.OfferElement);
        images = new ArrayList<>();
        imagePaths = new ArrayList<>();
    }

    /**
     * Metoda sprawdzająca, czy jakiekolwiek wymagane pola są puste.
     *
     * @return true, jeśli jakiekolwiek wymagane pola są puste, false w przeciwnym razie.
     */
    public boolean isAnyRequiredEmpty() {
        return brand.isEmpty() || model.isEmpty() || engine.isEmpty() || year == 0 || price == 0 || databaseId == 0;
    }
}
