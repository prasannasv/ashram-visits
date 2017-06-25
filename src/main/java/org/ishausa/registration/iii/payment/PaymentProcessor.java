package org.ishausa.registration.iii.payment;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.paypal.api.payments.CreditCard;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * https://developer.paypal.com/docs/classic/api/merchant/DoDirectPayment_API_Operation_NVP/
 *
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class PaymentProcessor {
    private static final Logger log = Logger.getLogger(PaymentProcessor.class.getName());

    private static final String PAYPAL_API_ENDPOINT = "https://api-3t.paypal.com/nvp";
    // TODO: This is really very old version of the API - even before 2012.
    // Move to a newer version.
    private static final String PAYPAL_API_VERSION = "56.0";
    private static final String PAYPAL_DO_DIRECT_PAYMENT_METHOD = "DoDirectPayment";

    // Hardcoding currency to USD. Need to configure payment gateway to support other countries.
    private static final String CURRENCY_CODE = "USD";

    private final String paypalApiUsername;
    private final String paypalApiPassword;
    private final String paypalApiSignature;

    private final List<NameValuePair> paypalApiRequestStandardParams;

    public PaymentProcessor() {
        paypalApiUsername = System.getenv("paypal_api_user");
        paypalApiPassword = System.getenv("paypal_api_password");
        paypalApiSignature = System.getenv("paypal_api_signature");
        if (Strings.isNullOrEmpty(paypalApiUsername) ||
                Strings.isNullOrEmpty(paypalApiPassword) ||
                Strings.isNullOrEmpty(paypalApiSignature)) {
            throw new IllegalStateException("paypal_api_user / paypal_api_password / paypal_api_signature " +
                    "environment variable(s) are not set");
        }
        paypalApiRequestStandardParams = ImmutableList.copyOf(paymentRequestNameValuePairs());
    }

    private List<NameValuePair> paymentRequestNameValuePairs() {
        final List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("USER", paypalApiUsername));
        params.add(new BasicNameValuePair("PWD", paypalApiPassword));
        params.add(new BasicNameValuePair("SIGNATURE", paypalApiSignature));
        params.add(new BasicNameValuePair("METHOD", PAYPAL_DO_DIRECT_PAYMENT_METHOD));
        params.add(new BasicNameValuePair("VERSION", PAYPAL_API_VERSION));

        return params;
    }

    public TransactionStatus chargeCreditCard(final PaymentInfo paymentInfo,
                                              final CreditCard card,
                                              final CardOwnerInfo ownerInfo) {
        final List<NameValuePair> params = buildNameValuePairs(paymentInfo, card, ownerInfo);

        try {
            log.info("Attempting transaction for Owner: " + ownerInfo + ", PaymentInfo: " + paymentInfo);

            final TransactionStatus txnStatus = transferMoney(params);

            log.info("Owner: " + ownerInfo + ", PaymentInfo: " + paymentInfo + ", Status: " + txnStatus);

            return txnStatus;
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private TransactionStatus transferMoney(final List<NameValuePair> params) throws UnsupportedEncodingException {
        final HttpPost post = new HttpPost(PAYPAL_API_ENDPOINT);
        params.addAll(paypalApiRequestStandardParams);
        post.setEntity(new UrlEncodedFormEntity(params));
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);

            final HttpEntity entity = response.getEntity();
            final String content = CharStreams.toString(new InputStreamReader(entity.getContent()));

            log.info("response: " + content);
            EntityUtils.consume(entity);

            return TransactionStatus.fromResponse(content);
        } catch (final IOException e) {
            return TransactionStatus.exception("Exception while contacting PayPal", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (final IOException e) {
                    log.log(Level.WARNING, "Exception while closing the response", e);
                }
            }
        }
    }

    private List<NameValuePair> buildNameValuePairs(final PaymentInfo paymentInfo,
                                                    final CreditCard card,
                                                    final CardOwnerInfo ownerInfo) {
        final List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("PAYMENTACTION", "Sale"));
        params.add(new BasicNameValuePair("AMT", Integer.toString(paymentInfo.getAmount())));
        params.add(new BasicNameValuePair("CURRENCYCODE", CURRENCY_CODE));
        params.add(new BasicNameValuePair("CREDITCARDTYPE", card.getType()));
        params.add(new BasicNameValuePair("ACCT", card.getNumber()));
        params.add(new BasicNameValuePair("EXPDATE", String.format("%02d%d", card.getExpireMonth(), card.getExpireYear())));
        params.add(new BasicNameValuePair("CVV2", card.getCvv2String()));
        params.add(new BasicNameValuePair("FIRSTNAME", ownerInfo.getFirstName()));
        params.add(new BasicNameValuePair("LASTNAME", ownerInfo.getLastName()));
        params.add(new BasicNameValuePair("CUSTOM", paymentInfo.getCustom()));
        params.add(new BasicNameValuePair("STREET", ownerInfo.getAddressLine1()));
        params.add(new BasicNameValuePair("CITY", ownerInfo.getCity()));
        params.add(new BasicNameValuePair("STATE", ownerInfo.getState()));
        params.add(new BasicNameValuePair("ZIP", ownerInfo.getZip()));
        params.add(new BasicNameValuePair("COUNTRYCODE", ownerInfo.getCountry()));
        params.add(new BasicNameValuePair("EMAIL", ownerInfo.getEmail()));
        params.add(new BasicNameValuePair("DESC", paymentInfo.getDescription()));

        return params;
    }
}
