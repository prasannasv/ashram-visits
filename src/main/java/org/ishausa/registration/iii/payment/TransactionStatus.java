package org.ishausa.registration.iii.payment;

import com.google.common.base.MoreObjects;
import org.ishausa.registration.iii.http.NameValuePairs;

import java.util.List;
import java.util.Map;

/**
 * https://developer.paypal.com/docs/classic/api/merchant/DoDirectPayment_API_Operation_NVP/
 *
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class TransactionStatus {
    private static final String FIELD_ACK = "ACK";
    private static final String FIELD_TIMESTAMP = "TIMESTAMP";
    private static final String FIELD_CORRELATION_ID = "CORRELATIONID";
    private static final String FIELD_TRANSACTION_ID = "TRANSACTIONID";
    private static final String FIELD_ERROR_CODE_0 = "L_ERRORCODE0";
    private static final String FIELD_LONG_MESSAGE_0 = "L_LONGMESSAGE0";

    private final String acknowledgment;
    private final String transactionId;
    private final String correlationId;
    private final String errorCode;
    private final String longMessage;

    private TransactionStatus(final String acknowledgment,
                              final String transactionId,
                              final String correlationId,
                              final String errorCode,
                              final String longMessage) {
        this.acknowledgment = acknowledgment;
        this.transactionId = transactionId;
        this.correlationId = correlationId;
        this.errorCode = errorCode;
        this.longMessage = longMessage;
    }

    private static class Builder {
        private String ack;
        private String txnId;
        private String corrId;
        private String errorCode;
        private String longMessage;

        public Builder withAck(final String ack) {
            this.ack = ack;
            return this;
        }

        public Builder withTxnId(final String txnId) {
            this.txnId = txnId;
            return this;
        }

        public Builder withCorrId(final String corrId) {
            this.corrId = corrId;
            return this;
        }

        public Builder withErrorCode(final String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder withLongMessage(final String longMessage) {
            this.longMessage = longMessage;
            return this;
        }

        public TransactionStatus build() {
            return new TransactionStatus(ack, txnId, corrId, errorCode, longMessage);
        }
    }

    // Sample response:
    // TIMESTAMP=2017%2d03%2d05T06%3a45%3a08Z&CORRELATIONID=b12b62b259bae&ACK=Success&VERSION=56%2e0&BUILD=25237094&AMT=1%2e00&CURRENCYCODE=USD&AVSCODE=Y&CVV2MATCH=X&TRANSACTIONID=51E24574PJ126625Y
    // TIMESTAMP=2017%2d03%2d05T07%3a31%3a49Z&CORRELATIONID=9dcd8f9ccc014&ACK=Failure&VERSION=56%2e0&BUILD=25237094&L_ERRORCODE0=10527&L_SHORTMESSAGE0=Invalid%20Data&L_LONGMESSAGE0=This%20transaction%20cannot%20be%20processed%2e%20Please%20enter%20a%20valid%20credit%20card%20number%20and%20type%2e&L_SEVERITYCODE0=Error&AMT=1%2e00&CURRENCYCODE=USD
    public static TransactionStatus fromResponse(final String response) {
        final Map<String, List<String>> responseParams = NameValuePairs.splitParams(response);
        final String acknowledgement = NameValuePairs.nullSafeGetFirst(responseParams, FIELD_ACK);
        if (acknowledgement.contains("Success")) {
            // TODO: Potential NPE
            return new TransactionStatus.Builder()
                    .withAck(acknowledgement)
                    .withTxnId(NameValuePairs.nullSafeGetFirst(responseParams, FIELD_TRANSACTION_ID))
                    .withCorrId(NameValuePairs.nullSafeGetFirst(responseParams, FIELD_CORRELATION_ID))
                    .build();
        }

        return new TransactionStatus.Builder()
                .withAck(acknowledgement)
                .withErrorCode(NameValuePairs.nullSafeGetFirst(responseParams, FIELD_ERROR_CODE_0))
                .withLongMessage(NameValuePairs.nullSafeGetFirst(responseParams, FIELD_LONG_MESSAGE_0))
                .build();
    }

    public static TransactionStatus exception(final String prefix,
                                              final Exception e) {
        return new TransactionStatus.Builder()
                .withAck(prefix)
                .withLongMessage(e.getMessage())
                .build();
    }

    public String getAcknowledgment() {
        return acknowledgment;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getLongMessage() {
        return longMessage;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("acknowledgement", getAcknowledgment())
                .add("transactionId", getTransactionId())
                .add("correlationId", getCorrelationId())
                .add("errorCode", getErrorCode())
                .add("longMessage", getLongMessage())
                .toString();
    }
}
