var ashramVisits = (function() {
  var cachedParticipants;
  var cachedAshramVisits;

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

    $.get("/api/participants", function(data) {
      cachedParticipants = data;
    });

    $.get("/api/visits", function(data) {
      cachedAshramVisits = data;
    });

  });

  function filterNameButtons(currentSearch) {
    $(".search_results a").hide();
    if (currentSearch.length > 2) {
      var searchPattern = new RegExp('^' + currentSearch, 'i');
      $(".search_results a").each(function(index, value) {
        if (searchPattern.test($(this).text())) {
          $(this).show();
        };
      });
    }
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
})();

