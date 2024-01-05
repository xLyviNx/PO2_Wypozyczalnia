package src.packets;

import java.io.Serial;
import java.io.Serializable;

public class addOfferButtonVisibility extends NetData implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;
    public boolean isVisible;

    public addOfferButtonVisibility(boolean vis) {
        super(Operation.addButton);
        this.isVisible=vis;
    }
}
