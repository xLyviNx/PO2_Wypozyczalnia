package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

public class UsernamePacket extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 12L;
    public boolean isSignedIn;
    public String username;
    public UsernamePacket(String username) {
        super(Operation.OfferUsername);
        isSignedIn=true;
        this.username =username;
    }
    public UsernamePacket()
    {
        super(Operation.OfferUsername);
        isSignedIn=false;
        username="NIE ZALOGOWANO";
    }
}
