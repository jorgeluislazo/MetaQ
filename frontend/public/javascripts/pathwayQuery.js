/**
 * Created by jorgeluis on 08/06/16.
 */
jQuery(function($) {

    var $search = $('#search'),
    //mappings for parsing Solr field ids to readable text
        li_map2 = {
            "ORF_len"       : "ORF length",
            "start"         : "Start",
            "end"           : "End",
            "strand_sense"  : "Sense",
            "extended_desc" : "Extended Description"
        },
        li_map1= {
            "taxonomy"  : "Taxonomy",
            "COGID"     : "COG id",
            "KEGGID"    : "KEGG id",
            "rpkm"      : "rpkm"
        },
        userSettDef ={
            "searchField" : "product",
            "resultsPerPage": 100,
            "page" : 1,
            "facetField" : ""
        },
        cachedResponse =  null,
        currentFacetFilter = "",
        currentFacetField = "";


    var fetchData = function(url){

        var jqxhr = $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json"
        });


        jqxhr.done(function (response) {

            //before
            var results = $('.results'),
                resultsInfo = $('.resultsInfo'),
                sidePanel = $('#facetPanel'),
                documents = $('.documents');

            if($.trim(documents.html())==''){
                documents.append("<img src='images/loading.gif' class='loading-gif'>");

            }

            documents.fadeOut("fast", function(){
                documents.empty();

                if (response.isFilterSearch) { // keep the current search and update result section
                    $('.filterGuide').remove();
                    $('.resultsInfo').remove();
                    console.log("page: "+ userSettDef["page"] + " results/page: " + userSettDef["resultsPerPage"])
                    for (var j in response.results) {
                        var hit = parseOrfData(response.results[j], j);
                        documents.append(hit)
                    }
                    var glyph = $('<i>').attr("class", "glyphicon glyphicon-remove"),
                        a = $('<a>').css("margin-left","10px").css("cursor", "pointer").text("remove").prepend(glyph).click(function () {
                            restoreResults()
                        }),
                        p = $('<p>').text("Current facet filter: ").append($('<b>').text(currentFacetFilter)),
                        filterGuide = $('<div>').attr('class', 'filterGuide').append(p).append(a);

                    var infoDiv = $('<div>').attr('class', "resultsInfo")
                        .text("Displaying " + response.results.length + " result(s) out of " + response.noOfResults + " results found.");
                    results.prepend(infoDiv).prepend(filterGuide);
                    documents.fadeIn("slow", function(){
                        displayPager(response.start, response.noOfResults, true)
                    });
                } else {
                    //update the cached result and parse all the new information
                    cachedResponse = response;
                    console.log(cachedResponse);

                    resultsInfo.empty();

                    if (cachedResponse.noOfResults == 0) {
                        var query = $('#searchBox').val(),
                            noResultsFound = $('<h4>').attr('class', "noResultsFound").text("No results found for the search: '" + query + "'.");
                        sidePanel.empty();
                        results.append(noResultsFound);
                    }
                    else {
                        resultsInfo.text("Page  " + userSettDef["page"] + " — Total of " + cachedResponse.noOfResults + " open reading frames found.");

                        for (var i in cachedResponse.results) {
                            hit = parseOrfData(cachedResponse.results[i], i);
                            documents.append(hit)
                        }

                        // sidePanel.fadeOut("fast", function(){
                            sidePanel.empty();
                            displayFacets(cachedResponse.facetFields["COGID"], "COG");
                            displayFacets(cachedResponse.facetFields["KEGGID"], "KEGG");
                            displayPager(response.start, response.noOfResults, false);
                            // sidePanel.fadeIn("slow");
                        // });

                        documents.fadeIn("slow");
                    }
                }
            });
        });

        jqxhr.fail(function (data) {
            var message;
            message = data.responseText || data.statusText;
            console.log(message);
        });
        // $('.documents').append("<img src='images/loading.gif'>");
    };

    var parseOrfData = function(data, i){
        var number = $("<small>" + (parseInt(i) + 1 + (userSettDef['page'] -1)*userSettDef['resultsPerPage']) + "</small>"),
            rank = $("<span>").attr('class', 'rank').append(number),
            proteinTitle = $("<h3>").attr("class", "hit-title").text(data.product),
            idTitle = $("<a>").attr("href", addIdLink(data.ORFID)).text(data.ORFID),

            h4 = $('<h4>').prepend(rank).append(proteinTitle).append(" | ORF id: ").append(idTitle),
            orfDiv = $("<div>").attr('class', 'hit').append(h4);

        //append all fields and data to each hit, using helper function
        var appendDataToList = function(ul, list){
            for( var a in list){
                var field = $('<b>').append(list[a]),
                    p = $('<p>').text(data[a] + " ").attr("class", "hit-data"),
                    li = $("<li>").append(field).append(": ").append(p);

                (function(field, value){
                    if(field == "COGID" && p.text() != "N/A "){
                        p.attr("class", "hit-data " + field).append($('<i>').attr("class", "glyphicon glyphicon-new-window"));
                        p.click(function(){
                            window.open('http://www.ncbi.nlm.nih.gov/Structure/cdd/cddsrv.cgi?uid=' + value, '_blank');
                        });
                    }
                    if(field == "KEGGID" && p.text() != "N/A "){
                        p.attr("class", "hit-data " + field).append($('<i>').attr("class", "glyphicon glyphicon-new-window"));
                        p.click(function(){
                            window.open('http://www.genome.jp/dbget-bin/www_bget?' + value, '_blank');
                        });
                    }
                })(a, data[a])
                ul.append(li)
            }
            return ul;
        }
        
        var ul = appendDataToList( $("<ul>"), li_map1);
        ul.append("<br>");
        ul = appendDataToList(ul, li_map2)

        orfDiv.append(ul);
        return orfDiv
    };

    var displayFacets = function(facetResults, fieldType){
        var h4 = $('<h4>').text("Common " + fieldType + " results"),
            ul = $('<ul>'),
            facetDiv = $('#facetPanel').append(h4);


        //make a new scale for sizing the facets
        var oldMin = 99999999, oldMax = 0, oldRange, newMin = 1, newRange = 5, sizeList = 0;
        for (var facet in facetResults) {
            var value = facetResults[facet];
            if (value < oldMin) {
                oldMin = value;
            }
            if (value > oldMax) {
                oldMax = value;
            }
            sizeList++
        }
        oldRange = oldMax - oldMin;

        if(sizeList == 0){
            var p = $('<p>').text("No facets found for " + fieldType + " candidate hits in results");
            facetDiv.append(p)
        }else {
            for (facet in facetResults) {
                var facetClass = normalizeFacetSize(facetResults[facet], oldMin, oldRange, newRange, newMin),
                    a = $('<a>').attr('class', facetClass).text(facet + " "),
                    li = $('<li>').attr('class', 'facetList').append(a);

                (function (facet, li) {
                    li.click(function () {
                        $('.facet-selected').attr("class","facetList");
                        li.attr("class","facetList facet-selected");
                        //save variables and send data
                        userSettDef["page"] = 1;
                        currentFacetFilter = facet;
                        currentFacetField = fieldType;
                        var facetSearchParam = "&facetFilter=" + fieldType + "ID:" + facet;
                        var fetchDataURL = constructURL(facetSearchParam);
                        fetchData(fetchDataURL)
                    });
                })(facet, li);

                ul.append(li)
            }
            facetDiv.append(ul)
        }
    };

    var restoreResults = function(){
        var results = $('.results'),
            sidePanel = $('#facetPanel'),
            resultsInfo = $('.resultsInfo'),
            documents = $('.documents');

        $('.facet-selected').attr("class","facetList");
        documents.fadeOut("fast", function(){
            $('.filterGuide').remove();
            documents.empty();

            resultsInfo.text("Displaying " + cachedResponse.results.length + " result(s) out of " + cachedResponse.noOfResults + " results found.");

            for (var i in cachedResponse.results) {
                var hit = parseOrfData(cachedResponse.results[i], i);
                documents.append(hit)
            }
            documents.fadeIn("slow", function(){
                displayPager(cachedResponse.start, cachedResponse.noOfResults, false)
            });
        });
    };

    var displayPager = function(start, totalResults, isFilterPager){
        var backButton = $("<li>").append($("<a>").attr("aria-label", "Previous").append($('<span>').html("&laquo;"))),
            nextButton = $("<li>").append($("<a>").attr("aria-label", "Next").append($('<span>').html("&raquo;"))),
            container = $(".pagination"),
            allPages = Math.ceil(totalResults / userSettDef["resultsPerPage"]),
            minPage = Math.max(1, userSettDef["page"] - 5),
            maxPage = Math.min(Math.max(userSettDef["page"] + 4, 10) , allPages),
            numPages = maxPage - minPage + 1; // to render
        //renew pages and set handlers
        container.empty()

        var filterOrEmpty
        if(isFilterPager == true){
            filterOrEmpty = "&facetFilter=" + currentFacetField + "ID:" + currentFacetFilter;
        }else{
            filterOrEmpty = ""
        }

        // disable/activate accordingly
        if(userSettDef["page"] == 1){
            backButton.attr("class", "disabled").css("cursor", "pointer");
        }else{
            backButton.click(function(){
                userSettDef["page"]--;
                var fetchDataURL = constructURL(filterOrEmpty);
                fetchData(fetchDataURL);
            }).css("cursor", "pointer");
        }
        if(userSettDef["page"] == maxPage){
            nextButton.css("cursor", "default").attr("class", "disabled");
        }else{
            nextButton.click(function(){
                userSettDef["page"]++;
                var fetchDataURL = constructURL(filterOrEmpty);
                fetchData(fetchDataURL);
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
                        var fetchDataURL = constructURL(filterOrEmpty);
                        fetchData(fetchDataURL);
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
                        var fetchDataURL = constructURL(filterOrEmpty);
                        fetchData(fetchDataURL);
                    });
                })(i,li)
                container.append(li);
            }

        }
        container.append(nextButton);
    }

    var normalizeFacetSize = function(oldValue, oldMin, oldRange, newRange, newMin){
        var newValue = (((oldValue - oldMin) * newRange) / oldRange) + newMin;
        return "facet-size-" + Math.ceil(newValue)
    };

    
    var addIdLink = function(id){
        return "#"
    };

    //search
    $search.submit( function () {
        //reset pagination
        userSettDef["page"] = 1;
        var fetchDataURL = constructURL();
        fetchData(fetchDataURL);
        return false;
    });

    var constructURL = function(extraParam){
        var query = $('#searchBox').val(),
            searchType = $('input:radio[name=df]:checked').val(),
            highQualOnly = $('input:checkbox[name=hq]:checked').val(),
            rpkm = $('#rpkm').val();

        if(highQualOnly === undefined){ highQualOnly = "false" }
        if(extraParam == undefined){ extraParam = ""}

        var fetchDataURL =
            $search.data('search') + query + "&searchField=" + searchType + "&highQualOnly=" + highQualOnly + "&minRPKM=" + rpkm + "&page=" + userSettDef["page"] + extraParam;
        console.log(fetchDataURL);
        return fetchDataURL
    };

    $('#tabs a').click(function (e) {
        console.log("here")
        e.preventDefault();
        $(this).tab('show')

    })

    $('a[data-target="#facetPanel"]').click(function(e){
        var w = $('.main-container').width() * 0.23
        $('.results').css("width",'74%');
        $('.side-bar').width(w);
    })

    $('a[data-target="#clusterPanel"]').click(function(e){
        var w = $('.main-container').width() * 0.54
        $('.results').css("width",'44%');
        $('.side-bar').width(w);
    })

    $('#settings-toggle').click(function(){
        var $settings = $('.settings'),
            $text = function(a){$("#settings-toggle").text(a)};
        if($settings.css("display") == "flex"){
            $settings.css("display", "none");
            $text("Show more settings");
        }else{
            $settings.css("display", "flex");
            $text("Hide more settings");
        }
    });

});