package src.packets;

import java.io.Serial;
import java.io.Serializable;

public class ReservationElement extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 10L;
    public int reserveId;
    public int reserveDays;
    public String brand;
    public String model;
    public int productionYear;
    public int carId;
    public float dailyPrice;
    public String login;
    public String firstName;
    public String lastName;
    public int phoneNumber;

    public ReservationElement() {
        super(Operation.ReservationElement);
    }
}
