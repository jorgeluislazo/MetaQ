package libs.solr.scala;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Created by jorgeluis on 20/02/17.
 */
public class PreEmptiveBasicAuthenticator implements HttpRequestInterceptor {
    private final UsernamePasswordCredentials credentials;

    public PreEmptiveBasicAuthenticator(String user, String pass) {
        credentials = new UsernamePasswordCredentials(user, pass);
    }

    @Override
    public void process(HttpRequest request, HttpContext context)
            throws HttpException, IOException {
        request.addHeader(BasicScheme.authenticate(credentials,"US-ASCII",false));
    }
}
