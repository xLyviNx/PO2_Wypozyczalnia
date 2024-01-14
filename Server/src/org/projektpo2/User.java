package org.projektpo2;

import org.projektpo2.packets.NetData;

import java.net.Socket;

/**
 * Klasa reprezentująca użytkownika.
 */
public class User {
    /** Flaga określająca, czy użytkownik jest zalogowany. */
    public boolean isSignedIn;

    /** Nazwa użytkownika. */
    public String username;

    /** Gniazdo klienta. */
    public transient Socket clientSocket;

    /** Flaga określająca, czy użytkownik może dodawać oferty. */
    public boolean canAddOffers = false;

    /** Flaga określająca, czy użytkownik może usuwać oferty. */
    public boolean canDeleteOffers = false;

    /** Flaga określająca, czy użytkownik może rezerwować oferty. */
    public boolean canReserve = false;

    /** Flaga określająca, czy użytkownik może zarządzać rezerwacjami. */
    public boolean canManageReservations = false;

    /**
     * Sprawdza, czy użytkownik ma uprawnienia do danej operacji.
     *
     * @param operation Operacja do sprawdzenia uprawnień.
     * @return true, jeśli użytkownik ma uprawnienia, false w przeciwnym razie.
     */
    public boolean hasPermission(NetData.Operation operation) {
        if (!isSignedIn || username.isEmpty())
            return false;
        return switch (operation) {
            case AddOffer -> canAddOffers;
            case DeleteOffer -> canDeleteOffers;
            case ReservationRequest -> canReserve;
            case RequestConfirmations, ManageReservation -> canManageReservations;
            default -> false;
        };
    }
}
