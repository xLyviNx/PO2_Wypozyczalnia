package src.packets;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class VehiclePacket extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID =13L;
    public String brand;
    public String model;
    public String engine;
    public int year;
    public float price;
    public int databaseId;
    public String description;
    public boolean isRented=false;
    public int daysLeft = 0;
    public byte[] thumbnail;
    public ArrayList<byte[]> images;
    public boolean canBeDeleted = false;
    public String thumbnailPath;
    public ArrayList<String> imagePaths;
    public boolean isAnyRequiredEmpty()
    {
        return brand.isEmpty()||model.isEmpty()||engine.isEmpty()||year==0||price==0||databaseId==0;
    }
    public VehiclePacket() {
        super(Operation.OfferElement);
        images = new ArrayList<>();
        imagePaths= new ArrayList<>();
    }
}
