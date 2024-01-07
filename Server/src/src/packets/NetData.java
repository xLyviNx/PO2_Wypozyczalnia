package src.packets;

import java.io.*;
import java.sql.Date;
import java.util.ArrayList;

public class NetData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L; // Dowolna liczba, możesz użyć generatora
    public enum Operation {
        Unspecified,
        Register,
        Login,
        OfferElement,
        OfferUsername,
        Ping,
        Exit,
        OfferDetails,
        Logout,
        ReservationRequest,
        removeButton,
        addButton,
        AddOffer,
        DeleteOffer,
        RequestConfirmtations,
        ReservationElement,
        ManageReservation,
        ConfirmationsButton,
        FilteredOffersRequest,
        BrandsList
    }

    public enum OperationType {
        Unspecified,
        Success,
        Error,
        MessageBox
    }

    public OperationType operationType;
    public Operation operation;

    public NetData(Operation op) {
        operation = op;
    }

    public static byte[] serialize(NetData netData) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(netData);
            return bos.toByteArray();
        }
    }
    public static NetData deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (NetData) ois.readObject();
        }
    }
}
