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

        // creates a TSV parser
        parser = new TsvParser(settings);
    }

    Quartet parseCogStats1(File file){
        List<String[]> allRows = parser.parseAll(file);
        return new Quartet<>(allRows.get(1)[1],allRows.get(2)[1], allRows.get(3)[1], allRows.get(4)[1]);
    }

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
        doc.addField("ORF_len", row[1]);
        doc.addField("start", row[2]);
        doc.addField("end", row[3]);
        doc.addField("strand_sense", row[6]);
        doc.addField("taxonomy", row[8]);
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
        extendeDesc.add(row[6]);
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

    private String modifyID(String ID){
        String[] list = ID.split("_");
        if (list.length >= 5){
            ID = list[0] + "_" + list[3] + "_" + list[4];
        }
        return ID;
    }

    private String prettyPrintRow(String[] row){
        StringBuilder result = new StringBuilder();
        for (String value : row){
            result.append(value).append("\t");
        }
        return result.toString();
    }


}
