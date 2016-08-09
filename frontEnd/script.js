$(function() {
    $("#form").keypress(function (e) {
        if (e.which == 13) {
            var search = $("input[type=text]").val();
            alert("SEARCH RESULTS: \n\n" + search);
        }
    });
});


