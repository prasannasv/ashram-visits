package org.ishausa.registration.iii.payment;

import com.google.common.base.MoreObjects;

/**
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class PaymentInfo {
    private final int amount;
    private final String custom;
    private final String description;

    public PaymentInfo(final int amount, final String custom, final String description) {
        this.amount = amount;
        this.custom = custom;
        this.description = description;
    }

    public int getAmount() {
        return amount;
    }

    public String getCustom() {
        return custom;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("amount", getAmount())
                .add("custom", getCustom())
                .add("description", getDescription())
                .toString();
    }
}
