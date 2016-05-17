package reactor;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;


import java.io.IOException;


public class SolrLink {

    public static void main(String[] args) throws IOException, SolrServerException {
        System.out.println("requesting ping");
        System.out.println(getCount());
    }

    private static long getCount() throws SolrServerException, IOException {
        // http://localhost:8983/solr/#/pathwayRuns
        // http://localhost:8983/core0
        SolrClient server = new HttpSolrClient("http://localhost:8983/solr/pathwayRuns");
        SolrQuery q = new SolrQuery("*:*");
        q.setRows(0);  // don't actually request any data
        return server.query(q).getResults().getNumFound();
    }
}
