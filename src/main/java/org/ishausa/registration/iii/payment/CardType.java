package org.ishausa.registration.iii.payment;

import java.util.regex.Pattern;

/**
 * Lifted from http://stackoverflow.com/a/23814692/7247103
 *
 * TypeName's are what PayPal uses: https://developer.paypal.com/docs/classic/api/merchant/DoDirectPayment_API_Operation_NVP/
 *
 * Created by Prasanna Venkat on 3/5/2017.
 */
public enum CardType {
    UNKNOWN,
    VISA("^4[0-9]{12}(?:[0-9]{3})?$", "Visa"),
    MASTERCARD("^5[1-5][0-9]{14}$", "MasterCard"),
    AMERICAN_EXPRESS("^3[47][0-9]{13}$", "Amex"),
    DINERS_CLUB("^3(?:0[0-5]|[68][0-9])[0-9]{11}$", "DinersClub"),
    DISCOVER("^6(?:011|5[0-9]{2})[0-9]{12}$", "Discover"),
    JCB("^(?:2131|1800|35\\d{3})\\d{11}$", "JCB");

    private final Pattern pattern;
    private final String typeName;

    CardType() {
        this.pattern = null;
        this.typeName = null;
    }

    CardType(final String pattern, final String typeName) {
        this.pattern = Pattern.compile(pattern);
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public static CardType detect(final String cardNumber) {

        for (final CardType cardType : CardType.values()) {
            if (cardType.pattern != null) {
                if (cardType.pattern.matcher(cardNumber).matches()) {
                    return cardType;
                }
            }
        }

        return UNKNOWN;
    }
}