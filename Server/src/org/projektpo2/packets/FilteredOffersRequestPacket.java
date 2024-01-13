package org.projektpo2.packets;
import java.io.Serial;
import java.io.Serializable;

public class FilteredOffersRequestPacket extends NetData implements Serializable {
    @Serial
    private static final long serialVersionUID = 16L;
    public String brand;
    public int yearMin;
    public int yearMax;
    public int engineCapMin;
    public int engineCapMax;
    public float priceMin;
    public float priceMax;
    public boolean priceDESC;

    public FilteredOffersRequestPacket(
            String brand,
            int yearMin,
            int yearMax,
            int engineCapMin,
            int engineCapMax,
            float priceMin,
            float priceMax,
            boolean priceDESC) {
        super(Operation.FilteredOffersRequest);
        this.brand = brand;
        this.yearMin = yearMin;
        this.yearMax = yearMax;
        this.engineCapMin = engineCapMin;
        this.engineCapMax = engineCapMax;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.priceDESC = priceDESC;
    }
}
