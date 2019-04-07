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
                                $("<option/>", {"value": section.sectionId}).text("Section: " + section.name)
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