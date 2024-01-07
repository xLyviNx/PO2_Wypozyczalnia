package src.packets;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;

public class BrandsList extends NetData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 15L;

    public HashSet<String> brands = new HashSet<>();
    public BrandsList()
    {
        super(Operation.BrandsList);
    }
}
