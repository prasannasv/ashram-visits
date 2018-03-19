var alerts = (function() {
    function showWarningMsg(message, timeout = 15000) {
        showMsg(message, "warning", timeout);
    }

    function showSuccessMsg(message, timeout = 3000) {
        showMsg(message, "success", timeout);
    }

    function showMsg(message, kind, timeout) {
        var messageClass = `.${kind}Message`;

        $(`#messageDiv ${messageClass} .message`).html(message);
        // Notes on fadeIn:
        // Relying on css transition for fade in and out didn't work even after following: http://stackoverflow.com/a/13257654/7247103
        // Using jquery based fadeOut works but I haven't figured the right place to put the fadeIn as we seem to prepend the message
        // over existing content.
        $("#alertMessage").append($(`#messageDiv ${messageClass}`).clone());
        $("#alertMessage").show();

        setTimeout(function() {
            $("#alertMessage").fadeOut();
            $("#alertMessage").html('');
        }, timeout);
    }

    return {
        showWarningMsg: showWarningMsg,
        showSuccessMsg: showSuccessMsg
    };
})();
