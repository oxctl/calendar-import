$(function () {
    // This attempts to load the sections contained in this course.
    var loadSections = function () {
        var $select = $("#dest-input");
        // Only load the sections if we found the element
        if ($select.length > 0) {
            var $loading = $("<option disabled>Loading sections...</option>");
            $loading.appendTo($select);
            $.getJSON("./sections")
                .done(function (json) {
                    // If there's just the default section we don't want to give people the option.
                    if (json.length > 1) {
                        $.each(json, function (i, section) {
                            $select.append(
                                // We include both the name and the ID.
                                $("<option/>", {"value": JSON.stringify(section)}).text("Section: " + section.name)
                            );
                        });
                    } else {
                        $select.append($("<option disabled>No sections in course</option>"));
                    }
                })
                .fail(function (jqxhr, textStatus, error) {
                    if (jqxhr.status == 401) {
                        // This gets the user to relogin again because their OAuth token isnt' valid any more.
                        $("#relogin").submit();
                    } else {
                        var $failed = $("<option disabled>Failed to load sections</option>");
                        $select.append($failed);
                    }
                })
                .always(function () {
                    $loading.remove();
                });
        }
    };

    loadSections();

});

// This watches for a string of keys to be typed and then reveals all elements with a class of debug
$(function() {
    var unlockStr = "debug";
    var unlockPos = 0;

    // Function that gets called when right keys are typed.
    var unlockFun = function() {
        $(".debug").show();
    };

    var checkKey = function(key) {
        if (unlockStr.charAt(unlockPos) === key) {
            if (unlockPos < unlockStr.length - 1) {
                unlockPos++;
            } else {
                unlockFun();
                unlockPos = 0;
            }
            return true;
        } else {
            return false;
        }
    };

    $("body").on("keypress", function(e) {
        var key = e.key;
        if (!checkKey(key)) {
            unlockPos = 0;
            checkKey(key);
        }
    });
});