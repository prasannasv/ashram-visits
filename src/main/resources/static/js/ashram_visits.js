var ashramVisits = (function() {
  var cachedParticipants;
  var cachedAshramVisitsPerParticipant = {};
  var posting = false;

  $(document).ready(function() {
    render(window.location.hash);
    $(window).on('hashchange', function() {
      // On every hash change the render function is called with the new hash.
      // This is how the navigation of our app happens.
      render(window.location.hash);
    });

    //capture keys for search screen
    $('#currentSearch').on('input', function() {
      filterNameButtons($(this).val());
    });

    let params = (new URL(document.location)).searchParams;
    let programId = params.get("id");

    var fetchParticipantsTask = $.get("/api/participants?pgm_id=" + programId, function(data) {
      cachedParticipants = data;
      console.log("fetched participants successfully");
    });

    var fetchVisitsInfoTask = $.get("/api/visits?pgm_id=" + programId, function(data) {
      $.each(data, function(i, value) {
        cachedAshramVisitsPerParticipant[value.participantId] = value;
      });
      console.log("fetched ashram visits successfully");
    });

    $.when(fetchParticipantsTask, fetchVisitsInfoTask).done(function() {
      var listGroupHtml = '<div class="list-group">';

      $.each(cachedParticipants, function(i, value) {
        listGroupHtml += '<a id="' + value.participantId + '" href="#" class="list-group-item list-group-item-action">' +
          toTitleCase(value.participantFirstName + ' ' + value.participantLastName) + ' (' + toTitleCase(value.sathsangCenterName) + ')' +
          '</a>';
      });
      listGroupHtml += '</div>';

      $(".search_results").html(listGroupHtml);
      filterNameButtons('');
      $('.search_results .list-group-item').click(function() {
        renderParticipantDetails($(this).attr('id'));
      });

      setGroupItemClassPerCheckInStatus();

      $("#progress").addClass("hidden");
      $(".btn-container").removeClass("hidden");
      console.log('done calling both');
    });
  });

  function setGroupItemClassPerCheckInStatus() {
    $(".active").removeClass("active");
    $.each(cachedAshramVisitsPerParticipant, function(key, value) {
      if (value["hasCheckedIn"]) {
        $(".search_results #" + key).addClass("active");
      }
    });
  }

  function toTitleCase(str) {
    return str.replace(/\w\S*/g, function(txt) {
      return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
    });
  }

  function filterNameButtons(currentSearch) {
    $(".search_results a").hide();
    if (currentSearch.length > 2) {
      var searchString = currentSearch.toLowerCase();
      $(".search_results a").each(function(index, value) {
        if ($(this).text().toLowerCase().indexOf(searchString) != -1) {
          $(this).show();
        };
      });
    }
  }

  function renderParticipantDetails(contactId) {
    // show the ashram visit details for the specified contact in the second page
    var ashramVisitInfo = cachedAshramVisitsPerParticipant[contactId];
    if (ashramVisitInfo) {
      if (ashramVisitInfo.needsToPayForStay) {
        alerts.showWarningMsg("Payment for stay not done for the entire duration of the stay. Please direct them to a separate counter to resolve this.");
      } else {
        fillFormFields('edit_visit_info', ashramVisitInfo);
        render('#details');
      }
    } else {
      console.warn("no ashram visit info found for contact id: " + contactId);
      alerts.showWarningMsg("Unable to locate ashram visit info for contact. Please refresh the page and try again and if problem persists, direct them to a separate counter to resolve this.");
    }
  }

  /**
   * Saves the current values of section_details view in the Ashram Visits info object.
   */
  function save() {
    if (posting) {
      return;
    }
    $('#pleaseWaitDialog').modal();
    posting = true;
    var visitInfo = getFormData('edit_visit_info');
    console.log("Posting with params: " + JSON.stringify(visitInfo));

    var jqxhr = $.post('/api/participant/visit', visitInfo);
    jqxhr.done(function(data) {
      console.log("received response for POST: " + JSON.stringify(data));

      if (data.status === "OK") {
        alerts.showSuccessMsg("Successfully updated Ashram Visit information.");
        var participantId = visitInfo["participantId"];
        //console.log("Ashram visit info for participant id: " + participantId + " is: " + JSON.stringify(cachedAshramVisitsPerParticipant[participantId]));
        cachedAshramVisitsPerParticipant[participantId] = visitInfo;
        //console.log("Updated that with " + JSON.stringify(visitInfo));
        setGroupItemClassPerCheckInStatus();
      } else {
        if (data.status_message && data.status_message.length > 0) {
          alerts.showWarningMsg(data.status_message);
        }
      }
    });

    jqxhr.fail(function() {
      parseAjaxFailureMessageAndAlert(jqxhr);
    });

    posting = false;
    $.when(jqxhr).done(function() {
      render("#search");
      $('#pleaseWaitDialog').modal('hide');
    });
  }

  function parseAjaxFailureMessageAndAlert(jqxhr) {
    if (jqxhr && jqxhr.responseText && jqxhr.responseText.length > 0) {
      var contentType = jqxhr.getResponseHeader("content-type") || "";
      if (contentType.indexOf('json') > -1) {
        var data = $.parseJSON(jqxhr.responseText || "");
        if (data.status_message) {
          alerts.showWarningMsg(data.status_message);
        } else {
          alerts.showWarningMsg(jqxhr.responseText);
        }
      } else {
        alerts.showWarningMsg(jqxhr.responseText);
      }
    } else {
      alerts.showWarningMsg("Failed to submit. Please try again.");
    }
  }

  function clearValues(formId) {
    var selector = `#${formId} input, #${formId} select, #${formId} textarea`;
    $(selector).each(function(index, n) {
      if (n.type == "checkbox") {
      } else if (n.type == "radio") {
      } else {
        $(n).val('');
      }
    });
  }

  function fillFormFields(formId, data) {
    clearValues(formId);
    var formObj = $(`#${formId}`);
    $.each(data, function(key, value) {
      var ctrl = $('[name=' + key + ']', formObj);
      switch (ctrl.attr("type")) {
        case "text":
        case "hidden":
          ctrl.val(value);
          break;
        case "bool":
          ctrl.html(value ? "Yes" : "No");
          break;
        case "span":
          ctrl.html(value);
          break;
        case "checkbox":
          ctrl.prop("checked", value);
          break;
        case "radio":
          ctrl.each(function() {
            if ($(this).attr('value') == value) {
              $(this).attr("checked", value);
            }
          });
          break;
        default:
          ctrl.val(value);
      }
    });
  }

  function getFormData(formId) {
    var selector = `#${formId} input, #${formId} select, #${formId} textarea`;
    var formData = {};

    $(selector).each(function(index, n) {
      if (n.type === "checkbox") {
        formData[n.name] = $(n).prop('checked');
      } else if (n.type === "radio") {
        formData[n.name] = $(`#${formId} input[name=${n.name}]:checked`).val();
      } else {
       formData[n.name] = $(n).val();
      }
    });

    return formData;
  }

  function render(url, alert = null) {

    // Get the keyword from the url.
    var temp = url.split('/')[0];
    // Hide current visible section
    $('.section').addClass("hidden");

    var map = {
      // The Homepage.
      '': function() {
        render('#search');
      },

      '#search': function() {
        $(".section_search").removeClass("hidden");
        $('#currentSearch').select();
      },

      '#details': function() {
        $(".section_details").removeClass("hidden");
      }
    };

    // Execute the needed function depending on the url keyword (stored in temp).
    if (map[temp]) {
      map[temp]();
    }

    if (alert != null) {
      if (alert.kind === "success") {
        alerts.showSuccessMsg(alert.message);
      } else {
        alerts.showWarningMsg(alert.message);
      }
    }
  }

  return {
    filterNameButtons: filterNameButtons,
    render: render,
    save: save
  };
})();
