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
            "resultsPerPage": 200,
            "page" : 1
        },
        cachedResponse =  null,
        currentFacetFilter = "";


    var fetchData = function(url){
        $('.documents').append("<img src='images/loading.gif'>");

        var jqxhr = $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json"
        });


        jqxhr.done(function(response){
            // save/update the results
            // before
            var results = $('.results'),
                sidePanel = $('#facetPanel'),
                documents = $('<div>').attr('class', 'documents');


            if(response.isFilterSearch){ // keep the current search and update result section
                $('.filterGuide').remove();
                $('.resultsInfo').remove();
                $('.documents').remove();
                for (var j in response.results) {
                    var hit = parseOrfData(response.results[j], j);
                    documents.append(hit)
                }
                var a = $('<a>').text("remove").click(function(){
                        restoreResults()
                    }),
                    p = $('<p>').text("Applying the facet filter: ").append($('<b>').text(currentFacetFilter)),
                    filterGuide = $('<div>').attr('class', 'filterGuide').append(p).append(a);

                var resultsInfo = $('<div>').attr('class', "resultsInfo")
                    .text("Displaying " + response.results.length + " result(s) out of " + response.noOfResults + " results found.");
                results.append(filterGuide).append(resultsInfo).append(documents);
            }else {
                //update the cached result and parse all the new information
                cachedResponse = response;
                console.log(cachedResponse);
                results.empty();
                sidePanel.empty();

                if (cachedResponse.noOfResults == 0) {
                    var div = $('<div>').attr('class', "noResultsFound").text("No results found for this search.");
                    results.append(div);
                }
                else {
                    resultsInfo = $('<div>').attr('class', "resultsInfo")
                        .text("Displaying " + cachedResponse.results.length + " result(s) out of " + cachedResponse.noOfResults + " results found.");
                    results.append(resultsInfo);

                    for (var i in cachedResponse.results) {
                        hit = parseOrfData(cachedResponse.results[i], i);
                        documents.append(hit)
                    }
                    results.append(documents);
                    displayFacets(cachedResponse.facetFields["COGID"], "COG");
                    displayFacets(cachedResponse.facetFields["KEGGID"], "KEGG");
                }
            }
            displayPager(response.start, response.noOfResults)
        });

        jqxhr.fail(function(data){
            var  message;
            message = data.responseText || data.statusText;
            console.log(message);
        });

    };

    var parseOrfData = function(data, i){
        var number = $("<small>" + (parseInt(i) + 1) + "</small>"),
            rank = $("<span>").attr('class', 'rank').append(number),
            proteinTitle = $("<a>").attr("href", addProteinLink(data.product)).text(data.product),
            idTitle = $("<a>").attr("href", addIdLink(data.ORFID)).text(data.ORFID),

            h4 = $('<h4>').prepend(rank).append(proteinTitle).append(" | ORF id: ").append(idTitle),
            orfDiv = $("<div>").attr('class', 'hit').append(h4);

        var ul = $("<ul>");
        for( var a in li_map1){
            var cat = $('<b>').append(li_map1[a]),
                li = $("<li>").append(cat).append(": ").append(data[a]);

            ul.append(li)
        }
        ul.append("<br>");

        for( var b in li_map2){
            cat = $('<b>').append(li_map2[b]);
            li = $("<li>").append(cat).append(": ").append(data[b]);

            ul.append(li)
        }
        orfDiv.append(ul);
        return orfDiv
    };

    var displayFacets = function(facetResults, fieldType){
        var h4 = $('<h4>').text(fieldType + " result facets"),
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

                (function (f) {
                    li.click(function () {
                        currentFacetFilter = f;
                        var facetSearchParam = "&facetFilter=" + fieldType + "ID:" + f;
                        var fetchDataURL = constructURL(facetSearchParam);
                        fetchData(fetchDataURL)
                    });
                })(facet);

                ul.append(li)
            }
            facetDiv.append(ul)
        }
    };

    var restoreResults = function(){
        var results = $('.results'),
            sidePanel = $('#facetPanel'),
            documents = $('<div>').attr('class', 'documents');

        results.empty();
        sidePanel.empty();

        var resultsInfo = $('<div>').attr('class', "resultsInfo")
            .text("Displaying " + cachedResponse.results.length + " result(s) out of " + cachedResponse.noOfResults + " results found.");
        results.append(resultsInfo);

        for (var i in cachedResponse.results) {
            var hit = parseOrfData(cachedResponse.results[i], i);
            documents.append(hit)
        }
        results.append(documents);
        displayFacets(cachedResponse.facetFields["COGID"], 'COG');
        displayFacets(cachedResponse.facetFields["KEGGID"], 'KEGG');

    };

    var displayPager = function(start, totalResults){
        var backButton = $("<li>").append($("<a>").attr("href", "#").attr("aria-label", "Previous").append($('<span>').html("&laquo;"))),
            nextButton = $("<li>").append($("<a>").attr("href", "#").attr("aria-label", "Next").append($('<span>').html("&raquo;"))),
            container = $(".pagination"),
            allPages = Math.ceil(totalResults / userSettDef["resultsPerPage"]),
            minPage = Math.max(1, userSettDef["page"] - 5),
            maxPage = Math.min(Math.max(userSettDef["page"] + 4, 10) , allPages),
            numPages = maxPage - minPage; // to render
        //renew pages and set handlers
        container.empty()

        backButton.click(function(){
            userSettDef["page"]--;
            var fetchDataURL = constructURL();
            fetchData(fetchDataURL);
        });

        nextButton.click(function(){
            userSettDef["page"]++;
            var fetchDataURL = constructURL();
            fetchData(fetchDataURL);
        });

        if(userSettDef["page"] = 1){
            backButton.attr("class", "disabled")
        }

        container.append(backButton)

        if(allPages <= 10){ //render all
            for(var i = 1; i <= numPages; i++){
                var li = $("<li>").append($("<a>").text(i));
                if(i == userSettDef["page"]){
                    li.attr("class", "active")
                }
                container.append(li);
            }
        }else{ // render currPage +/- 4
            for(i = minPage; i <= maxPage; i++){
                li = $("<li>").append($("<a>").text(i));
                if(i == userSettDef["page"]){
                    li.attr("class", "active")
                }
                container.append(li);
            }

        }
        container.append(nextButton);
        console.log(start)
        console.log(numPages)
    }

    var normalizeFacetSize = function(oldValue, oldMin, oldRange, newRange, newMin){
        var newValue = (((oldValue - oldMin) * newRange) / oldRange) + newMin;
        return "facet-size-" + Math.ceil(newValue)
    };


    var addProteinLink = function(name){
        return "#"
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
        e.preventDefault();
        $(this).tab('show')
    })

});