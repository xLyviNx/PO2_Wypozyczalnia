package src.packets;

import java.io.Serializable;

public class ErrorPacket extends NetData implements Serializable
{
    public String ErrorMessage;
    public ErrorPacket(String err) {
        super(Operation.Unspecified);
        this.ErrorMessage = err;
    }
}
