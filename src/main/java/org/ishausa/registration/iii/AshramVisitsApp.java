package org.ishausa.registration.iii;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.sobject.Ashram_Visit_information__c;
import com.sforce.soap.enterprise.sobject.Program_Contact_Relation__c;
import com.sforce.soap.enterprise.sobject.Program__c;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.ConnectionException;
import org.ishausa.registration.iii.renderer.SoyRenderer;
import org.ishausa.registration.iii.security.HttpsEnforcer;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;
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

    private final EnterpriseConnection connection;

    private AshramVisitsApp() throws ConnectionException {
        final SalesforceAuthenticator authenticator = new SalesforceAuthenticator();
        connection = null; //authenticator.login();
    }

    public static void main(final String[] args) throws ConnectionException {
        final AshramVisitsApp app = new AshramVisitsApp();

        port(Integer.parseInt(System.getenv("PORT")));
        staticFiles.location("/static");

        get("/", app::handleGet);

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

    private SoyListData participantsToSoyData(final List<Program_Contact_Relation__c> participants) {
        final SoyListData listData = new SoyListData();

        for (final Program_Contact_Relation__c participant : participants) {
            final SoyMapData soyMapData = new SoyMapData();
            soyMapData.put("id", participant.getId());
            soyMapData.put("name", participant.getName());
            listData.add(soyMapData);
        }

        return listData;
    }

    private SoyListData ashramVisitsToSoyData(final List<Ashram_Visit_information__c> ashramVisits) {
        final SoyListData listData = new SoyListData();

        for (final Ashram_Visit_information__c ashramVisit : ashramVisits) {
            final SoyMapData soyMapData = new SoyMapData();
            soyMapData.put("id", ashramVisit.getId());
            soyMapData.put("name", ashramVisit.getName());
            soyMapData.put("checkedIn", ashramVisit.getChecked_In__c() ? "yes" : "no");
            soyMapData.put("visitorName", ashramVisit.getVisitorName__r().getName());
            soyMapData.put("visitPurpose", ashramVisit.getVisit_Purpose__c());
            soyMapData.put("visitDate", SoyRenderer.calendarToString(ashramVisit.getVisit_Date__c()));
            soyMapData.put("accommodation", ashramVisit.getAccommodation__r() != null ? ashramVisit.getAccommodation__r().getName() : "UNASSIGNED");
            listData.add(soyMapData);
        }

        return listData;
    }

    private List<Program_Contact_Relation__c> getParticipantsForProgram(final EnterpriseConnection connection,
                                                                        final String programId) {
        final List<Program_Contact_Relation__c> participants = new ArrayList<>();
        try {
            final String query =
                    "SELECT Id, Name, Campus_Checkin__c, Campus_Checkin_By__c, Campus_Checkin_Time__c, Checked_In__c, " +
                            "Checkout_Meal_Selection__c, Valuables_Scanned__c, Valuables_Scan_Time__c " +
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
                    "SELECT Id, Name, Checked_In__c, VisitorName__r.Name, Visit_Purpose__c, Visit_Date__c, Accommodation__r.Name " +
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

    private String getProgramName(final EnterpriseConnection connection, final String programId) {
        if ("a0A0G00000UQRNH".equals(programId)) {
            return "Samyama";
        }
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
}
