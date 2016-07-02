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
            var documents = $('.documents');
            documents.empty();

            if(response.noOfResults == 0){
                var div = $('<div>').attr('class', "noResultsFound").text("No results found for this search.");
                documents.append(div);
            }
            else{
                displayResultsInfo(response.noOfResults, response.results.length);
                for(var i in response.results){
                    parseOrfData(response.results[i], i);
                }
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
        console.log(orfDiv)
    };

    var displayResultsInfo = function(totalCount, displayCount){
        var div = $('<div>').attr('class', "resultsInfo").text("Displaying " + displayCount + " result(s) out of " + totalCount + " results found.");
        $('.documents').append(div);
    };


    var addProteinLink = function(name){
        return "#"
    };

    var addIdLink = function(id){
        return "#"
    };

    //on submit
    $search.submit( function () {
        var searchType = $('input:radio[name=df]:checked').val();
        var value = $('#searchBox').val();
        var fetchDataURL = $search.data('search') + value + "&searchField=" + searchType;
        console.log(fetchDataURL);
        fetchData(fetchDataURL);


        return false;
    });

});