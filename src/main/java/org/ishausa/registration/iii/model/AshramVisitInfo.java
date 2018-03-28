package org.ishausa.registration.iii.model;

import com.sforce.soap.enterprise.sobject.Ashram_Visit_information__c;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * @author prasanna.venkatasubramanian
 */
public class AshramVisitInfo {
    private static final Logger log = Logger.getLogger(AshramVisitInfo.class.getName());

    private String id;
    private String participantId;
    private String participantName;
    private String participantRegion;

    private Boolean needsToPayForStay;
    private String nameTagTrayLocation;

    private Boolean hasCheckedIn;
    private Boolean hasSignedWaiver;
    private Boolean hasCollectedNameTag;

    private String departureDate;
    private String departureDateMealOption;
    private String batchNumber;

    private Boolean isBaggageScreened;
    private Boolean doneMedicalScreening;
    private Boolean isValuablesCollected;

    private String hallLocation;

    private String number;
    private String numberTagTrayLocation;

    // for gson
    AshramVisitInfo() {}

    public AshramVisitInfo(final Ashram_Visit_information__c sfObject) {
        this.id = sfObject.getId();
        this.participantId = sfObject.getVisitorName__c();
        if (sfObject.getVisitorName__r() == null) {
            log.warning("visitorName isn't set for ashram visit with id: " + sfObject.getId());
            this.participantName = "No Contact associated with Ashram Visit: " + sfObject.getId();
        } else {
            this.participantName = sfObject.getVisitorName__r().getName();
        }
        this.participantRegion = sfObject.getSamyama_Participant_Region__c();

        this.needsToPayForStay = sfObject.getSamyama_PaymentFlag__c();
        this.nameTagTrayLocation = sfObject.getSamyama_Name_Tag_Tray_Location__c();

        this.hasCheckedIn = sfObject.getChecked_In__c();
        this.hasSignedWaiver = sfObject.getSamyama_Waiver_Signed__c();
        this.departureDate = sfObject.getSamyama_Departure_Date__c() != null ? sfObject.getSamyama_Departure_Date__c()
                .getTime()
                .toInstant()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_LOCAL_DATE) : "2018-04-07";
        this.departureDateMealOption = sfObject.getSamyama_Departure_Date_Meal_Option__c();
        this.batchNumber = sfObject.getSamyama_Batch_Number__c();

        this.isBaggageScreened = sfObject.getSamyama_Baggage_Screened__c();
        this.doneMedicalScreening = sfObject.getSamyama_Done_Medical_Screening__c();
        this.isValuablesCollected = sfObject.getSamyama_Valuables_Collected__c();

        this.hallLocation = sfObject.getSamyama_Hall_Location__c();

        // this corresponds to samyama number tag
        this.hasCollectedNameTag = sfObject.getSamyama_Name_Tag_Collected__c();
        this.number = sfObject.getSamyama_Number__c();
        this.numberTagTrayLocation = sfObject.getSamyama_Number_Tag_Tray_Location__c();
    }
}
