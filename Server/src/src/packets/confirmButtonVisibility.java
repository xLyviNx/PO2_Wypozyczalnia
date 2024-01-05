package src.packets;

import java.io.Serializable;
public class confirmButtonVisibility extends src.packets.NetData implements Serializable
{
    public confirmButtonVisibility(Operation op, boolean vis) {
        super(op);
        isVisible=vis;
    }
    public boolean isVisible;
}
