/**
 * Created by jorgeluis on 25/10/16.
 */
jQuery(function($) {

    var $searchGene = $('#searchGene');
    var $searchPway = $('#searchPway');

    var constructSearchGeneURL = function(){
        var query = $('#searchGeneBox').val(),
            searchType = $('input:radio[name=df]:checked').val(),
            highQualOnly = $('input:checkbox[name=hq]:checked').val(),
            rpkm = $('#rpkm').val();

        if(highQualOnly === undefined){ highQualOnly = "false" }

        var fetchDataURL = $searchGene.data('submit') + query + "&searchField="+searchType + "&highQualOnly="+highQualOnly + "&minRPKM="+rpkm + "&page=1";

        console.log(fetchDataURL);
        return fetchDataURL
    };

    var constructSearchPwayURL = function(){
        var query = $('#searchPwayBox').val(),
            searchType = "pway_name";
        var fetchDataURL = $searchPway.data('submit') + query + "&searchField="+searchType + "&page=1";

        console.log(fetchDataURL);
        return fetchDataURL
    };
    //search
    $searchGene.submit(function () {
        var fetchDataURL = constructSearchGeneURL();
        $(location).attr('href',fetchDataURL);
        return false;
    });

    $searchPway.submit(function () {
        var fetchDataURL = constructSearchPwayURL();
        $(location).attr('href',fetchDataURL);
        return false;
    });

    $('#settings-toggle').click(function(){
        var $settings = $('.settings'),
            $text = function(a){$("#settings-toggle").text(a)};
        if($settings.css("display") == "flex"){
            $settings.css("display", "none");
            $text("Advanced search...");
        }else{
            $settings.css("display", "flex");
            $text("Hide more settings");
        }
    });

});