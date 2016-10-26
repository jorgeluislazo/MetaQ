/**
 * Created by jorgeluis on 25/10/16.
 */
jQuery(function($) {

    var $search = $('#search');

    var constructSearchURL = function(){
        var query = $('#searchBox').val(),
            searchType = $('input:radio[name=df]:checked').val(),
            highQualOnly = $('input:checkbox[name=hq]:checked').val(),
            rpkm = $('#rpkm').val();

        if(highQualOnly === undefined){ highQualOnly = "false" }

        var fetchDataURL = $search.data('submit') + query + "&searchField="+searchType + "&highQualOnly="+highQualOnly + "&minRPKM="+rpkm + "&page=1";

        console.log(fetchDataURL);
        return fetchDataURL
    };


    //search
    $search.submit(function () {
        var fetchDataURL = constructSearchURL();
        $(location).attr('href',fetchDataURL);
        return false;
    });

});