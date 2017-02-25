package reactor;


import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import com.google.common.io.Files;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Sextet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by jorgeluis on 20/05/16.
 */
public class Main {

    private static SolrLink client;
    private static FileParser parser;
    private static List<SolrInputDocument> orfDocs;
    private static List<SolrInputDocument> pwayDocs;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        if(args.length == 0){
            printUsage("No arguments found!");
            System.exit(0);
        }else{
            switch(args[0]){
                case "login":
                    if(args.length < 2){
                        printUsage("No [user] argument found!");
                        System.exit(0);
                    }else{
                        if(checkUser(args[1])){
                            System.out.println("Please provide your password: ");
                            String pass = scanner.next();

                            while(!login(args[1], pass)){
                                System.out.println("That password did not work.");
                                pass = scanner.next();
                            }
                            System.out.println("Welcome, " + args[1] + " ! ");
                            try {
                                userMethods(args[1], pass);
                            } catch (IOException | SolrServerException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;

                case "signup":
                    System.out.println("Create an account to index MetaPathways data into your own collection");
                    System.out.println("User name? (alphanumeric, dashes and underscores only, no spaces)");
                    //TODO check for white spaces
                    String newUser = scanner.next();
                    while(checkUser(newUser)){
                        System.out.println("Username '" + newUser + "' already exists! Choose another username");
                        newUser = scanner.next();
                    }
                    System.out.println("Choose a password for this account (alphanumeric, dashes and underscores only, no spaces)");
                    String newPass = scanner.next();
                    //TODO: create account
                    break;

                default:
                    printUsage("Command " + args[0] + " not recognized");
                    System.exit(0);
            }
        }
    }

    static private void importFile(File file, String username){
        String name = file.getName();

        switch (name){
            case "functional_and_taxonomic_table.txt":
                client.changeTargetCore("ORFDocs");
                orfDocs = parser.parseFuncTable(file, username);
                client.index(orfDocs);
                break;
            case "ORF_annotation_table.txt":
                orfDocs = parser.parseORFAnnotTable(file, username);
                client.index(orfDocs);
                break;
            default:
                if (name.matches(".+_combined_unique\\.orf_rpkm\\.txt")){
                    try {
                        orfDocs = parser.parseRPKMTable(file, username);
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
                        pwayDocs = parser.parsePwayTable(file, username);
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

    private static void userMethods(String userName, String password) throws IOException, SolrServerException {
        boolean ask = true;
        Scanner scanner = new Scanner(System.in);
        String command;
        while(ask){
            System.out.println("What would you like to do today? use one of the following commands: ");
            System.out.println("\t • index\n\t • status\n\t • delete\n\t • exit ");
            command = scanner.next();

            switch(command){
                case "index":
                    String path;
                    System.out.println("Please provide the absolute path directory to index metaPathways data sample folders");
                    path = scanner.next();

                    File baseDir = new File(path);
                    if(!baseDir.isDirectory()) {
                        System.out.println("\n\nERROR: Invalid directory path passed.");
                        break;
                    }

                    client = new SolrLink("ORFDocs", userName, password);
                    parser = new FileParser();

                    //for each SampleRun
                    System.out.println("Indexing files...\nWorking directory: " + path);
                    Long start = System.currentTimeMillis();
                    for( File metaGenomeRun : Files.fileTreeTraverser().children(baseDir)){
                        if (metaGenomeRun.isDirectory()) {
//                            String runID = metaGenomeRun.getName().substring(0, metaGenomeRun.getName().indexOf("_"));

                            orfDocs = new ArrayList<>();
                            for (File f : Files.fileTreeTraverser().preOrderTraversal(metaGenomeRun)) {
                                importFile(f, userName);
                            }
                            System.out.println("Successfuly indexed sample run: " + metaGenomeRun.getName() +
                                    ". With " + parser.oRFRows + " ORFs and " + parser.pwayRows + " pathways.");
                        }
                    }

                    System.out.println("Total time taken: " + (System.currentTimeMillis() - start)/1000 + " seconds");
                    break;

                case "status":
                    client = new SolrLink("ORFDocs", userName, password);
                    System.out.println("Number of ORFs indexed by " + userName + " : " + client.getCount());
                    client.changeTargetCore("PwayDocs");
                    System.out.println("Number of Pathways indexed by " + userName + ": " + client.getCount());
                    break;

                case "delete":
                    System.out.println("Please provide your password before proceeding");
                    client = new SolrLink("ORFDocs", userName, password);
                    client.deleteRecords();
                    break;

                case "exit":
                    ask = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Command not recognized");
                    break;
            }
        }
    }

    private static boolean login(String userName, String password){
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials defaultcreds = new UsernamePasswordCredentials(userName, password);
        provider.setCredentials(AuthScope.ANY, defaultcreds);

        HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();

        HttpGet request = new HttpGet("http://localhost:8983/solr/admin/authentication");
        HttpResponse response;

        try{
            response = client.execute(request);
//            System.out.println("Response Code : " + response.getStatusLine().getStatusCode());
            return response.getStatusLine().getStatusCode() == 200;
        }catch(Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean checkUser(String userName){
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("http://localhost:9000/checkUser/" + userName);
        HttpResponse response;
        try{
            response = client.execute(request);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//            System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

            return !rd.readLine().equals("user not found");
        }catch(Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    private static void printUsage(String extraMessage){
        System.out.println("-- -- -- Welcome to MetaQ Indexer! -- -- -- \n INIT COMMANDS:\n\t • login [user]\n\t • signup\n");
        if(extraMessage.length() > 0){
            System.out.println("Error: " + extraMessage);
        }
    }
}
