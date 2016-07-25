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
    // http://localhost:8983/solr/
    // http://ec2-54-153-99-252.us-west-1.compute.amazonaws.com:8983/solr/
    private static final String BASE_URL = "http://ec2-54-153-99-252.us-west-1.compute.amazonaws.com:8983/solr/";
    private SolrClient solrClient;

    /**
     * Constructor method, returns a {@link SolrLink} with a specified
     * core to link to. Valid cores are: "ORFDocs" and "PathwayRuns"
     * See https://wiki.apache.org/solr/Solrj#Adding_Data_to_Solr
     *
     * @param coreName: the name of the Solr core to point to.
     */
    public SolrLink(String coreName){
        // http://ec2-54-153-99-252.us-west-1.compute.amazonaws.com:8983/solr/#/ORFDocs
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
    public void index(Collection<SolrInputDocument> documents) {
        try {
            solrClient.add(documents);
            solrClient.commit();
            System.out.println("succesfully indexed/updated a batch size of :" + documents.size());
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * This method is called to index data to Solr.
     * See https://wiki.apache.org/solr/Solrj#Adding_Data_to_Solr
     *
     * @param document: the {@link org.apache.solr.common.SolrInputDocument}
     *                   to index to Solr.
     */
    public void indexSingle(SolrInputDocument document) {
        try {
            changeTargetCore("pathwayRuns");
            solrClient.add(document);
            solrClient.commit();
            System.out.println("Succesfully added PathwayRun: " + document.getFieldValue("runID"));
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to delete all indexed records in Solr.
     *
     * @throws IOException
     * @throws SolrServerException
     */
    public void deleteRecords() throws IOException, SolrServerException {
        solrClient.deleteByQuery("*:*");
        solrClient.commit();
        System.out.println("Deleting all records...Done");
    }

    public void changeTargetCore(String newTarget){
        // http://localhost:8983/solr/#/pathwayRuns
        this.solrClient = new HttpSolrClient(BASE_URL + newTarget);
    }

}
