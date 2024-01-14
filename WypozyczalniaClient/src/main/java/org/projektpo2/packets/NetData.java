package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

/**
 * Klasa reprezentująca bazowy pakiet sieciowy.
 */
public class NetData implements Serializable {
    /** Wersja serializacji. */
    @Serial
    private static final long serialVersionUID = 1L;

    /** Enumeracja określająca operację związaną z pakietem. */
    public enum Operation {
        Unspecified,
        Register,
        Login,
        OfferElement,
        OfferUsername,
        Ping,
        OfferDetails,
        Logout,
        ReservationRequest,
        addButton,
        AddOffer,
        DeleteOffer,
        RequestConfirmations,
        ReservationElement,
        ManageReservation,
        ConfirmationsButton,
        FilteredOffersRequest,
        BrandsList,
        Exit,
    }

    /** Enumeracja określająca typ operacji (sukces, błąd, niesprecyzowane). */
    public enum OperationType {
        Unspecified,
        Success,
        Error,
    }

    /** Typ operacji. */
    public OperationType operationType;
    /** Operacja związana z pakietem. */
    public Operation operation;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param op Operacja związana z pakietem.
     */
    public NetData(Operation op) {
        operation = op;
    }
}
