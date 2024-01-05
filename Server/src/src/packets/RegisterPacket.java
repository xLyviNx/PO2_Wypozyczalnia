package src.packets;

import java.io.Serializable;

public class RegisterPacket extends NetData implements Serializable
{
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
