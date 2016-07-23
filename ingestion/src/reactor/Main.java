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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jorgeluis on 20/05/16.
 */
public class Main {

    private static SolrLink client;
    private static FileParser parser;
    private static List<SolrInputDocument> orfDocs;

    public static void main(String[] args) throws IOException, SolrServerException {
        if(args[0].equals("del")){
            client = new SolrLink("ORFDocs");
            client.deleteRecords();
            return;
        }

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
        System.out.println("Indexing files...\nWorking directory: " + args[0]);
        Long start = System.currentTimeMillis();
        for( File pathwayRun : Files.fileTreeTraverser().children(baseDir)){
            if(!pathwayRun.isDirectory()){
                continue;
            }
            String runID = pathwayRun.getName().substring(0, pathwayRun.getName().indexOf("_"));
            SolrInputDocument pathwayRunDoc = new SolrInputDocument();
            orfDocs = new ArrayList<>();

            for (File f : Files.fileTreeTraverser().preOrderTraversal(pathwayRun)){
                importFile(f, pathwayRunDoc);
            }
            pathwayRunDoc.addField("runID", runID);
            client.indexSingle(pathwayRunDoc);
        }
        System.out.println("Total time taken in seconds: " + (System.currentTimeMillis() - start)/1000);
    }

    static private void importFile(File file, SolrInputDocument pathwayRunDoc){
        String name = file.getName();

        switch (name){
            case "COG_stats_1.txt":
                Quartet cogStats1 = parser.parseCogStats1(file);
                pathwayRunDoc.addField("poorly_char_cog1", cogStats1.getValue0());
                pathwayRunDoc.addField("info_process_cog1", cogStats1.getValue1());
                pathwayRunDoc.addField("metabolism_cog1", cogStats1.getValue2());
                pathwayRunDoc.addField("cell_signal_cog1", cogStats1.getValue3());
                break;
            case "COG_stats_2.txt":
                List<Pair> cogStats2 = parser.parseCogStats2(file);
                for (Pair tuple : cogStats2){
                    pathwayRunDoc.addField((String) tuple.getValue0(), tuple.getValue1());
                }
                break;
            case "KEGG_stats_1.txt":
                Sextet keggStats1 = parser.parseKeggStats1(file);
                pathwayRunDoc.addField("cell_process_kegg1", keggStats1.getValue0());
                pathwayRunDoc.addField("human_disease_kegg1", keggStats1.getValue1());
                pathwayRunDoc.addField("gene_info_kegg1", keggStats1.getValue2());
                pathwayRunDoc.addField("environmental_info_kegg1", keggStats1.getValue3());
                pathwayRunDoc.addField("organism_sys_kegg1", keggStats1.getValue4());
                pathwayRunDoc.addField("metabolism_kegg1", keggStats1.getValue5());
                break;
            case "KEGG_stats_2.txt":
                List<Pair> keggStats2 = parser.parseKeggStats2(file);
                for (Pair tuple : keggStats2){
                    pathwayRunDoc.addField((String) tuple.getValue0(), tuple.getValue1());
                }
                break;
            case "functional_and_taxonomic_table.txt":
                client.changeTargetCore("ORFDocs");
                orfDocs = parser.parseFuncTable(file);
                client.index(orfDocs);
                break;
            case "ORF_annotation_table.txt":
                orfDocs = parser.parseORFAnnotTable(file);
                client.index(orfDocs);
                break;
//            case "SI4096390_combined_unique.orf_rpkm.txt":
//                orfDocs = parser.parseRPKMTable(file);
//                client.index(orfDocs);
//                break;
            default:
                if (name.matches(".+_combined_unique\\.orf_rpkm\\.txt")){
                    try {
                        orfDocs = parser.parseRPKMTable(file);
                    } catch (IllegalTableException e) {
                        e.printStackTrace();
                        System.out.println("File originating error: " + file.getName());
                        System.out.println("Path: " + file.getAbsoluteFile());
                    }
                    client.index(orfDocs);

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
