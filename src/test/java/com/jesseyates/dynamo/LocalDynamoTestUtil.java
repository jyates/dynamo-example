package com.jesseyates.dynamo;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.google.common.collect.Lists;
import com.jesseyates.dynamo.iter.PageManager;
import com.jesseyates.dynamo.iter.PagingIterator;
import com.jesseyates.dynamo.iter.TableNamePager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.ServerSocket;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

/**
 * Utility instance to help create and run a local dynamo instance
 */
public class LocalDynamoTestUtil {

  private static final Log LOG = LogFactory.getLog(LocalDynamoTestUtil.class);
  private AmazonDynamoDBClient dynamodb;
  private DynamoDBProxyServer server;

  private int port;
  private String url;
  private final AWSCredentialsProvider credentials;
  private String storeTableName = generateTableName();
  private Random random = new Random();
  private String testPrefix = generateTestPrefix();

  public LocalDynamoTestUtil(AWSCredentialsProvider credentials) {
    this.credentials = credentials;
  }

  public void start() throws Exception {
    // create a local database instance with an local server url on an open port
    ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    final String[] localArgs = {"-inMemory", "-port", String.valueOf(port)};
    server = ServerRunner.createServerFromCommandLineArgs(localArgs);
    server.start();
    url = "http://localhost:" + port;

    // internal client connection so we can easily stop, cleanup, etc. later
    this.dynamodb = getClient();
  }

  public void stop() throws Exception {
    server.stop();
    dynamodb.shutdown();
  }

  public void cleanupTables() {
    // cleanup anything with the ingest prefix. Ingest prefix is assumed to start after any other
    // table names, for the sake of this test utility, so we just get the last group of tables
    for (String name : new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return new PagingIterator<>(50, new PageManager<>(
          Lists.newArrayList(new TableNamePager("", getAsyncClient(), 50))));
      }
    }) {
      LOG.info("Deleting table: " + name);
      dynamodb.deleteTable(name);
    }
    // get the next table name
    this.storeTableName = generateTableName();
    this.testPrefix = generateTestPrefix();
  }

  private String generateTableName() {
    return "kinesis-avro-test-" + UUID.randomUUID().toString();
  }

  private String generateTestPrefix() {
    return "z-test-ingest-" + random.nextInt(500);
  }

  public String getCurrentTestTable() {
    return this.storeTableName;
  }

  public AmazonDynamoDBClient getClient() {
    return withProvider(new AmazonDynamoDBClient(credentials.getCredentials()));
  }

  public AmazonDynamoDBAsyncClient getAsyncClient() {
    return withProvider(new AmazonDynamoDBAsyncClient(credentials.getCredentials()));
  }

  private <T extends AmazonWebServiceClient> T withProvider(T client) {
    client.setEndpoint("http://localhost:" + port);
    return client;
  }
}
