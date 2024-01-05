package src.packets;

import java.io.Serial;
import java.io.Serializable;

public class DeleteOfferRequestPacket extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 4L; // Dowolna liczba, możesz użyć generatora
    public int id;
    public DeleteOfferRequestPacket(int id) {
        super(Operation.DeleteOffer);
        this.id=id;
    }
}
