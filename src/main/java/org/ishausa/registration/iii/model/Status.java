package org.ishausa.registration.iii.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author prasanna.venkatasubramanian
 */
public class Status {
    private StatusCode status;
    @SerializedName("status_message")
    private String statusMessage;

    Status() {
        // for gson
    }

    public Status(StatusCode code, String statusMessage) {
        this.status = code;
        this.statusMessage = statusMessage;
    }
}
