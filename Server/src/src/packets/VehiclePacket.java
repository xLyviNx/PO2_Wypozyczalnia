package src.packets;

import java.io.Serializable;
import java.util.ArrayList;

public class VehiclePacket extends NetData implements Serializable
{
    public String brand;
    public String model;
    public String engine;
    public int year;
    public float price;
    public int databaseId;
    public boolean isRented=false;
    public int daysLeft = 0;
    public byte[] thumbnail;
    public ArrayList<byte[]> images;
    public VehiclePacket() {
        super(Operation.OfferElement);
        images = new ArrayList<>();
    }
}
