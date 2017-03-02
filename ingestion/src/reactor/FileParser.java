package reactor;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.apache.solr.common.SolrInputDocument;


import java.io.File;
import java.util.*;

/**
 * Created by jorgeluis on 20/05/16.
 */
class FileParser {
    private TsvParser parser;
    public Long oRFRows;
    public Long pwayRows;

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

    List<SolrInputDocument> parseFuncTable(File file, String username, String runID){
        parser.beginParsing(file);
        String[] row;
        List<SolrInputDocument> documentBatch = new ArrayList<>();

        parser.parseNext(); //skip the titles...
        oRFRows = 0L; //reset count
        while((row = parser.parseNext()) != null){
            try {
                SolrInputDocument orfDoc = parseFunctTableRow(row, username, runID);
                documentBatch.add(orfDoc);
                oRFRows++;
            }catch(IllegalTableException e){
                e.printStackTrace();
                System.out.println("File originating error: " + file.getName());
                System.out.println("Path: " + file.getAbsoluteFile());
                System.out.println("row #: " + parser.getContext().currentLine());
            }
        }
        return documentBatch;
    }

    private SolrInputDocument parseFunctTableRow(String[] row, String username, String runID) throws IllegalTableException{
        if(row[0] == null){
            //no id found
            throw new IllegalTableException("No id found in this row:\n'" + prettyPrintRow(row) + "'");
        }
        SolrInputDocument doc = new SolrInputDocument();

        Map<String, String> owner = new HashMap<>();
        owner.put("add", username);
        doc.addField("owner", owner);

        doc.addField("ORFID", modifyID(runID, row[0]));
        doc.addField("runID", runID);
        doc.addField("ORF_len", row[1]);
        doc.addField("start", row[2]);
        doc.addField("end", row[3]);
        doc.addField("strand_sense", row[6]);
        doc.addField("rpkm", 3.02012); //todo default
        String taxonomyName = row[8].replaceAll("unclassified ", "")
                .replaceAll(" \\(miscellaneous\\)", "")
                .replaceAll("sp\\.","sp");

        String taxonomyID = taxonomyName;
        if(row[8].indexOf("(") > 0){
            taxonomyName =taxonomyName.substring(0, taxonomyName.indexOf("(") -1);

            //if taxonomy is given as expanded path with ; as delims
            String[] taxonomyNameDelims = taxonomyName.split(";");
            if(taxonomyNameDelims.length > 2){
                taxonomyName = taxonomyNameDelims[taxonomyNameDelims.length - 2];
                //grab the second last slot, this is the most accurate taxonomy
            }

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


    List<SolrInputDocument> parseORFAnnotTable(File file, String username, String runID){
        parser.beginParsing(file);
        String[] row;
        List<SolrInputDocument> documentBatch = new ArrayList<>();

        while((row = parser.parseNext()) != null){
            try {
                SolrInputDocument orfDoc = parseORFAnnotTableRow(row, username, runID);
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

    private SolrInputDocument parseORFAnnotTableRow(String[] row, String username, String runID) throws IllegalTableException{
        if(row[0].equals("")){
            //no id found
            throw new IllegalTableException("No id found in this row:\n'" + prettyPrintRow(row) + "'");

        }
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("ORFID",modifyID(runID, row[0]));

        Map<String, String> owner = new HashMap<>();
        owner.put("add", username);
        doc.addField("owner", owner);

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
        }

        if(row[6] != null) {
            extendeDesc.add(row[6]);
        }
        doc.addField("extended_desc", extendeDesc);
        return doc;
    }

    List<SolrInputDocument> parseRPKMTable(File file, String username, String runID) throws IllegalTableException{
        parser.beginParsing(file);
        String[] row;
        List<SolrInputDocument> documentBatch = new ArrayList<>();
        while((row = parser.parseNext()) != null){
            SolrInputDocument rpkmDoc = new SolrInputDocument();
            if(row[0].equals("")){
                throw new IllegalTableException("No id found in this row:\n'" + prettyPrintRow(row) + "'");
            }

            Map<String, String> owner = new HashMap<>();
            owner.put("add", username);
            rpkmDoc.addField("owner", owner);

            rpkmDoc.addField("ORFID", modifyID(runID, row[0]));
            Map<String, String> rpkmValue = new HashMap<>();
            rpkmValue.put("set", row[1]);
            rpkmDoc.addField("rpkm", rpkmValue);
            documentBatch.add(rpkmDoc);
        }
        return documentBatch;
    }

    List<SolrInputDocument> parsePwayTable(File file, String username, String runID) throws IllegalTableException{
        parser.beginParsing(file);
        String[] row;
        List<SolrInputDocument> documentBatch = new ArrayList<>();
        parser.parseNext(); //skip the titles...
        pwayRows = 0L; //reset count
        while ((row = parser.parseNext()) != null){
            SolrInputDocument pwayDoc = new SolrInputDocument();
            if(row[1].equals("")){
                throw new IllegalTableException("No pwayID found in this row:\n" + prettyPrintRow(row));
            }

            Map<String, String> owner = new HashMap<>();
            owner.put("add", username);
            pwayDoc.addField("owner", owner);

            pwayDoc.addField("pway_id",row[1]);
            pwayDoc.addField("pway_name",row[2].replaceAll("^\"|\"$", ""));
            pwayDoc.addField("rxn_total", row[3]);
            // rxn covered
            Map<String,String> rxn_covered = new HashMap<>();
            rxn_covered.put("add", row[0] + ":" + row[4]);
            pwayDoc.addField("rxn_covered", rxn_covered);
            Map<String, ArrayList<String>> orfs = new HashMap<>();
            orfs.put("add", convertToList(runID, row[9]));
            pwayDoc.addField("orfs",  orfs);
            //sample runs
            Map<String, String> sample_run = new HashMap<>();
            sample_run.put("add", runID);
            pwayDoc.addField("sample_runs", sample_run);

            documentBatch.add(pwayDoc);
            pwayRows++;
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

    private String modifyID(String runID, String ID){
        String[] list = ID.split("_");
        if (list.length >= 5){
            ID = list[0] + "_" + list[3] + "_" + list[4];
        }
        if(list[0].equals("O")){
            return runID + "_" + list[1] + "_" + list[2];
        }else{
            return runID + "_" + ID;
        }
    }

    private String modifyID(String ID){
        String[] list = ID.split("_");
        if (list.length >= 5){
            ID = list[0] + "_" + list[3] + "_" + list[4];
        }
        return ID;
    }

    //helper funciton, converts [A,B,C] string to an array list.
    private ArrayList<String> convertToList(String runID, String listString){
        String[] arrayID = listString.substring(1, listString.length() -1).split(",");
        for(int i=0; i < arrayID.length; i++){
            arrayID[i] = modifyID(runID, arrayID[i]);
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
