package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca pakiet żądania ofert z filtrem.
 */
public class FilteredOffersRequestPacket extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 16L;
    /** Marka pojazdu. */
    public String brand;
    /** Minimalny rok produkcji. */
    public int yearMin;
    /** Maksymalny rok produkcji. */
    public int yearMax;
    /** Minimalna pojemność silnika. */
    public int engineCapMin;
    /** Maksymalna pojemność silnika. */
    public int engineCapMax;
    /** Minimalna cena. */
    public float priceMin;
    /** Maksymalna cena. */
    public float priceMax;
    /** Kierunek sortowania według ceny (malejący/rosnący). */
    public boolean priceDESC;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param brand       Marka pojazdu.
     * @param yearMin     Minimalny rok produkcji.
     * @param yearMax     Maksymalny rok produkcji.
     * @param engineCapMin Minimalna pojemność silnika.
     * @param engineCapMax Maksymalna pojemność silnika.
     * @param priceMin    Minimalna cena.
     * @param priceMax    Maksymalna cena.
     * @param priceDESC   Kierunek sortowania według ceny (malejący/rosnący).
     */
    public FilteredOffersRequestPacket(
            String brand,
            int yearMin,
            int yearMax,
            int engineCapMin,
            int engineCapMax,
            float priceMin,
            float priceMax,
            boolean priceDESC) {
        super(Operation.FilteredOffersRequest);
        this.brand = brand;
        this.yearMin = yearMin;
        this.yearMax = yearMax;
        this.engineCapMin = engineCapMin;
        this.engineCapMax = engineCapMax;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.priceDESC = priceDESC;
    }
}
