package reactor;


import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import com.google.common.io.Files;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Sextet;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.List;

/**
 * Created by jorgeluis on 20/05/16.
 */
public class Main {

    private static SolrLink client;
    private static FileParser parser;
    public static Collection<SolrInputDocument> collection;

    public static void main(String[] args) throws IOException, SolrServerException {
        boolean isDirectory = (new File(args[0]).isDirectory());
        if (!isDirectory){
            System.out.println("\n\nERROR: Invalid arguments passed.");
            System.out.println("\n\nUSAGE: \n\targ[0]: Absolute path to folder containing a metaPathwayRun.");
            throw new NoSuchFileException(printArgs(args));
        }

        //initializations
        File baseDir = new File(args[0]);
        client = new SolrLink("pathwayRuns");
        parser = new FileParser();
        //for each pathway run

        for( File pathwayRun : Files.fileTreeTraverser().children(baseDir)){

            SolrInputDocument document = new SolrInputDocument();
            for (File f : Files.fileTreeTraverser().preOrderTraversal(pathwayRun)){
                importFile(f, document);
            }
        }
        //parser.parse();
    }

    static private void importFile(File file, SolrInputDocument document){
        String name = file.getName();
//        System.out.println(file.getName());

        switch (name){
            case "COG_stats_1.txt":
                Quartet cogStats1 = parser.getCogStats1(file);
                document.addField("poorly_char_cog1", cogStats1.getValue0());
                document.addField("info_process_cog1", cogStats1.getValue1());
                document.addField("metabolism_cog1", cogStats1.getValue2());
                document.addField("cell_signal_cog1", cogStats1.getValue3());
                break;
            case "COG_stats_2.txt":
                List<Pair> cogStats2 = parser.getCogStats2(file);
                for (Pair tuple : cogStats2){
                    document.addField((String) tuple.getValue0(), tuple.getValue1());
                }
                break;
            case "KEGG_stats_1.txt":
                Sextet keggStats1 = parser.getKeggStats1(file);
                document.addField("cell_process_kegg1", keggStats1.getValue0());
                document.addField("human_disease_kegg1", keggStats1.getValue1());
                document.addField("gene_info_kegg1", keggStats1.getValue2());
                document.addField("environmental_info_kegg1", keggStats1.getValue3());
                document.addField("organism_sys_kegg1", keggStats1.getValue4());
                document.addField("metabolism_kegg1", keggStats1.getValue5());
                break;
            case "KEGG_stats_2.txt":
                List<Pair> keggStats2 = parser.getKeggStats2(file);
                for (Pair tuple : keggStats2){
                    document.addField((String) tuple.getValue0(), tuple.getValue1());
                }
                break;
        }

    }

    /**
     * Method to pretty print arguments for debugging.
     */
    private static String printArgs(String[] arguments){
        String result = "";
        for (String arg: arguments){
            result += arg + "\n";
        }
        if (result.equals("")){
            return "NO ARGUMENTS SUPPLIED";
        }
        return result;
    }
}
