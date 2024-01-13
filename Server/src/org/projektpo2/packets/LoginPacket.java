package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

public class LoginPacket extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 6L;
    public String login;
    public String password;

    public LoginPacket(String log, String pwd) {
        super(Operation.Login);
        login=log;
        password=pwd;
    }
    public LoginPacket()
    {
        super(Operation.Login);
    }
}
