package src;

import java.io.*;
import java.sql.Date;
import java.util.ArrayList;

public class NetData implements Serializable {
    private static final long serialVersionUID = 1L;

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
        ConfirmReservation,
        CancelReservation,
        ConfirmationsButton,
    }

    public enum OperationType {
        Unspecified,
        Success,
        Error,
        MessageBox
    }

    public OperationType operationType;
    public Operation operation;
    public ArrayList<String> Strings = new ArrayList<>();
    public ArrayList<Integer> Integers = new ArrayList<>();
    public ArrayList<Float> Floats = new ArrayList<>();
    public ArrayList<Boolean> Booleans = new ArrayList<>();
    public ArrayList<byte[]> Images = new ArrayList<>();
    public ArrayList<Date> Dates = new ArrayList<>();

    public NetData(Operation op, String str) {
        operation = op;
        Strings.add(str);
    }

    public NetData(Operation op) {
        operation = op;
    }

    // Metoda do zapisywania obiektu do strumienia
    public static byte[] serialize(NetData netData) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(netData);
            return bos.toByteArray();
        }
    }

    // Metoda do deserializacji obiektu z tablicy bajtów
    public static NetData deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (NetData) ois.readObject();
        }
    }
}
