/**
 * Created by jorgeluis on 08/06/16.
 */
jQuery(function($) {

    var $search = $('#search');


    var fetchData = function(url){

        var jqxhr = $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json"
        });

        jqxhr.done(function(response){
            console.log(response);

        });

        jqxhr.fail(function(data){
            var  message;
            message = data.responseText || data.statusText;
           console.log(message);
        });

    };

    //on submit
    $search.submit( function () {

        var value = $('#searchBox').val();
        var fetchDataURL = $search.data('search') + value;
        fetchData(fetchDataURL);
        console.log(fetchDataURL);

        return false;
    });

});