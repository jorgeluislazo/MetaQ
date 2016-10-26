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
    private static List<SolrInputDocument> pwayDocs;

    public static void main(String[] args) throws IOException, SolrServerException {
        if(args[0].equals("del")){
            client = new SolrLink("ORFDocs");
            client.deleteRecords();
            return;
        }
        Long start = System.currentTimeMillis();

        boolean isDirectory = (new File(args[0]).isDirectory());
        if (!isDirectory){
            System.out.println("\n\nERROR: Invalid arguments passed.");
            System.out.println("\n\nUSAGE: \n\targ[0]: Absolute path to folder containing a metaPathwayRun.");
            throw new NoSuchFileException(printArgs(args));
        }

        //initializations
        File baseDir = new File(args[0]);
        client = new SolrLink("ORFDocs");
        parser = new FileParser();

        //for each ORFDoc
        System.out.println("Indexing files...\nWorking directory: " + args[0]);
        for( File metaGenomeRun : Files.fileTreeTraverser().children(baseDir)){
            if(!metaGenomeRun.isDirectory()){
                continue;
            }
            String runID = metaGenomeRun.getName().substring(0, metaGenomeRun.getName().indexOf("_"));
            SolrInputDocument ORFDoc = new SolrInputDocument();
            orfDocs = new ArrayList<>();

            for (File f : Files.fileTreeTraverser().preOrderTraversal(metaGenomeRun)){
                importFile(f, ORFDoc);
            }
//            ORFDoc.addField("runID", runID);
        }
        System.out.println("Total time taken in seconds: " + (System.currentTimeMillis() - start)/1000);
    }

    static private void importFile(File file, SolrInputDocument pathwayRunDoc){
        String name = file.getName();

        switch (name){
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
                if(name.matches(".+_combined_unique\\.pwy\\.txt")){
                    client.changeTargetCore("PwayDocs");
                    try {
                        pwayDocs = parser.parsePwayTable(file);
                    } catch (IllegalTableException e) {
                        e.printStackTrace();
                        System.out.println("File originating error: " + file.getName());
                        System.out.println("Path: " + file.getAbsoluteFile());
                    }
                    client.index(pwayDocs);
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
