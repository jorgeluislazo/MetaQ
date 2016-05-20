package reactor;


import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by jorgeluis on 20/05/16.
 */
public class Main {

    private static SolrLink client;
    private static FileParser parser;
    public static Collection<SolrInputDocument> collection;

    public static void main(String[] args) throws IOException, SolrServerException {
        client = new SolrLink("pathwayRuns");
        parser = new FileParser();

        System.out.println("Num records in Core: " + client.getCount());

        parser.parse();
    }
}
