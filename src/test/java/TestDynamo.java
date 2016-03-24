import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.ServerSocket;
import java.util.UUID;

/**
 *
 */
public class TestDynamo {

    private static final Log LOG = LogFactory.getLog(TestDynamo.class);
    private static String testTableName = "schema-repository-test-" + UUID.randomUUID().toString();

    private static AmazonDynamoDBClient dynamodb;
    private static DynamoDBProxyServer server;

    private static int port;

    @BeforeClass
    public static void setupDb() throws Exception {
        // create a local database instance with an local server url on an open port
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        final String[] localArgs = {"-inMemory", "-port", String.valueOf(port)};
        server = ServerRunner.createServerFromCommandLineArgs(localArgs);
        server.start();
        dynamodb = createClient();

        for (String table : dynamodb.listTables().getTableNames()) {
            System.out.println(table);
        }
    }

    private static AmazonDynamoDBClient createClient() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(getFakeProvider());
        client.setEndpoint("http://localhost:" + port);
        return client;
    }

    @After
    public void tearDownRepository() throws Exception {
        dynamodb.deleteTable(testTableName);
    }


    @AfterClass
    public static void shutdown() throws Exception {
        server.stop();
        dynamodb.shutdown();
    }

    private static AWSCredentialsProvider getFakeProvider() {
        return new StaticCredentialsProvider(
                new BasicAWSCredentials("AKIAIZFKPYAKBFDZPAEA", "18S1bF4bpjCKZP2KRgbqOn7xJLDmqmwSXqq5GAWq"));
    }


    @Test
    public void testReadWriteDynamo() throws Exception {

    }
}
