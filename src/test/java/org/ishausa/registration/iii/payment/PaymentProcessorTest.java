package org.ishausa.registration.iii.payment;

import com.paypal.api.payments.CreditCard;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class PaymentProcessorTest {
    private PaymentProcessor paymentProcessor;

    @Before
    public void setUp() {
        paymentProcessor = new PaymentProcessor();
    }

    @Test
    public void validateFormatting() {
        final CreditCard card = new CreditCard("4147008081913313", "Visa", 3, 2020);
        card.setCvv2("453");
        final String value = String.format("%02d%d", card.getExpireMonth(), card.getExpireYear());
        assertEquals("022020", value);
        assertEquals("453", card.getCvv2String());
    }

    @Test
    public void invalidCard() {
        final PaymentInfo amount = new PaymentInfo(1, "Children's Program Jul 2017", "Children's Program Jul 2017");
        final CreditCard card = new CreditCard("4147008081913313", "Visa", 1, 2010);
        final CardOwnerInfo ownerInfo = new CardOwnerInfo.Builder()
                .withFirstName("Prasanna")
                .withLastName("Venkat")
                .withAddressLine1("123 2nd Ave N")
                .withCity("Atlanta")
                .withState("GA")
                .withZip("30301")
                .withEmail("random@gmail.com").build();

        final TransactionStatus status = paymentProcessor.chargeCreditCard(amount, card, ownerInfo);
        assertEquals("Failure", status.getAcknowledgment());
        assertNotNull(status.getLongMessage());
    }
}
