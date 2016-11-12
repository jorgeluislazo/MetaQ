/**
 * Created by jorgeluis on 08/06/16.
 */
jQuery(function($) {

    var $search = $('#search'),
        $cache = $('.results').data('cache'),
        $sidePanel = $('#facetPanel'),
        $results = $('.results'),
        $resultsInfo = $('.resultsInfo'),
        $documents = $('.documents'),
        $exportButton = $('#exportButton'),
    //mappings for parsing Solr field ids to readable text
        li_map2 = {
            "ORF_len"       : "ORF length",
            "start"         : "Start",
            "end"           : "End",
            "strand_sense"  : "Sense",
            "extended_desc" : "Secondary Description"
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
        //clientside variables to save
        cachedResponse =  null,
        currentFacetFilter = "",
        currentFacetField = "",
        currentClusterFilter = "";

    var fetchData = function(url){

        var jqxhr = $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json"
        });

        jqxhr.done(function (response) {
                $documents.empty();
                if (response.isFilterSearch || response.isClusterFilter) { // keep the current search and update result section
                    $('.filterGuide').remove();
                    $resultsInfo.empty();
                    for (var j in response.results) {
                        var hit = parseOrfData(response.results[j], j);
                        $documents.append(hit)
                    }
                    var constructFilterGuide = function(p){
                        var glyph = $('<i>').attr("class", "glyphicon glyphicon-remove"),
                            a = $('<a>').css("margin-left","10px").css("cursor", "pointer").text("remove").prepend(glyph).click(function () {restoreResults()}),
                            filterGuide = $('<div>').attr('class', 'filterGuide').append(p).append(a);
                        return filterGuide;}

                    if(response.isFilterSearch){
                        var p = $('<p>').text("All results with filter facet: ").append($('<b>').text(currentFacetFilter)),
                        filterGuide = constructFilterGuide(p);
                    }
                    if(response.isClusterFilter){
                        p = $('<p>').text("Current cluster filter: ").append($('<b>').text(currentClusterFilter));
                        filterGuide = constructFilterGuide(p);
                    }

                    $resultsInfo.text("Displaying " + response.results.length + " result(s) out of " + response.noOfResults + " results found.");
                    $results.prepend(filterGuide);
                    $documents.fadeIn("slow", function(){
                        displayPager(response.start, response.noOfResults, true)
                    });
                } else {
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
                        $resultsInfo.text("Page  " + userSettDef["page"] + " — Total of " + cachedResponse.noOfResults + " open reading frames found.");

                        for (var i in cachedResponse.results) {
                            hit = parseOrfData(cachedResponse.results[i], i);
                            $documents.append(hit)
                        }
                        $sidePanel.empty();
                        displayFacets(cachedResponse.facetFields["COGID"], "COG");
                        displayFacets(cachedResponse.facetFields["KEGGID"], "KEGG");
                        displayPager(response.start, response.noOfResults, false);
                        $documents.fadeIn("slow");
                    }
                }
        });

        jqxhr.fail(function (data) {
            var message;
            message = data.responseText || data.statusText;
            console.log(message);
        });
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
                    if(field == "extended_desc" && p.text() != "N/A "){
                        var old = p.text();
                        p.text(old.slice(1,old.length-2));
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

                        userSettDef["page"] = 1;
                        currentFacetFilter = facet;
                        currentFacetField = fieldType;
                        var facetSearchParam = "&facetFilter=" + fieldType + "ID:" + facet;
                        var fetchDataURL = constructSearchURL(facetSearchParam);
                        $documents.fadeOut("slow", function(){
                            fetchData(fetchDataURL)
                        });
                    });
                })(facet, li);

                ul.append(li)
            }
            facetDiv.append(ul)
        }
    };

    var fetchClusters = function(url){
        var jqxhr = $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json"
        });

        jqxhr.done(function (data) {
            $('.clusterGraph').empty();
            var graphData = nodeify(data);
            renderNodeGraph(graphData);
        });
    }

    var nodeify = function(data){
        var graph = { "nodes": [], "links": []};//results
        var list = { "docs": [], "group": []}, //for intersection
            index = 0, //for nucleus
            total = 0, // nodes
            label = ""

        for (var i=0; i< data.noOfResults; i++){ //for each cluster
            label = Object.keys(data.clusters[i])[0]
            graph["nodes"].push( {"nucleus": label, "cluster": ["clust"+ i], "selected": false}); //push its nucleus
            // graph["nuclei"].push({"name":label, index: total});
            list["docs"].push(i);
            list["group"].push(index);
            var child = {"name": undefined, "cluster": []};

            for (var k=0; k< data.clusters[i][label].length; k++){ //for each individual document inside a cluster
                child = {"name": data.clusters[i][label][k], "cluster": ["clust"+i], "selected": false}; //create a node
                var intersection = list["docs"].indexOf(child.name); //is it found in any other cluster?
                list["docs"].push(child.name);
                list["group"].push(index);
                total++;

                if(intersection > -1){
                    graph["links"].push({"source": intersection, "target": list["group"][total]}); // create link from intersection
                    graph["nodes"][intersection]["cluster"].push("clust" + i);
                    graph["nodes"].push({"fixed": true, "x": -30}); //tombstone node quick fix
                    continue;
                }
                graph["nodes"].push(child); //add the node
                graph["links"].push({"source": index, "target": total}); //create links within each cluster

            }
            total++;
            index = total;
        }
        return  graph;
    }

    var renderNodeGraph = function(graphData){
        var self = this;
        var width = 800,
            height = 850,
            ctrlKey,
            grav,
            charge,
            rig;

        var links = graphData.links.length;
        grav = (links >= 700) ? 0.7: (links<= 50) ? 0.05 : links/1200;
        charge = (links >= 400) ? -350: (links <= 100) ? -150 : links*-0.5;
        rig = (links >= 1000) ? 0.95: (links <= 100) ? 0.4 : links/1000;

        var force = d3.layout.force()
            .size([width, height - 50])
            .charge(charge)     //sets the repulsion/attraction strength to the specified value. def=-30 myval = charge/4
            .chargeDistance(600)//sets the maximum distance over which charge forces are applied. def= inf myval= 750
            .theta(0.5)         //sets the Barnes–Hut approximation criterion to the specified value. def=0.8 myval = 0.5
            .linkDistance(20)    //sets the target distance between linked nodes to the specified value. def=20 myval=3
            .linkStrength(1)  //sets the strength (rigidity) of links to the specified value in the range [0,1]. def=1 myval=rig
            .gravity(grav)      //sets the gravitational strength to the specified numerical value. def=0.1 myval=grav
            // .friction(0.9)      //sets the friction coefficient, approximates velocity decay. def= 0.9
            .alpha(0.01)         //cooling parameter, if >0 restarts force layout, if <0 ends it. def=0.1
            .on("tick", tick);

        var svg = d3.select(".clusterGraph").append("svg")
            .attr("id", "cluster-svg")
            .attr("width", width)
            .attr("height", height);

        force
            .nodes(graphData.nodes)
            .links(graphData.links)
            .start();

        var link = svg.selectAll(".link").data(graphData.links)
            .enter().append("line")
            .attr("class", "link");


        var  node = svg.selectAll(".node")
            .data(graphData.nodes)
            .enter().append("g")
            .attr("class", "cluster")
            .attr("id", function(d){
                if (d.name) return d.name;
                if (d.cluster) return d.cluster;
                else return "tombstone"})
            .call(force.drag);

        node.append("circle")
            .attr("class", function(d){
                if (d.nucleus && d.cluster){
                    return "nucleus " + d.cluster[0]
                }
                if (d.cluster){
                    var l = d.cluster.length,
                        result = "node";
                    while(l > 0){
                        result+= " " + d.cluster[l-1];
                        l--;
                    }
                    return result;
                }
                else{
                    return "tombstone"
                }})
            .attr("id", function(d){
                if(d.name) return d.name
            })
            .attr("r", function(d){
                if (d.nucleus){
                    return 20
                }
                return 5
            })
            .style("opacity", function(d){
                if(d.nucleus){
                    return 1
                } return 0.5
            })
            .on("click", toggle);

        node.append("text")
            .attr("class","cluster-text")
            .attr("dx", function(d){
                if (d.nucleus){
                    return (-4 * (d.nucleus.length));
                }
            })
            .attr("dy", -20)
            .text(function(d){
                if (d.nucleus){
                    return d.nucleus + " (" + d.weight + ")"
                }
            })
            .on("mouseover", function(d){d3.select(this).style("fill", "#18364b").style("font-weight","bold")
            })
            .on("mouseout", function(d){d3.select(this).style("fill", "black").style("font-weight", "normal")})
            .on("click",function(d,i){
                toggle(node[0][i].__data__); //small hack to activate toggle on node when clicking text
            });

        // console.log(node);
        /**
         * D3 Method, handles the movement behaviour of every node at each tick (every fraction of a second)
         */
        function tick() {
            link.attr("x1", function(d) {
                    if (d.source.x < 8){ return 10;}
                    if (d.source.x > (width * 1.2)){ return (width * 1.2);}
                    return d.source.x;
                })
                .attr("y1", function(d) {
                    if (d.source.y < 8){ return 8;}
                    if (d.source.y > (height - 10)) {return (height - 10);}
                    return d.source.y;
                })
                .attr("x2", function(d) {
                    if (d.target.x < 8){ return 10;}
                    if (d.target.x > (width * 1.2)){ return (width * 1.2);}
                    return d.target.x;
                })
                .attr("y2", function(d) {
                    if (d.target.y < 8){ return 8;}
                    if (d.target.y > (height - 10)) {return (height - 10);}
                    return d.target.y;
                });

            node.attr("transform", function(d) {
                var d_x = d.x,
                    d_y = d.y;
                if (d.x < 20){ d_x = 20;}
                if (d.x > (width - 20)){ d_x = width - 20;}
                if (d.y < 30){ d_y = 30;}
                if (d.y > (height - 100)) {d_y = height - 100;}
                return "translate(" +  d_x + "," + d_y + ")";
            });
        }

        /**
         * D3 Method, handles a click to a node (both css effects, as well as initiating a query
         * @param d a single data point.
         */
        function toggle(d) {
            ctrlKey = d3.event.ctrlKey;
            if (ctrlKey){
                if (d.fixed){
                    for (var k = 0; k< d.cluster.length; k++){d3.selectAll("." + d.cluster[k]).classed( d.fixed = false);}
                }
                else{
                    for (var i = 0; i< d.cluster.length; i++){d3.selectAll("." + d.cluster[i]).classed( d.fixed = true);}}
            }
            else{
                $("circle").css("fill", "#50c1cc");
                var iDs = [];
                for (var l = 0; l < d.cluster.length; l++) {
                    $("." + d.cluster[l]).css("fill", "#18364b").each(function(){
                        var text =$(this).next().text(); //gets text associated
                        if (text.length > 0){
                            currentClusterFilter = text;
                        }
                        if(this.id)iDs.push(this.id);
                        // console.log(d);
                    });
                }
                // console.log(iDs)
                $documents.fadeOut("fast", function(){
                    var url = constructClusterFilterURL(iDs, "&clusterFilter=true");
                    console.log(url);
                    fetchData(url);
                });
            }
        }
        svg.selectAll("g#tombstone").remove()
    }

    var restoreResults = function(){
         $('circle').css("fill", "#50c1cc");
        $('.facet-selected').attr("class","facetList");
        $documents.fadeOut("fast", function(){
            $('.filterGuide').remove();
            var $resultsInfo = $('.resultsInfo');
            $documents.empty();
            $resultsInfo.empty();
            $resultsInfo.text("Page  " + userSettDef["page"] + " — Total of " + cachedResponse.noOfResults + " open reading frames found.");

            for (var i in cachedResponse.results) {
                var hit = parseOrfData(cachedResponse.results[i], i);
                $documents.append(hit)
            }
            $documents.fadeIn("slow", function(){
                displayPager(cachedResponse.start, cachedResponse.noOfResults, false)
            });
        });
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

        var filterOrEmpty
        if(isFilterPager == true){
            filterOrEmpty = "&facetFilter=" + currentFacetField + "ID:" + currentFacetFilter;
        }else{
            filterOrEmpty = ""
        }

        if(userSettDef["page"] == 1){
            backButton.attr("class", "disabled").css("cursor", "pointer");
        }else{
            backButton.click(function(){
                userSettDef["page"]--;
                var fetchDataURL = constructSearchURL(filterOrEmpty);
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
                var fetchDataURL = constructSearchURL(filterOrEmpty);
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
                        var fetchDataURL = constructSearchURL(filterOrEmpty);
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
                        var fetchDataURL = constructSearchURL(filterOrEmpty);
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

    var normalizeFacetSize = function(oldValue, oldMin, oldRange, newRange, newMin){
        var newValue = (((oldValue - oldMin) * newRange) / oldRange) + newMin;
        return "facet-size-" + Math.ceil(newValue)
    };



    var addIdLink = function(id){
        return "#"
    };

    $exportButton.on("click", function(){
        var baseUrl = $exportButton.data("url"),
            // query = $cache.match(/^.*?(?=&)/) + "", //extract query from the cached URL
            query = window.location.pathname.split("/")[2]; //TODO: remember its always the 3rd bucket, this could change in the future

        $(location).attr('href',baseUrl + query);
        return false;
    });

    var constructSearchURL = function(extraParam, isClusterSearch, ajax){
        if(highQualOnly === undefined){ highQualOnly = "false" }
        if(extraParam == undefined){ extraParam = ""} //any extra params such as facetSearch
        if(isClusterSearch == undefined){isClusterSearch = false} //do we need a double query for the cluster?
        if(ajax == undefined){ajax = true} //unless specified, do the search as an ajax (no reload)

        var query = $('#searchBox').val(),
            searchType = $('input:radio[name=df]:checked').val(),
            highQualOnly = $('input:checkbox[name=hq]:checked').val(),
            rpkm = $('#rpkm').val();

        if(isClusterSearch){
            var fetchDataURL =
                $('#clusterPanel').data("request") + query + "&searchField="+searchType + "&highQualOnly="+highQualOnly + "&minRPKM="+rpkm + extraParam;
        }else {
            fetchDataURL =
                (ajax ? $search.data('searchurl') : $search.data('submit')) + query + "&searchField="+searchType + "&highQualOnly="+highQualOnly + "&minRPKM="+rpkm + "&page=" + userSettDef["page"] + extraParam;
        }
        console.log($cache)
        console.log(fetchDataURL);
        return fetchDataURL
    };

    var constructClusterFilterURL = function(idList, extraParam){
        var idsString = idList.join(" OR "),
            fetchDataURL =
                $search.data('searchurl') + idsString + "&searchField=ORFID" + "&page=" + userSettDef["page"] + extraParam;
        return fetchDataURL;
    };

    $search.on("submit", function(){
        var fetchDataURL = constructSearchURL(undefined, undefined, false);
        alert(fetchDataURL)
        $(location).attr('href',fetchDataURL);
        return false;
    });

    $('#tabs a').click(function (e) {
        e.preventDefault();
        $(this).tab('show')

    })

    $('a[data-target="#facetPanel"]').click(function(e){
        $('.results').css("width",'74%');
        $('.side-bar').css("width",'23%');
    })

    $('a[data-target="#clusterPanel"]').click(function(e){
        $('.results').css("width",'52%');
        $('.side-bar').css('width', "46%");
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
    //init the search upon loading the page
    $('#searchBox').val($cache.match(/^.*?(?=&)/));
    fetchData($search.data('searchurl') + $cache);
    fetchClusters($('#clusterPanel').data("request") + $cache)
    $('[data-toggle="tooltip"]').tooltip();

});