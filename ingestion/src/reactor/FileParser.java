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

    void parseFuncTable(File file, String username, String runID){
        parser.beginParsing(file);
        String[] row;

        parser.parseNext(); //skip the titles...
        while((row = parser.parseNext()) != null){
            try {
                parseFunctTableRow(row, username, runID);
            }catch(IllegalTableException e){
                e.printStackTrace();
                System.out.println("File originating error: " + file.getName());
                System.out.println("Path: " + file.getAbsoluteFile());
                System.out.println("row #: " + parser.getContext().currentLine());
            }
        }
    }

    private void parseFunctTableRow(String[] row, String username, String runID) throws IllegalTableException{
        if(row[0] == null){
            //no id found
            throw new IllegalTableException("No id found in this row:\n'" + prettyPrintRow(row) + "'");
        }
        SolrInputDocument doc;
        if(Main.orfDocs.containsKey(modifyID(runID, row[0]))){
            doc = Main.orfDocs.get(modifyID(runID, row[0]));
        }else{
            doc = new SolrInputDocument();
            doc.addField("ORFID", modifyID(runID, row[0]));
            doc.addField("owner", username);
        }

        doc.addField("runID", runID);
        doc.addField("ORF_len", row[1]);
        doc.addField("start", row[2]);
        doc.addField("end", row[3]);
        doc.addField("strand_sense", row[6]);
        doc.addField("rpkm", 3.000); //todo default
        String taxonomyName = row[8].replaceAll("unclassified ", "")
                .replaceAll(" \\(miscellaneous\\)", "")
                .replaceAll("sp\\.","sp");

        String taxonomyID = taxonomyName;
        if(row[8].indexOf("(") > 0){
            taxonomyName =taxonomyName.substring(0, taxonomyName.lastIndexOf("(") -1);

            //if taxonomy is given as expanded path with ; as delims
            String[] taxonomyNameDelims = taxonomyName.split(";");
            if(taxonomyNameDelims.length > 2){
                taxonomyName = taxonomyNameDelims[taxonomyNameDelims.length - 2];
                //grab the second last slot, this is the most accurate taxonomy
            }
            taxonomyID = taxonomyID.substring(taxonomyID.lastIndexOf("(") + 1, taxonomyID.length() - 1);
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

        Main.orfDocs.put(modifyID(runID, row[0]), doc);
    }


    void parseORFAnnotTable(File file, String username, String runID){
        parser.beginParsing(file);
        String[] row;

        while((row = parser.parseNext()) != null){
            try {
                parseORFAnnotTableRow(row, username, runID);
            }catch(IllegalTableException e){
                e.printStackTrace();
                System.out.println("File originating error: " + file.getName());
                System.out.println("Path: " + file.getAbsoluteFile());
                System.out.println("row #: " + parser.getContext().currentLine());
            }
        }
    }

    private void parseORFAnnotTableRow(String[] row, String username, String runID) throws IllegalTableException{
        if(row[0].equals("")){
            //no id found
            throw new IllegalTableException("No id found in this row:\n'" + prettyPrintRow(row) + "'");

        }
        SolrInputDocument doc;
        if(Main.orfDocs.containsKey(modifyID(runID, row[0]))){
            doc = Main.orfDocs.get(modifyID(runID, row[0]));
        }else{
            doc = new SolrInputDocument();
            doc.addField("ORFID", modifyID(runID, row[0]));
            doc.addField("owner", username);
        }

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
        Main.orfDocs.put(modifyID(runID, row[0]), doc);
    }

    void parseRPKMTable(File file, String username, String runID) throws IllegalTableException{
        parser.beginParsing(file);
        String[] row;
        while((row = parser.parseNext()) != null){
            if(row[0].equals("")){
                throw new IllegalTableException("No id found in this row:\n'" + prettyPrintRow(row) + "'");
            }
            SolrInputDocument doc;
            if(Main.orfDocs.containsKey(modifyID(runID, row[0]))){
                doc = Main.orfDocs.get(modifyID(runID, row[0]));
            }else{
                doc = new SolrInputDocument();
                doc.addField("ORFID", modifyID(runID, row[0]));
                doc.addField("owner", username);
            }

            Map<String, String> rpkmValue = new HashMap<>();
            rpkmValue.put("set", row[1]);
            doc.addField("rpkm", rpkmValue);
            Main.orfDocs.put(modifyID(runID, row[0]), doc);
        }
    }

    void parseMetaCycTable(File file, String username, String runID) throws IllegalTableException{
        parser.beginParsing(file);
        String[] row;

        while((row = parser.parseNext()) != null){
            int i = row.length - 1;
            String rxn = row[0];
            while(i >= 2){
                // ORFs were identified with this reaction
                //start from the last ORF, go backwards
                String orfID = row[i].substring(1, row[i].length() - 1);
                orfID = modifyID(runID, orfID);

                SolrInputDocument doc;
                if(Main.orfDocs.containsKey(orfID)){
                    doc = Main.orfDocs.get(orfID);
                }else{
                    doc = new SolrInputDocument();
                    doc.addField("ORFID", orfID);
                    doc.addField("owner", username);
                }

//                Map<String, String> rxnValue = new HashMap<>();
//                rxnValue.put("add", rxn);
                doc.addField("rxn", rxn);

                Main.orfDocs.put(modifyID(runID, row[0]), doc);
                i--;
            }
        }
    }

    void parsePwayTable(File file, String username, String runID) throws IllegalTableException{
        parser.beginParsing(file);
        String[] row;
        parser.parseNext(); //skip the titles...
        while ((row = parser.parseNext()) != null){
            if(row[1].equals("")){
                throw new IllegalTableException("No pwayID found in this row:\n" + prettyPrintRow(row));
            }

            SolrInputDocument pwayDoc;
            if(Main.pwayDocs.containsKey(row[1])){
                pwayDoc = Main.pwayDocs.get(row[1]);
            }else{
                pwayDoc = new SolrInputDocument();
                pwayDoc.addField("pway_id",row[1]);
                pwayDoc.addField("owner", username);
            }


            pwayDoc.addField("pway_name",row[2].replaceAll("^\"|\"$", ""));
            pwayDoc.addField("rxn_total", row[3]);
            // rxn covered
            pwayDoc.addField("rxn_covered", row[0] + ":" + row[4]);

            Map<String, ArrayList<String>> orfs = new HashMap<>();
            orfs.put("add", convertToList(runID, row[9]));
            pwayDoc.addField("orfs",  orfs);
            //sample runs
            Map<String, String> sample_run = new HashMap<>();
            sample_run.put("add", runID);
            pwayDoc.addField("sample_runs", sample_run);

            Main.pwayDocs.put(row[1],pwayDoc);
        }
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
        if(list[0].equals("O")){ //sometimes it starts with an O_23423_223
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

    //helper function, converts [A,B,C] string to an array list.
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
