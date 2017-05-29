package info.androidhive.firebase;

import android.content.Context;
import android.content.res.AssetManager;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryScopes;
import com.google.api.services.bigquery.model.GetQueryResultsResponse;
import com.google.api.services.bigquery.model.QueryRequest;
import com.google.api.services.bigquery.model.QueryResponse;
import com.google.api.services.bigquery.model.TableCell;
import com.google.api.services.bigquery.model.TableRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class BigQueryConnector {
    private AssetManager am;
    private String query;
    private Context context;

    BigQueryConnector(AssetManager am, String query, Context context){
        this.am = am;
        this.query = query;
        this.context = context;
    }

    private File createFileFromInputStream(InputStream inputStream) {

        try{
            File f = new File(context.getFilesDir(), "UkrBikeApp-ff55878cb577.p12");
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        }catch (IOException e) {
            //Logging exception
        }

        return null;
    }

    public Bigquery createAuthorizedClient() throws IOException {
        // Create the credential
        //String SCOPE = "https://www.googleapis.com/auth/bigquery";
        //final HttpTransport transport = new NetHttpTransport();
        //final JsonFactory jsonFactory = new JacksonFactory();
        final List SCOPE = Arrays.asList("https://www.googleapis.com/auth/bigquery");
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        GoogleCredential credential = new GoogleCredential();
        try{
            InputStream inputStream = am.open("UkrBikeApp-ff55878cb577.p12");
            File file = createFileFromInputStream(inputStream);
            //File file = new File("UkrBikeApp-ff55878cb577.p12");
            credential = new GoogleCredential.Builder().setTransport(transport)
                    .setJsonFactory(jsonFactory)
                    .setServiceAccountId("bigquerymain@ukrbikeapp.iam.gserviceaccount.com")
                    .setServiceAccountScopes(SCOPE)
                    .setServiceAccountPrivateKeyFromP12File(file)
                    .build();
        }
        catch (Exception e){
            System.out.println("e = " + e);
        }


        if (credential.createScopedRequired()) {
            credential = credential.createScoped(BigqueryScopes.all());
        }

        return new Bigquery.Builder(transport, jsonFactory, credential)
                .setApplicationName("Bigquery Samples")
                .build();
    }

    private static List<TableRow> executeQuery(String querySql, Bigquery bigquery, String projectId)
            throws IOException {
        QueryResponse query =
                bigquery.jobs().query(projectId, new QueryRequest().setQuery(querySql).setUseLegacySql(false)).execute();

        // Execute it
        GetQueryResultsResponse queryResult =
                bigquery
                        .jobs()
                        .getQueryResults(
                                query.getJobReference().getProjectId(), query.getJobReference().getJobId())
                        .execute();

        return queryResult.getRows();
    }


    private static void displayResults(List<TableRow> rows) {
        System.out.print("\nResults:\n------------\n");
        if(rows.size() != 0){
            for (TableRow row : rows) {
                for (TableCell field : row.getF()) {
                    System.out.printf("%-50s", field.getV());
                }
                System.out.println();
            }
        }

    }

    public List<TableRow> start_bigquery() throws IOException {

        String projectId = "ukrbikeapp";

        // Create a new Bigquery client authorized via Application Default Credentials.
        Bigquery bigquery = createAuthorizedClient();

        List<TableRow> rows =
                executeQuery(
                        query,
                        bigquery,
                        projectId);
//        executeQuery(
//                "SELECT COUNT(DISTINCT t0.user_dim.app_info.app_instance_id, 50000) AS t0.unique_users, \n" +
//                        "t0.user_dim.geo_info.city FROM (SELECT * FROM TABLE_DATE_RANGE([ukrbikeapp:info_androidhive_firebase_ANDROID.app_events_], \n" +
//                        "TIMESTAMP('20170320'), TIMESTAMP('20170622'))) AS t0 GROUP EACH BY t0.user_dim.geo_info.city ORDER BY t0.unique_users DESC",
//                bigquery,
//                projectId);

        //displayResults(rows);
        return rows;
    }
}
