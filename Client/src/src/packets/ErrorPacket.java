package src.packets;

import java.io.Serial;
import java.io.Serializable;

public class ErrorPacket extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 5L; // Dowolna liczba, możesz użyć generatora
    public String ErrorMessage;
    public ErrorPacket(String err) {
        super(Operation.Unspecified);
        this.ErrorMessage = err;
    }
}
