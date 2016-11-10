/**
 * Created by jorgeluis on 08/11/16.
 */
jQuery(function($) {
    
    var $search = $('#search'),
        $cache = $('.results').data('cache'),
        $sidePanel = $('#facetPanel'),
        $results = $('.results'),
        $resultsInfo = $('.resultsInfo'),
        $documents = $('.documents'),
        $exportButton = $('#exportButton');

    var fetchData = function(url){

        var jqxhr = $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json"
        });

        jqxhr.done(function (response) {
            console.log(response)

        });

        jqxhr.fail(function (data) {
            var message;
            message = data.responseText || data.statusText;
            console.log(message);
        });
    };

    //init the search upon loading the page
    $('#searchBox').val($cache.match(/^.*?(?=&)/));
    fetchData($search.data('searchurl') + $cache);
    // fetchClusters($('#clusterPanel').data("request") + $cache)
    $('[data-toggle="tooltip"]').tooltip();

});