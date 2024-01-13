package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

public class RegisterPacket extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 9L;
    public String login;
    public String password;
    public String repeat_password;
    public String imie;
    public String nazwisko;
    public int phonenumber;
    public boolean anyEmpty()
    {
        return login.isEmpty() || password.isEmpty() || repeat_password.isEmpty() || imie.isEmpty()||nazwisko.isEmpty() || phonenumber==0;
    }
    public RegisterPacket()
    {
        super(Operation.Register);
    }
}
