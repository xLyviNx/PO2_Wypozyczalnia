package src.packets;

import java.io.Serial;
import java.io.Serializable;
public class ConfirmButtonVisibility extends src.packets.NetData implements Serializable
{    @Serial
private static final long serialVersionUID = 3L; // Dowolna liczba, możesz użyć generatora
    public ConfirmButtonVisibility(Operation op, boolean vis) {
        super(op);
        isVisible=vis;
    }
    public boolean isVisible;
}
