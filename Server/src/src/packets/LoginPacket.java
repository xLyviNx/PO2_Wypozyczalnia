package src.packets;

import java.io.Serializable;

public class LoginPacket extends NetData implements Serializable
{
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
