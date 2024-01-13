package org.projektpo2.packets;

import java.io.Serial;
import java.io.Serializable;

public class AddOfferButtonVisibility extends NetData implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;
    public boolean isVisible;

    public AddOfferButtonVisibility(boolean vis) {
        super(Operation.addButton);
        this.isVisible=vis;
    }
}
