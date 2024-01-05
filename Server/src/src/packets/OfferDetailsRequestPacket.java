package src.packets;

import java.io.Serial;
import java.io.Serializable;

public class OfferDetailsRequestPacket extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 8L;
    public int id;
    public OfferDetailsRequestPacket(int id) {
        super(Operation.OfferDetails);
        this.id=id;
    }
}
