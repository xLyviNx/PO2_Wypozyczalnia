package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;

/**
 * Klasa reprezentująca pakiet zawierający listę marek samochodów.
 */
public class BrandsList extends NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 15L;
    /** Zbiór zawierający nazwy marek samochodów. */
    public HashSet<String> brands = new HashSet<>();

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     */
    public BrandsList() {
        super(Operation.BrandsList);
    }
}
