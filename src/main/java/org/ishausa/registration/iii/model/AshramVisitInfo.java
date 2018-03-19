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

    // for gson
    AshramVisitInfo() {}

    public AshramVisitInfo(final Ashram_Visit_information__c sfObject) {
        this.id = sfObject.getId();
        this.participantId = sfObject.getVisitorName__c();
        this.participantName = sfObject.getVisitorName__r().getName();
        this.hasCheckedIn = sfObject.getChecked_In__c();
    }
}
