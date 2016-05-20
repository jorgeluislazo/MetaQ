package reactor;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;


import java.io.IOException;
import java.util.Collection;

/**
 * Created by jorgeluis on 20/05/16.
 */
public class SolrLink {
    private static final String BASE_URL = "http://localhost:8983/solr/";
    private SolrClient solrClient;

    /**
     * Constructor method, returns a {@link SolrLink} with a specified
     * core to link to. Valid cores are: "ORFDocs" and "PathwayRuns"
     * See https://wiki.apache.org/solr/Solrj#Adding_Data_to_Solr
     *
     * @param coreName: the name of the Solr core to point to.
     */
    public SolrLink(String coreName){
        // http://localhost:8983/solr/#/pathwayRuns
        this.solrClient = new HttpSolrClient(BASE_URL + coreName);
    }

    /**
     * This method is called to check how many documents are in the current Solr Core
     */
    public long getCount() throws SolrServerException, IOException {
        SolrQuery q = new SolrQuery("*:*");
        q.setRows(0);  // don't actually request any data
        return solrClient.query(q).getResults().getNumFound();
    }


    /**
     * This method is called to index data to Solr.
     * See https://wiki.apache.org/solr/Solrj#Adding_Data_to_Solr
     *
     * @param documents: the {@link java.util.Collection} of {@link org.apache.solr.common.SolrInputDocument}
     *                   to index to Solr.
     */
    public void update(Collection<SolrInputDocument> documents) {
        try {
            solrClient.add(documents);
            solrClient.commit();
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
    }

}
