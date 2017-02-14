## What is MetaQ? ##
  MetaQ is a web-based search platform built for [MetaPathways](http://hallam.microbiology.ubc.ca/MetaPathways/) data. It integrates a backend search engine server with indexed annoted metagenomic data and a frontend web visualizing module for both open reading frame annotations as well as microbiological pathway annotations.
  
## Quick start guide ##

#### ORF search module ####
This module focuses on open reading frames (ORFs), so one can search functionally annotated genes through a protein product name. each ORF will have other stats tagged with it, such as ORF length, taxonomic origin, RPKM and  functional annotation IDs from [MetaCyc](http://www.metacyc.org/), [KEGG](http://www.genome.jp/kegg/) and [COG](http://www.ncbi.nlm.nih.gov/COG/) protein databases (if recognized).

Searches allow filters like a minimum rpkm allowed or for results to require at least one protein functionally annotation id (from COG or KEGG) to refine the searches. In addition, this module also includes a clustering visualization which groups up results based on common keywords between them for explorative purposes (clusters tab). Finally, there is also a phylogenetic tree constructed (taxonomy tab) from the search results based on the taxonomies found.

#### Pathway search module ####
On the other hand, this module focuses on searching through pathways identified by MetaPathways. Every pathway identified will have a pathway name and id annotated against the MetaCyc database. Additonally, all the associated ORFs found in this pathway can be further examined and what sample runs they belong to. Future implementation will include exploring how many reactions are covered for a specific pathway per sample run identified in the entry.

Current functionality also supports exporting the results found and downloading them as a TSV file with each column attribute for further use.

## Demo ##
  https://pathway-query.herokuapp.com/
