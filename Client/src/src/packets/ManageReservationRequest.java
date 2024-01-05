package src.packets;

import java.io.Serial;
import java.io.Serializable;

public class ManageReservationRequest extends NetData implements Serializable {
    @Serial
    private static final long serialVersionUID = 7L;
    public int id;
    public boolean confirm;
    public ManageReservationRequest(int id, boolean b) {
        super(Operation.ManageReservation);
        this.id=id;
        this.confirm=b;
    }
}
