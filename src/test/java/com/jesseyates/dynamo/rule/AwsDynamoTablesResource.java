package com.jesseyates.dynamo.rule;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.jesseyates.dynamo.LocalDynamoTestUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.ExternalResource;

/**
 * Manage aws tables and getting a connection to them. Generally, this should be used at the
 * {@link org.junit.Rule} level.
 */
public class AwsDynamoTablesResource extends ExternalResource {

  private static final Log LOG = LogFactory.getLog(AwsDynamoTablesResource.class);

  private final AwsDynamoResource dynamoResource;
  private LocalDynamoTestUtil util;
  private boolean storeTableCreated;
  private AmazonDynamoDBAsyncClient client;

  public AwsDynamoTablesResource(AwsDynamoResource dynamo) {
    this.dynamoResource = dynamo;
  }

  @Override
  protected void after() {
    try {
      getUtil().cleanupTables();
    } catch (ResourceNotFoundException e) {
      LOG.error("\n----------\n Could not delete a table! " + (
        storeTableCreated ? "Marked" :
        "Not marked") + " that the store table was expected to be created. Change that "
                + "expectation with #setStoreTableCreated()\n---------");
      throw e;
    }

    // reset any open clients
    if (client != null) {
      client.shutdown();
      client = null;
    }
  }

  public void setStoreTableCreated(boolean created) {
    this.storeTableCreated = created;
  }

  public String getTestTableName() {
    return getUtil().getCurrentTestTable();
  }

  public AmazonDynamoDBAsyncClient getAsyncClient() {
    if (this.client == null) {
      this.client = getUtil().getAsyncClient();
    }
    return this.client;
  }

  private LocalDynamoTestUtil getUtil() {
    if (this.util == null) {
      this.util = dynamoResource.getUtil();
    }
    return this.util;
  }
}
