package org.ishausa.registration.iii.model;

import com.sforce.soap.enterprise.sobject.Program_Contact_Relation__c;

/**
 * @author prasanna.venkatasubramanian
 */
public class ProgramParticipantRecord {
    private String id;
    private String participantId;
    private String participantFirstName;
    private String participantLastName;
    private String sathsangCenterName;

    // for gson
    ProgramParticipantRecord() {}

    public ProgramParticipantRecord(final Program_Contact_Relation__c sfObject) {
        this.id = sfObject.getId();
        this.participantId = sfObject.getParticipant__r().getId();
        this.participantFirstName = sfObject.getParticipant__r().getFirstName();
        this.participantLastName = sfObject.getParticipant__r().getLastName();
        this.sathsangCenterName = sfObject.getParticipant__r().getSathsang_Center__r() != null ?
                sfObject.getParticipant__r().getSathsang_Center__r().getName() : "N/A";
    }
}
