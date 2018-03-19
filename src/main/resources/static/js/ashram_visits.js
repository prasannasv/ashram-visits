var ashramVisits = (function() {
  var cachedParticipants;
  var cachedAshramVisitsPerParticipant = {};

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
        value['VisitorName'] = value['VisitorName__r']['Name'];
        cachedAshramVisitsPerParticipant[value.VisitorName__c] = value;
      });
      console.log("fetched ashram visits successfully");
    });

    $.when(fetchParticipantsTask, fetchVisitsInfoTask).done(function() {
      var listGroupHtml = '<div class="list-group">';

      $.each(cachedParticipants, function(i, value) {
        listGroupHtml += '<a id="' + value.Participant__r.Id + '" href="#" class="list-group-item list-group-item-action">' +
          toTitleCase(value.Participant__r.FirstName + ' ' + value.Participant__r.LastName) +
          '</a>';
      });
      listGroupHtml += '</div>'

      $(".search_results").html(listGroupHtml);
      filterNameButtons('');
      $('.search_results .list-group-item').click(function() {
        renderParticipantDetails($(this).attr('id'));
      });

      $(".btn-container").removeClass("hidden");
      console.log('done calling both');
    });
  });

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
      fillFormFields('edit_visit_info', ashramVisitInfo);
      render('#details');
    } else {
      console.warn("no ashram visit info found for contact id: " + contactId);
    }
  }

  function fillFormFields(formId, data) {
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
      showMsg(alert.message, alert.kind, 3000);
    }
  }

  return {
    filterNameButtons: filterNameButtons,
    render: render
  };
})();
