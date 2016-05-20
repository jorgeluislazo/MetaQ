package reactor;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by jorgeluis on 20/05/16.
 */
class FileParser {

    public void parse(){
        TsvParserSettings settings = new TsvParserSettings();
        //the file used here uses '\n' as the line separator sequence.
        //the line separator sequence is defined here to ensure systems such as MacOS and Windows
        //are able to process this file correctly (MacOS uses '\r'; and Windows uses '\r\n').
        settings.getFormat().setLineSeparator("\n");

        // creates a TSV parser
        TsvParser parser = new TsvParser(settings);

        // parses all rows in one go.
        List<String[]> allRows = parser.parseAll(new File("/home/jorgeluis/Documents/Hallam/AlyseData/SI4096441_results/annotation_table/COG_stats_1.txt"));

        for(String[] row : allRows){
            prettyPrint(row);
        }

    }

    private void prettyPrint(String[] row){
        StringBuilder output = new StringBuilder();
        for (int i = 0; i <row.length ; i++){
            output.append(row[i] + ',');
        }
        System.out.println(output);
    }

}
