package org.projektpo2;
import org.projektpo2.packets.NetData;

import java.net.Socket;

public class User {
    public boolean isSignedIn;
    public String username;
    public transient Socket clientSocket;
    public boolean canAddOffers = false;
    public boolean canDeleteOffers = false;
    public boolean canReserve = false;
    public boolean canManageReservations = false;

    public boolean hasPermission(NetData.Operation operation) {
        if (!isSignedIn || username.isEmpty())
            return false;
        return switch (operation) {
            case AddOffer -> canAddOffers;
            case DeleteOffer -> canDeleteOffers;
            case ReservationRequest -> canReserve;
            case RequestConfirmtations, ManageReservation -> canManageReservations;
            default -> false;
        };
    }
}
