package org.ishausa.registration.iii;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.SaveResult;
import com.sforce.soap.enterprise.sobject.Ashram_Visit_information__c;
import com.sforce.soap.enterprise.sobject.Program_Contact_Relation__c;
import com.sforce.soap.enterprise.sobject.Program__c;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.ConnectionException;
import org.ishausa.registration.iii.http.NameValuePairs;
import org.ishausa.registration.iii.model.AshramVisitInfo;
import org.ishausa.registration.iii.model.ProgramParticipantRecord;
import org.ishausa.registration.iii.model.Status;
import org.ishausa.registration.iii.model.StatusCode;
import org.ishausa.registration.iii.renderer.SoyRenderer;
import org.ishausa.registration.iii.security.HttpsEnforcer;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    private static final String VISITS_START_DATE_BEGIN = "2018-03-27T00:00:00Z";
    private static final String VISITS_START_DATE_END = "2018-04-09T00:00:00Z";
    private static final String DAY_AFTER_PROGRAM_START = "2018-04-02T00:00:00Z";
    private static final String DAY_BEFORE_PROGRAM_END = "2018-04-05T00:00:00Z";

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
        get("/api/visits/all", app::getAllAshramVisits);

        post("/api/participant/visit", app::updateVisitInfo);

        exception(Exception.class, ((exception, request, response) -> {
            log.info("Exception: " + exception + " stack: " + Throwables.getStackTraceAsString(exception));
            response.header("Content-Type", "application/json");
            response.status(500);
            response.body(GSON.toJson(new Status(StatusCode.FAILURE, exception.getMessage())));
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
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final String json = GSON.toJson(participants.stream().map(ProgramParticipantRecord::new).collect(Collectors.toList()));
        final long translationTimeMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        log.info("Json transformation time (in ms): " + translationTimeMillis);
        return json;
    }

    private String getAshramVisitsForProgram(final Request request, final Response response) {
        response.header("Content-Type", "application/json");
        final String programId = request.queryParams("pgm_id");
        final List<Ashram_Visit_information__c> ashramVisits = getAshramVisitsForProgram(connection, programId);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final String json = GSON.toJson(ashramVisits.stream().map(AshramVisitInfo::new).collect(Collectors.toList()));
        final long translationTimeMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        log.info("Json transformation time (in ms): " + translationTimeMillis);
        return json;
    }

    private String getAllAshramVisits(final Request request, final Response response) {
        response.header("Content-Type", "application/json");

        final List<Ashram_Visit_information__c> ashramVisits = getAshramVisitsForDateRange(connection, VISITS_START_DATE_BEGIN, VISITS_START_DATE_END);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final List<AshramVisitInfo> ashramVisitInfos = ashramVisits.stream().map(AshramVisitInfo::new).collect(Collectors.toList());
        log.info("Fetched ashram visits: " + ashramVisitInfos.size());
        final String json = GSON.toJson(ashramVisitInfos);
        final long translationTimeMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        log.info("Json transformation time (in ms): " + translationTimeMillis);
        return json;
    }

    private List<Program_Contact_Relation__c> getParticipantsForProgram(final EnterpriseConnection connection,
                                                                        final String programId) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final List<Program_Contact_Relation__c> participants = new ArrayList<>();
        try {
            final String query =
                    "SELECT Id, Participant__r.Id, Participant__r.FirstName, Participant__r.LastName, Participant__r.Sathsang_Center__r.Name " +
                            "FROM Program_Contact_Relation__c " +
                            "WHERE Program__c = '" + programId + "'";
            final QueryResult queryResult = connection.query(query);
            log.info("getParticipants query execution time (in ms): " + stopwatch.elapsed(TimeUnit.MILLISECONDS));

            for (final SObject record : queryResult.getRecords()) {
                participants.add((Program_Contact_Relation__c) record);
            }
        } catch (final ConnectionException e) {
            log.log(Level.SEVERE, "Exception querying Program_Contact_Relation__c for programId: " + programId, e);
        }
        return participants;
    }

    /**
     * Returns exactly one AshramVisit info that corresponds to the program even if there are multiple associated with the
     * program.
     */
    private List<Ashram_Visit_information__c> getAshramVisitsForProgram(final EnterpriseConnection connection,
                                                                        final String programId) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final List<Ashram_Visit_information__c> ashramVisits = new ArrayList<>();
        try {
            final String query =
                    "SELECT Id, VisitorName__c, VisitorName__r.Name, samyama_PaymentFlag__c, Checked_In__c, " +
                            "Visit_Date__c, Check_Out_Date__c, " +
                            "Samyama_Baggage_Screened__c, Samyama_Batch_Number__c, Samyama_Departure_Date__c, " +
                            "Samyama_Participant_Region__c, " +
                            "Samyama_Departure_Date_Meal_Option__c, Samyama_Name_Tag_Tray_Location__c, " +
                            "Samyama_Done_Medical_Screening__c, Samyama_Hall_Location__c, Samyama_Name_Tag_Collected__c, " +
                            "Samyama_Number__c, Samyama_Number_Tag_Tray_Location__c, Samyama_Valuables_Collected__c, " +
                            "Samyama_Waiver_Signed__c " +
                            "FROM Ashram_Visit_information__c " +
                            "WHERE Program__c = '" + programId + "' " +
                            "AND Check_Out_Date__c >= " + DAY_BEFORE_PROGRAM_END +
                            " AND Visit_Date__c <= " + DAY_AFTER_PROGRAM_START;
            log.info("About to run query: " + query);
            final QueryResult queryResult = connection.query(query);
            log.info("getAshramVisits query execution time (in ms): " + stopwatch.elapsed(TimeUnit.MILLISECONDS));

            for (final SObject record : queryResult.getRecords()) {
                ashramVisits.add((Ashram_Visit_information__c) record);
            }
        } catch (final ConnectionException e) {
            log.log(Level.SEVERE, "Exception querying Ashram_Visit_information__c for programId: " + programId, e);
        }
        return ashramVisits;
    }

    private List<Ashram_Visit_information__c> getAshramVisitsForDateRange(final EnterpriseConnection connection,
                                                                          final String visitsStartDateBegin,
                                                                          final String visitsStartDateEnd) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final List<Ashram_Visit_information__c> ashramVisits = new ArrayList<>();
        try {
            final String query =
                    "SELECT Id, VisitorName__c, VisitorName__r.Name, Visit_Date__c, Check_Out_Date__c " +
                            "FROM Ashram_Visit_information__c " +
                            "WHERE Check_Out_Date__c >= " + visitsStartDateBegin +
                            " AND VisitorName__c != '0030G00002dEG7NQAW'" + // Program Luggage Blocker
                            " AND VisitorName__c != '0030G00002an2OrQAI'" + // Program Luggage Blocker
                            " ORDER BY Visit_Date__c";
            log.info("Running query: " + query);
            final QueryResult queryResult = connection.query(query);
            log.info("getAshramVisits query execution time (in ms): " + stopwatch.elapsed(TimeUnit.MILLISECONDS));

            for (final SObject record : queryResult.getRecords()) {
                ashramVisits.add((Ashram_Visit_information__c) record);
            }
        } catch (final ConnectionException e) {
            log.log(Level.SEVERE, "Exception querying Ashram_Visit_information__c for visit date range: " +
                    visitsStartDateBegin + " to " + visitsStartDateEnd, e);
        }
        return ashramVisits;
    }

    private String updateVisitInfo(final Request request, final Response response)
            throws IOException, ConnectionException {
        response.header("Content-Type", "application/json");
        final String content = request.body();
        final Map<String, List<String>> params = NameValuePairs.splitParams(content);
        // Validate input
        final String ashramVisitId = NameValuePairs.nullSafeGetFirst(params, "id");
        if (Strings.isNullOrEmpty(ashramVisitId)) {
            return GSON.toJson(new Status(StatusCode.FAILURE, "Request is missing ashram visit id param"));
        }

        // todo check if the needsToPayForStay is not set.
        // a participant may have multiple Ashram Visits around the program.
        // the check in status needs to be updated in all of them. TODO: Prasanna
        // the other fields need to be updated only in the Ashram Visit object that corresponds to the program dates.
        final List<String> fieldsToNull = new ArrayList<>();
        final Ashram_Visit_information__c visitInfo = new Ashram_Visit_information__c();
        visitInfo.setId(ashramVisitId);
        final String batchNumber = NameValuePairs.nullSafeGetFirst(params, "batchNumber");
        if (!Strings.isNullOrEmpty(batchNumber)) {
            visitInfo.setSamyama_Batch_Number__c(batchNumber.toUpperCase());
        } else {
            fieldsToNull.add("Samyama_Batch_Number__c");
        }
        visitInfo.setChecked_In__c(getBoolean(params, "hasCheckedIn"));
        visitInfo.setSamyama_Waiver_Signed__c(getBoolean(params, "hasSignedWaiver"));
        final String departureDate = NameValuePairs.nullSafeGetFirst(params, "departureDate");
        final Calendar departureDateCal = Calendar.getInstance();
        departureDateCal.setTime(Date.from(LocalDate.parse(departureDate).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        visitInfo.setSamyama_Departure_Date__c(departureDateCal);
        visitInfo.setSamyama_Departure_Date_Meal_Option__c(NameValuePairs.nullSafeGetFirst(params, "departureDateMealOption"));
        visitInfo.setSamyama_Name_Tag_Collected__c(getBoolean(params, "hasCollectedNameTag"));
        visitInfo.setSamyama_Baggage_Screened__c(getBoolean(params, "isBaggageScreened"));
        visitInfo.setSamyama_Done_Medical_Screening__c(getBoolean(params, "doneMedicalScreening"));
        visitInfo.setSamyama_Valuables_Collected__c(getBoolean(params, "isValuablesCollected"));
        final String hallLocation = NameValuePairs.nullSafeGetFirst(params, "hallLocation");
        if (!Strings.isNullOrEmpty(hallLocation)) {
            visitInfo.setSamyama_Hall_Location__c(hallLocation);
        } else {
            fieldsToNull.add("Samyama_Hall_Location__c");
        }

        visitInfo.setFieldsToNull(fieldsToNull.toArray(new String[] {}));
        log.info("About to save ashram visit info for id: " + ashramVisitId +
                " with check in status: " + visitInfo.getChecked_In__c() +
                ", batchNumber: " + visitInfo.getSamyama_Batch_Number__c() +
                ", hasSignedWaiver: " + visitInfo.getSamyama_Waiver_Signed__c() +
                ", departureDate: " + visitInfo.getSamyama_Departure_Date__c()
                    .getTime()
                    .toInstant()
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE) +
                ", departureDateMealOption: " + visitInfo.getSamyama_Departure_Date_Meal_Option__c() +
                ", hasCollectedNameTag: " + visitInfo.getSamyama_Name_Tag_Collected__c() +
                ", isBaggageScreened: " + visitInfo.getSamyama_Baggage_Screened__c() +
                ", doneMedicalScreening: " + visitInfo.getSamyama_Done_Medical_Screening__c() +
                ", hallLocation: " + visitInfo.getSamyama_Hall_Location__c() +
                ", isValuablesCollected: " + visitInfo.getSamyama_Valuables_Collected__c() +
                ", fieldsToNull: " + Arrays.asList(visitInfo.getFieldsToNull())
        );

        final SaveResult[] saveResults = connection.update(new Ashram_Visit_information__c[] {visitInfo});
        if (!saveResults[0].isSuccess()) {
            if (saveResults[0].getErrors().length == 1) {
                throw new IOException("Check-in failed: " + saveResults[0].getErrors()[0].getMessage());
            } else {
                throw new IOException("Check-in failed: " + saveResults[0]);
            }
        } else {
            log.info("Saved successfully");
        }

        return GSON.toJson(new Status(StatusCode.OK, ""));
    }

    private Boolean getBoolean(final Map<String, List<String>> params, final String fieldName) {
        return "true".equals(NameValuePairs.nullSafeGetFirst(params, fieldName));
    }
}
