package src.packets;

import java.io.Serial;
import java.io.Serializable;

public class ReservationRequestPacket  extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 11L;
    public int id;
    public int days;
    public ReservationRequestPacket(int id, int days) {
        super(Operation.ReservationRequest);
        this.id=id;
        this.days=days;
    }
}
