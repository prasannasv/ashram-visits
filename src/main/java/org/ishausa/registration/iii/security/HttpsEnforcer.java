package org.ishausa.registration.iii.security;

import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.logging.Logger;

import static spark.Spark.halt;

/**
 * Created by Prasanna Venkat on 3/5/2017.
 */
public class HttpsEnforcer implements Filter {
    private static final Logger log = Logger.getLogger(HttpsEnforcer.class.getName());

    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    @Override
    public void handle(final Request request, final Response response) throws Exception {

        log.info("url: " + request.url() + ", queryString: " + request.queryString());
        final String url = request.url();
        final String queryString = request.queryString();
        final String fullUrl = (queryString != null) ? url + '?' + queryString : url;

        if (!fullUrl.startsWith("http://localhost:")) {
            final String forwardedProto = request.headers(X_FORWARDED_PROTO);

            if ("http".equals(forwardedProto)) {
                final String redirectUrl = fullUrl.replace("http://", "https://");
                log.info("Redirecting to a secure scheme on a non-secure request. Redirect to: " + redirectUrl);
                response.redirect(redirectUrl);
                halt();
            }
        }
    }
}
