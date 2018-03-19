package org.ishausa.registration.iii;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.sobject.Ashram_Visit_information__c;
import com.sforce.soap.enterprise.sobject.Program_Contact_Relation__c;
import com.sforce.soap.enterprise.sobject.Program__c;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.ConnectionException;
import org.ishausa.registration.iii.http.NameValuePairs;
import org.ishausa.registration.iii.renderer.SoyRenderer;
import org.ishausa.registration.iii.security.HttpsEnforcer;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

/**
 * Web app to help assign lodges / accommodation for iii program visitors.
 *
 * The input is a program id (as in salesforce). We will fetch all the Ashram Visits info
 * for that program. This will be a list of participants details along with Lodge assignments if any.
 * The user can then select a set of participants and then click on "Assign Lodges" button to
 * assign / re-assign the lodge details. After that, the updated information is again rendered.
 *
 * Update: Mar 2018.
 * Going to reuse this for Samyama check-in. The process is as below:
 * 1. Participant arrives to iii potentially several days before the program.
 * 2. Before entering the visitor center, we give them a waiver form & card for each participant in the car to fill their
 *  a. name,
 *  b. city and state,
 *  c. the meal option on their departure date
 * 3. Once filled, the volunteer collects and comes to the tent where we search for each of them by name,
 *  a. ensure they have registered for the program
 *  b. ensure they have paid for the stay from current date till beginning of program
 * 4. If all good, we will mark the participant as checked-in in the Ashram Visit info corresponding to the program dates.
 * 5. We will also update the departure date meal option, waiver status as signed and name tag status as collected.
 * 6. If someone is already checked-in, we will show the screen that allows one to modify/view
 *  * batch number
 *  * departure date (view only)
 *  * departure date meal option,
 *  * waiver signed,
 *  * name tag status
 *  * number (view only)
 *  * number tag tray location (view only)
 *  * medical screening status
 *  * valuables deposit status
 *  * hall location number
 * 7. One can search by either partial first/last name or the program number.
 * 8. Search should be efficient (we will preload the list of all participants and their ashram visit info in the browser app after login)
 *
 * Created by Prasanna Venkat on 6/25/2017.
 */
public class AshramVisitsApp {
    private static final Logger log = Logger.getLogger(AshramVisitsApp.class.getName());
    private static final Gson GSON = new GsonBuilder().create();

    private final EnterpriseConnection connection;

    private AshramVisitsApp() throws ConnectionException {
        final SalesforceAuthenticator authenticator = new SalesforceAuthenticator();
        connection = authenticator.login();
    }

    public static void main(final String[] args) throws ConnectionException {
        final AshramVisitsApp app = new AshramVisitsApp();

        port(Integer.parseInt(System.getenv("PORT")));
        staticFiles.location("/static");

        get("/", app::handleGet);
        get("/api/participants", app::getParticipantsForProgram);
        get("/api/visits", app::getAshramVisitsForProgram);

        post("/api/participant/visit", app::updateVisitInfo);

        exception(Exception.class, ((exception, request, response) -> {
            log.info("Exception: " + exception + " stack: " + Throwables.getStackTraceAsString(exception));
            response.status(500);
            response.body("Exception: " + exception + " stack: " + Throwables.getStackTraceAsString(exception));
        }));

        app.initFilters();
    }

    private void initFilters() {
        before(new HttpsEnforcer());
    }

    private String handleGet(final Request request, final Response response) {
        final String programId = request.queryParams("id");

        final String programName = getProgramName(connection, programId);
        if (!Strings.isNullOrEmpty(programId) && !Strings.isNullOrEmpty(programName)) {
            return SoyRenderer.INSTANCE.render(SoyRenderer.RegistrationAppTemplate.INDEX,
                    ImmutableMap.of("programName", programName));
        } else {
            return SoyRenderer.INSTANCE.render(SoyRenderer.RegistrationAppTemplate.NON_EXISTENT_ID,
                    ImmutableMap.of("id", Strings.nullToEmpty(programId)));
        }
    }

    private String getProgramName(final EnterpriseConnection connection, final String programId) {
        if (!Strings.isNullOrEmpty(programId)) {
            try {
                final QueryResult result =
                        connection.query("SELECT Name FROM Program__c WHERE Id = '" + programId + "'");
                if (result.getRecords().length > 0) {
                    return ((Program__c) result.getRecords()[0]).getName();
                }
            } catch (final ConnectionException e) {
                log.log(Level.SEVERE, "Exception querying Program__c for programId: " + programId, e);
            }
        }
        return null;
    }

    private String getParticipantsForProgram(final Request request, final Response response) {
        response.header("Content-Type", "application/json");
        final String programId = request.queryParams("pgm_id");
        final List<Program_Contact_Relation__c> participants = getParticipantsForProgram(connection, programId);
        return GSON.toJson(participants);
    }

    private String getAshramVisitsForProgram(final Request request, final Response response) {
        response.header("Content-Type", "application/json");
        final String programId = request.queryParams("pgm_id");
        final List<Ashram_Visit_information__c> ashramVisits = getAshramVisitsForProgram(connection, programId);
        return GSON.toJson(ashramVisits);
    }

    private List<Program_Contact_Relation__c> getParticipantsForProgram(final EnterpriseConnection connection,
                                                                        final String programId) {
        final List<Program_Contact_Relation__c> participants = new ArrayList<>();
        try {
            final String query =
                    "SELECT Id, Participant__r.Id, Participant__r.FirstName, Participant__r.LastName, Participant__r.Sathsang_Center__r.Name " +
                            "FROM Program_Contact_Relation__c " +
                            "WHERE Program__c = '" + programId + "'";
            final QueryResult queryResult = connection.query(query);

            for (final SObject record : queryResult.getRecords()) {
                participants.add((Program_Contact_Relation__c) record);
            }
        } catch (final ConnectionException e) {
            log.log(Level.SEVERE, "Exception querying Program_Contact_Relation__c for programId: " + programId, e);
        }
        return participants;
    }

    private List<Ashram_Visit_information__c> getAshramVisitsForProgram(final EnterpriseConnection connection,
                                                                        final String programId) {
        final List<Ashram_Visit_information__c> ashramVisits = new ArrayList<>();
        try {
            final String query =
                    "SELECT Id, VisitorName__c, VisitorName__r.Name, Checked_In__c " +
                            "FROM Ashram_Visit_information__c " +
                            "WHERE Program__c = '" + programId + "'";
            final QueryResult queryResult = connection.query(query);

            for (final SObject record : queryResult.getRecords()) {
                ashramVisits.add((Ashram_Visit_information__c) record);
            }
        } catch (final ConnectionException e) {
            log.log(Level.SEVERE, "Exception querying Ashram_Visit_information__c for programId: " + programId, e);
        }
        return ashramVisits;
    }

    private String updateVisitInfo(final Request request, final Response response) throws ConnectionException {
        response.header("Content-Type", "application/json");
        final String content = request.body();
        final Map<String, List<String>> params = NameValuePairs.splitParams(content);
        // Validate input
        final String ashramVisitId = NameValuePairs.nullSafeGetFirst(params, "Id");
        if (Strings.isNullOrEmpty(ashramVisitId)) {
            return GSON.toJson(new Status(StatusCode.FAILURE, "Request is missing ashram visit id param"));
        }

        // a participant may have multiple Ashram Visits around the program.
        // the check in status needs to be updated in all of them. TODO: Prasanna
        // the other fields need to be updated only in the Ashram Visit object that corresponds to the program dates.
        final Ashram_Visit_information__c visitInfo = new Ashram_Visit_information__c();
        visitInfo.setId(ashramVisitId);
        final String checkedInStatus = NameValuePairs.nullSafeGetFirst(params, "Checked_In__c");
        visitInfo.setChecked_In__c("true".equals(checkedInStatus));
        log.info("About to save ashram visit info for id: " + ashramVisitId + " with check in status: " + visitInfo.getChecked_In__c());
        connection.update(new Ashram_Visit_information__c[] {visitInfo});

        return GSON.toJson(new Status(StatusCode.OK, ""));
    }

    enum StatusCode {
        OK,
        FAILURE,
    }

    private class Status {
        private StatusCode status;
        @SerializedName("status_message")
        private String statusMessage;

        Status() {
            // for gson
        }

        Status(StatusCode code, String statusMessage) {
            this.status = code;
            this.statusMessage = statusMessage;
        }
    }
}
