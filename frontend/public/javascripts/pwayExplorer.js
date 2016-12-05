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
        $exportButton = $('#exportButton'),
        userSettDef ={
            "searchField" : "pway_name",
            "resultsPerPage": 20,
            "page" : 1,
            "facetField" : ""
        },
        cachedResponse =  null;

    var fetchData = function(url){

        var jqxhr = $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json"
        });

        jqxhr.done(function (response) {
            console.log(response);
            //update the cached result and parse all the new information
            cachedResponse = response;
            $('.filterGuide').remove();
            $resultsInfo.empty();

            if (cachedResponse.noOfResults == 0) {
                var query = $('#searchBox').val();

                if(cachedResponse.error != undefined){
                    var errorResponse = $('<h4>').attr('class', "noResultsFound").text("Query syntax error for the search term: '" + query + "'.");
                }else{
                    errorResponse = $('<h4>').attr('class', "noResultsFound").text("No results found for the search term: '" + query + "'.");
                }
                $sidePanel.empty();
                $resultsInfo.empty();
                $documents.append(errorResponse);
                $documents.fadeIn("fast");
            }
            else {
                $resultsInfo.text("Page  " + userSettDef["page"] + " — Total of " + cachedResponse.noOfResults + " pathways found.");

                for (var i in cachedResponse.results) {
                    var hit = parsePwayData(cachedResponse.results[i], i);
                    $documents.append(hit)
                }
                $sidePanel.empty();
                displayPager(response.start, response.noOfResults, false);
                $documents.fadeIn("slow");
            }

        });

        jqxhr.fail(function (data) {
            var message;
            message = data.responseText || data.statusText;
            console.log(message);
        });
    };

    var parsePwayData = function(data, i){
        //title fields and container
        var number = $("<small>" + (parseInt(i) + 1 + (userSettDef['page'] -1)*userSettDef['resultsPerPage']) + "</small>"),
            rank = $("<span>").attr('class', 'rank').append(number),
            pwayTitle = $("<h3>").attr("class", "hit-title").text(data.pway_name),
            idTitle = $("<a>").attr("target", "blank").attr("href", "http://metacyc.org/META/NEW-IMAGE?type=NIL&object="+data.pway_id+"&redirect=T").text(data.pway_id),

            h4 = $('<h4>').prepend(rank).append(pwayTitle).append("&nbsp;&nbsp;").append(idTitle).append(" ").append($('<small>').attr("class", "glyphicon glyphicon-new-window")),
            pwayDiv = $("<div>").attr('class', 'hit').append(h4);

        var rxnTotal = $('<p>').text(data.rxn_total).attr("class", "hit-data").prepend($("<b>").text("Total reactions:\t").attr("style", "margin-left:17px"));
        var sampleRuns = $('<ul>').append($("<b>").text("Associated library runs:").attr("style", "margin-left:11px"))

        for (var a in data.sample_runs){
            var run = $('<li>').append($('<p>').text(data.sample_runs[a]).attr("class", "sampleRun"))
            sampleRuns.append(run)
        }
        var orfs = $('<button>').attr("class", "btn btn-info").attr("type", "button").text("explore associated ORFs (" + data.orfs.length + ")")
        orfs.click(function(){
            var pwayid = data.pway_id
            var fetchDataURL = constructORFsearchURL(pwayid)
            window.open(fetchDataURL, "_blank")
            return false;

        })

        return pwayDiv.append(rxnTotal).append(sampleRuns).append(orfs)
    };

    var displayPager = function(start, totalResults, isFilterPager){
        var backButton = $("<li>").append($("<a>").attr("href", "#").attr("aria-label", "Previous").append($('<span>').html("&laquo;"))),
            nextButton = $("<li>").append($("<a>").attr("href", "#").attr("aria-label", "Next").append($('<span>').html("&raquo;"))),
            container = $(".pagination"),
            allPages = Math.ceil(totalResults / userSettDef["resultsPerPage"]),
            minPage = Math.max(1, userSettDef["page"] - 5),
            maxPage = Math.min(Math.max(userSettDef["page"] + 4, 10) , allPages),
            numPages = maxPage - minPage + 1; // to render

        //renew pages and set handlers
        container.empty()

        if(userSettDef["page"] == 1){
            backButton.attr("class", "disabled").css("cursor", "pointer");
        }else{
            backButton.click(function(){
                userSettDef["page"]--;
                var fetchDataURL = constructPwaySearchURL();
                $documents.fadeOut("slow", function(){
                    fetchData(fetchDataURL);
                });
            }).css("cursor", "pointer");
        }
        if(userSettDef["page"] == maxPage){
            nextButton.css("cursor", "default").attr("class", "disabled");
        }else{
            nextButton.click(function(){
                userSettDef["page"]++;
                var fetchDataURL = constructPwaySearchURL();
                $documents.fadeOut("slow", function(){
                    fetchData(fetchDataURL);
                });
            }).css("cursor", "pointer");
        }

        container.append(backButton)

        if(allPages <= 10){ //render all
            for(var i = 1; i <= numPages; i++){
                var li = $("<li>").append($("<a>").attr("href", "#").text(i));
                if(i == userSettDef["page"]){
                    li.attr("class", "active")
                }
                (function(a,b){
                    b.click(function(){
                        userSettDef["page"] = a;
                        var fetchDataURL = constructPwaySearchURL();
                        $documents.fadeOut("slow", function(){
                            fetchData(fetchDataURL);
                        });
                    });
                })(i,li)
                container.append(li);
            }
        }else{ // render currPage +/- 4
            for(i = minPage; i <= maxPage; i++){
                li = $("<li>").append($("<a>").attr("href", "#").text(i));
                if(i == userSettDef["page"]){
                    li.attr("class", "active")
                }
                (function(a,b){
                    b.click(function(){
                        userSettDef["page"] = a;
                        var fetchDataURL = constructPwaySearchURL();
                        $documents.fadeOut("slow", function(){
                            fetchData(fetchDataURL);
                        });
                    });
                })(i,li)
                container.append(li);
            }
        }
        container.append(nextButton);
    }

    //default for this module
    var constructPwaySearchURL = function(extraParam, isClusterSearch, ajax){
        if(highQualOnly === undefined){ highQualOnly = "false" }
        if(extraParam == undefined){ extraParam = ""} //any extra params such as facetSearch
        if(isClusterSearch == undefined){isClusterSearch = false} //do we need a double query for the cluster?
        if(ajax == undefined){ajax = true} //unless specified, do the search as an ajax (no reload)

        var query = $('#searchBox').val(),
            searchType = "pway_name", //TODO: settings?
            highQualOnly = $('input:checkbox[name=hq]:checked').val(),
            rpkm = $('#rpkm').val();

        if(isClusterSearch){
            var fetchDataURL =
                $('#clusterPanel').data("request") + query + "&searchField="+searchType + "&highQualOnly="+highQualOnly + "&minRPKM="+rpkm + extraParam;
        }else {
            fetchDataURL =
                (ajax ? $search.data('searchurl') : $search.data('submit')) + query + "&searchField="+searchType + "&page=" + userSettDef["page"] + extraParam;
        }
        console.log($cache)
        console.log(fetchDataURL);
        return fetchDataURL
    };

    //link to Genes
    var constructORFsearchURL = function(pwayid){
        return $search.data("jumpurl") + pwayid +  "&searchField=pway" + "&highQualOnly=false" +"&page=1"
    };

    $search.on("submit", function(){
        var fetchDataURL = constructPwaySearchURL(undefined, undefined, false);
        $(location).attr('href',fetchDataURL);
        return false;
    });


    //init the search upon loading the page
    $('#searchBox').val($cache.match(/^.*?(?=&)/));
    fetchData($search.data('searchurl') + $cache);
    // fetchClusters($('#clusterPanel').data("request") + $cache)
    $('[data-toggle="tooltip"]').tooltip();

});