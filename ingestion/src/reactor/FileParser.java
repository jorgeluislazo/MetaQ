package reactor;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.apache.solr.common.SolrInputDocument;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Sextet;


import java.io.File;
import java.util.*;

/**
 * Created by jorgeluis on 20/05/16.
 */
class FileParser {
    private TsvParser parser;

    FileParser(){
        TsvParserSettings settings = new TsvParserSettings();
        //the file used here uses '\n' as the line separator sequence.
        //the line separator sequence is defined here to ensure systems such as MacOS and Windows
        //are able to process this file correctly (MacOS uses '\r'; and Windows uses '\r\n').
        settings.getFormat().setLineSeparator("\n");
        settings.setMaxCharsPerColumn(200000); //how big can this list be...???!

        // creates a TSV parser
        parser = new TsvParser(settings);
    }

    //unused
    Quartet parseCogStats1(File file){
        List<String[]> allRows = parser.parseAll(file);
        return new Quartet<>(allRows.get(1)[1],allRows.get(2)[1], allRows.get(3)[1], allRows.get(4)[1]);
    }

    //unused
    List<Pair> parseCogStats2(File file){
        List<Pair> result = new ArrayList<>();
        List<String[]> allRows = parser.parseAll(file);

        for (int i=1; i<= 25; i++){
            String name = allRows.get(i)[0] + "_cog2";
            Pair<String, String> tuple = Pair.with(name , allRows.get(i)[2]);
            result.add(tuple);
        }
        return result;
    }

    //unused
    Sextet parseKeggStats1(File file){
        List<String[]> allRows = parser.parseAll(file);
        return new Sextet<>(
                allRows.get(1)[1],
                allRows.get(2)[1],
                allRows.get(3)[1],
                allRows.get(4)[1],
                allRows.get(5)[1],
                allRows.get(6)[1]
        );
    }

    //unused
    List<Pair> parseKeggStats2(File file){
        List<Pair> result = new ArrayList<>();
        List<String[]> allRows = parser.parseAll(file);

        for (int i=1; i<= 37; i++){
            String name = modifyName(allRows.get(i)[0]);
            Pair<String, String> tuple = Pair.with(name , allRows.get(i)[1]);
            result.add(tuple);
        }

        return result;
    }

    List<SolrInputDocument> parseFuncTable(File file){
        parser.beginParsing(file);
        String[] row;
        List<SolrInputDocument> documentBatch = new ArrayList<>();

        parser.parseNext(); //skip the titles...
        while((row = parser.parseNext()) != null){
            try {
                SolrInputDocument orfDoc = parseFunctTableRow(row);
                documentBatch.add(orfDoc);
            }catch(IllegalTableException e){
                e.printStackTrace();
                System.out.println("File originating error: " + file.getName());
                System.out.println("Path: " + file.getAbsoluteFile());
                System.out.println("row #: " + parser.getContext().currentLine());
            }

        }
        return documentBatch;
    }

    private SolrInputDocument parseFunctTableRow(String[] row) throws IllegalTableException{
        if(row[0] == null){
            //no id found
            throw new IllegalTableException("No id found in this row:\n'" + prettyPrintRow(row) + "'");
        }
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("ORFID", modifyID(row[0]));
        doc.addField("runID", getRunID(row[0]));
        doc.addField("ORF_len", row[1]);
        doc.addField("start", row[2]);
        doc.addField("end", row[3]);
        doc.addField("strand_sense", row[6]);
        String taxonomyName = row[8].replaceAll("unclassified ", "").replaceAll(" \\(miscellaneous\\)", "");
        String taxonomyID = taxonomyName;
        if(row[8].indexOf("(") > 0){
            taxonomyName =taxonomyName.substring(0, taxonomyName.indexOf("(") -1);
            taxonomyID = taxonomyID.substring(taxonomyID.indexOf("(") + 1, taxonomyID.length() - 1);
        }else{
            if (taxonomyName.equals("Monera")){
                taxonomyID = "2";
            }else{
                taxonomyID = "none";
            }
        }
        doc.addField("taxonomy", taxonomyName);
        doc.addField("taxonomyID", taxonomyID);
        doc.addField("product", row[9]);
        return doc;
    }


    List<SolrInputDocument> parseORFAnnotTable(File file){
        parser.beginParsing(file);
        String[] row;
        List<SolrInputDocument> documentBatch = new ArrayList<>();

        while((row = parser.parseNext()) != null){
            try {
                SolrInputDocument orfDoc = parseORFAnnotTableRow(row);
                documentBatch.add(orfDoc);
            }catch(IllegalTableException e){
                e.printStackTrace();
                System.out.println("File originating error: " + file.getName());
                System.out.println("Path: " + file.getAbsoluteFile());
                System.out.println("row #: " + parser.getContext().currentLine());
            }
        }
        return documentBatch;
    }

    private SolrInputDocument parseORFAnnotTableRow(String[] row) throws IllegalTableException{
        if(row[0].equals("")){
            //no id found
            throw new IllegalTableException("No id found in this row:\n'" + prettyPrintRow(row) + "'");

        }
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("ORFID",modifyID(row[0]));

        Map<String, String> cogID = new HashMap<>();
        cogID.put("set", row[2]);
        doc.addField("COGID", cogID);

        Map<String, String> keggID = new HashMap<>();
        keggID.put("set", row[3]);
        doc.addField("KEGGID", keggID);

        List<String> extendeDesc = new ArrayList<>();

        //adds this table product description to extendedDesc as well
        if(row[4] != null){
            extendeDesc.add(row[4]);
//            doc.addField("extended_desc", row[4]);
        }

//        Map<String, String> extended = new HashMap<>();
//        extended.put("set", row[6]);

        if(row[6] != null) {
            extendeDesc.add(row[6]);
        }
        doc.addField("extended_desc", extendeDesc);
        return doc;
    }

    List<SolrInputDocument> parseRPKMTable(File file) throws IllegalTableException{
        parser.beginParsing(file);
        String[] row;
        List<SolrInputDocument> documentBatch = new ArrayList<>();
        while((row = parser.parseNext()) != null){
            SolrInputDocument rpkmDoc = new SolrInputDocument();
            if(row[0].equals("")){
                throw new IllegalTableException("No id found in this row:\n'" + prettyPrintRow(row) + "'");
            }

            rpkmDoc.addField("ORFID", modifyID(row[0]));
            Map<String, String> rpkmValue = new HashMap<>();
            rpkmValue.put("set", row[1]);
            rpkmDoc.addField("rpkm", rpkmValue);
            documentBatch.add(rpkmDoc);
        }
        return documentBatch;
    }

    List<SolrInputDocument> parsePwayTable(File file) throws IllegalTableException{
        parser.beginParsing(file);
        String[] row;
        List<SolrInputDocument> documentBatch = new ArrayList<>();
        parser.parseNext(); //skip the titles...
        while ((row = parser.parseNext()) != null){
            SolrInputDocument pwayDoc = new SolrInputDocument();
            if(row[1].equals("")){
                throw new IllegalTableException("No pwayID found in this row:\n" + prettyPrintRow(row));
            }
            pwayDoc.addField("pway_id",row[1]);
            pwayDoc.addField("pway_name",row[2].replaceAll("^\"|\"$", ""));
            pwayDoc.addField("rxn_total", row[3]);
            Map<String, ArrayList<String>> orfs = new HashMap<>();
            orfs.put("add", convertToList(row[9]));
            pwayDoc.addField("orfs",  orfs);
            Map<String, String> sample_run = new HashMap<>();
            sample_run.put("add", modifyRun(row[0]));
            pwayDoc.addField("sample_runs", sample_run);

            documentBatch.add(pwayDoc);
        }

        return documentBatch;
    }

    private String getRunID(String idString){
        return idString.substring(0, idString.indexOf("_"));
    }


    private String modifyName(String title){
        StringBuilder result = new StringBuilder();

        int len = title.length();
        if(len < 25){ //parse the smaller titles first
            result = result.append(title.trim().replaceAll(" ", "_").toLowerCase()).append("_kegg2");
            return result.toString();
        }
        //specific cases
        if(len == 40){
            return "metabolism_terpenoids_kegg2";
        }
        if(len == 31){
            return "metabolism_other_kegg2";
        }
        if(len == 36){
            return "metabolism_cofactors_kegg2";
        }
        //others
        else{
            String[] words = title.split(" ");
            result = result.append(words[0].replaceAll(",","").toLowerCase()).append("_kegg2");
            return result.toString();
        }
    }

    private String modifyRun(String run){
        String[] list = run.split("_");
        if (list.length >= 3){
            run = list[0];
        }
        return run;
    }

    private String modifyID(String ID){
        String[] list = ID.split("_");
        if (list.length >= 5){
            ID = list[0] + "_" + list[3] + "_" + list[4];
        }
        return ID;
    }

    //helper funciton, converts [A,B,C] string to an array list.
    private ArrayList<String> convertToList(String listString){
        String[] arrayID = listString.substring(1, listString.length() -1).split(",");
        for(int i=0; i < arrayID.length; i++){
            arrayID[i] = modifyID(arrayID[i]);
        }
        List<String> resultList = new ArrayList<>(Arrays.asList(arrayID));
       return (ArrayList<String>) resultList;
    }

    private String prettyPrintRow(String[] row){
        StringBuilder result = new StringBuilder();
        for (String value : row){
            result.append(value).append("\t");
        }
        return result.toString();
    }


}
