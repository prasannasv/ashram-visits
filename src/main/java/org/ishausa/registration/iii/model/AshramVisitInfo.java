package org.ishausa.registration.iii.model;

import com.sforce.soap.enterprise.sobject.Ashram_Visit_information__c;

/**
 * @author prasanna.venkatasubramanian
 */
public class AshramVisitInfo {
    private String id;
    private String participantId;
    private String participantName;

    private Boolean hasCheckedIn;
    private Boolean hasSignedWaiver;
    private Boolean hasCollectedNameTag;
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
        this.participantName = sfObject.getVisitorName__r().getName();

        this.hasCheckedIn = sfObject.getChecked_In__c();
        this.hasSignedWaiver = sfObject.getSamyama_Waiver_Signed__c();
        this.hasCollectedNameTag = sfObject.getSamyama_Name_Tag_Collected__c();
        this.departureDateMealOption = sfObject.getSamyama_Departure_Date_Meal_Option__c();
        this.batchNumber = sfObject.getSamyama_Batch_Number__c();

        this.isBaggageScreened = sfObject.getSamyama_Baggage_Screened__c();
        this.doneMedicalScreening = sfObject.getSamyama_Done_Medical_Screening__c();
        this.isValuablesCollected = sfObject.getSamyama_Valuables_Collected__c();

        this.hallLocation = sfObject.getSamyama_Hall_Location__c();

        this.number = sfObject.getSamyama_Number__c();
        this.numberTagTrayLocation = sfObject.getSamyama_Number_Tag_Tray_Location__c();
    }
}
