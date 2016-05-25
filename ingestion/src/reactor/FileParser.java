package reactor;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Sextet;


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by jorgeluis on 20/05/16.
 */
class FileParser {
//    private TsvParserSettings settings;
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

    public void parse(){
        TsvParserSettings settings = new TsvParserSettings();
        //the file used here uses '\n' as the line separator sequence.
        //the line separator sequence is defined here to ensure systems such as MacOS and Windows
        //are able to process this file correctly (MacOS uses '\r'; and Windows uses '\r\n').
        settings.getFormat().setLineSeparator("\n");

        // creates a TSV parser
        parser = new TsvParser(settings);

        // parses all rows in one go.
        List<String[]> allRows = parser.parseAll(new File("/home/jorgeluis/Documents/Hallam/AlyseData/SI4096441_results/annotation_table/COG_stats_1.txt"));

        for(String[] row : allRows){
            prettyPrint(row);
        }

    }

    private void prettyPrint(String[] row){
        StringBuilder output = new StringBuilder();
        for (int i = 0; i <row.length ; i++){
            output.append(row[i]);
        }
        System.out.println(output);
    }

     Quartet getCogStats1(File file){
        List<String[]> allRows = parser.parseAll(file);
        return new Quartet<>(allRows.get(1)[1],allRows.get(2)[1], allRows.get(3)[1], allRows.get(4)[1]);
    }

    List<Pair> getCogStats2(File file){
        List<Pair> result = new ArrayList<>();
        List<String[]> allRows = parser.parseAll(file);

        for (int i=1; i<= 25; i++){
            String name = allRows.get(i)[0] + "_cog2";
            Pair<String, String> tuple = Pair.with(name , allRows.get(i)[2]);
            result.add(tuple);
        }
        return result;
    }

    Sextet getKeggStats1(File file){
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

    List<Pair> getKeggStats2(File file){
        List<Pair> result = new ArrayList<>();
        List<String[]> allRows = parser.parseAll(file);

        for (int i=1; i<= 37; i++){
            String name = modifyName(allRows.get(i)[0]);
            Pair<String, String> tuple = Pair.with(name , allRows.get(i)[1]);
            result.add(tuple);
        }

        return result;
    }

    private static String modifyName(String title){
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


}
