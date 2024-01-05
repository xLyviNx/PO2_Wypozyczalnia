package src.packets;

import java.io.Serializable;

public class addOfferButtonVisibility extends NetData implements Serializable {

    public addOfferButtonVisibility(boolean vis) {
        super(Operation.addButton);
    }
}
