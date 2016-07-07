/**
 * Created by jorgeluis on 08/06/16.
 */
jQuery(function($) {

    var $search = $('#search'),
    //Arbitrary mappings
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
        userSettingsDefaults ={
            "searchField" : "product"
        };


    var fetchData = function(url){

        $('.documents').append("<img src='images/loading.gif'>");

        var jqxhr = $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json"
        });


        jqxhr.done(function(response){
            console.log(response);
            //before
            var documents = $('.documents'),
                sidePanel = $('#facetPanel');
            documents.empty();
            sidePanel.empty();

            if(response.noOfResults == 0){
                var div = $('<div>').attr('class', "noResultsFound").text("No results found for this search.");
                documents.append(div);
            }
            else{
                displayResultsInfo(response.noOfResults, response.results.length);
                for(var i in response.results){
                    parseOrfData(response.results[i], i);
                }
                displayCogFacets(response.facetFields["COGID"]);
                displayKeggFacets(response.facetFields["KEGGID"]);
            }

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

        $('.documents').append(orfDiv);

    };

    var displayResultsInfo = function(totalCount, displayCount){
        var div = $('<div>').attr('class', "resultsInfo").text("Displaying " + displayCount + " result(s) out of " + totalCount + " results found.");
        $('.documents').append(div);
    };

    var displayCogFacets = function(facetResults){
        var h4 = $('<h4>').text("COG result facets"),
            ul = $('<ul>'),
            facetDiv = $('#facetPanel').append(h4);

        //make a new scale for sizing the facets
        var oldMin = 99999999, oldMax = 0, oldRange, newMin = 1, newRange = 5;
        for (var facet in facetResults){
            var value = facetResults[facet];
            if (value < oldMin){
                oldMin = value;
            }
            if (value > oldMax){
                oldMax = value;
            }
        }
        oldRange = oldMax - oldMin;

        for (facet in facetResults){
            var facetClass = normalizeFacetSize(facetResults[facet],oldMin,oldRange,newRange,newMin),
                a = $('<a>').attr('class' , facetClass).text(facet + " "),
                li = $('<li>').attr('class','facetList').append(a);

            console.log(normalizeFacetSize(facetResults[facet],oldMin,oldRange,newRange,newMin));
            ul.append(li)
        }
        facetDiv.append(ul)

    };

    var displayKeggFacets = function(facetResults){
        var h4 = $('<h4>').text("KEGG result facets"),
            ul = $('<ul>'),
            facetDiv = $('#facetPanel').append(h4);

        for (var value in facetResults){
            var li = $('<li>').attr('class','facetList').text(value + " (" + facetResults[value] + ")");
            ul.append(li)
        }
        facetDiv.append(ul)

    };

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

    //on submit
    $search.submit( function () {
        var query = $('#searchBox').val(),
            searchType = $('input:radio[name=df]:checked').val(),
            highQualOnly = $('input:checkbox[name=hq]:checked').val(),
            rpkm = $('#rpkm').val();

        if(highQualOnly === undefined){
            highQualOnly = "false"
        }

        var fetchDataURL = $search.data('search') + query + "&searchField=" + searchType + "&highQualOnly=" + highQualOnly + "&minRPKM=" + rpkm;
        console.log(fetchDataURL);
        fetchData(fetchDataURL);

        return false;
    });

    $('#tabs a').click(function (e) {
        e.preventDefault();
        $(this).tab('show')
    })

});