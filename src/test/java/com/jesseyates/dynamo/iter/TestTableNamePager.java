package com.jesseyates.dynamo.iter;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.google.common.collect.Lists;
import com.jesseyates.dynamo.rule.AwsDynamoResource;
import com.jesseyates.dynamo.rule.AwsDynamoTablesResource;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.jesseyates.dynamo.AwsUtil.getFakeProvider;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class TestTableNamePager {

  @ClassRule
  public static AwsDynamoResource dynamoResource = new AwsDynamoResource(getFakeProvider());
  @Rule
  public AwsDynamoTablesResource tableResource = new AwsDynamoTablesResource(dynamoResource);

  private final String primaryKey = "pk";

  @Test
  public void testNoTables() throws Exception {
    assertEquals(Lists.newArrayList(),
      getTables(tableResource.getAsyncClient(), null, 1).collect(toList()));
  }

  @Test
  public void testReadOneTable() throws Exception {
    String name = tableResource.getTestTableName();
    createStringKeyTable();
    assertEquals(Lists.newArrayList(name),
      getTables(tableResource.getAsyncClient(), null, 1).collect(toList()));
  }

  @Test
  public void testReadPrefix() throws Exception {
    createStringKeyTable("aname");
    String name = "bname";
    createStringKeyTable(name);

    assertEquals(Lists.newArrayList(name),
      getTables(tableResource.getAsyncClient(), "b", 1).collect(toList()));
  }

  private Table createStringKeyTable(String tableName) {
    CreateTableRequest create =
      new CreateTableRequest().withTableName(tableName)
                              .withKeySchema(new KeySchemaElement(primaryKey, KeyType.HASH))
                              .withAttributeDefinitions(
                                new AttributeDefinition(primaryKey, ScalarAttributeType.S))
                              .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
    AmazonDynamoDBAsyncClient client = tableResource.getAsyncClient();
    DynamoDB dynamo = new DynamoDB(client);
    return dynamo.createTable(create);
  }

  private Table createStringKeyTable() {
    return createStringKeyTable(tableResource.getTestTableName());
  }

  public static Stream<String> getTables(AmazonDynamoDBAsyncClient dynamo, String prefix, int
    pageSize) {
    PagingRunner<String> runner = new TableNamePager(prefix, dynamo, pageSize);
    return StreamSupport.stream(new PagingIterator<>(pageSize, new PageManager<>(
      Lists.newArrayList(runner))).iterable().spliterator(), false);
  }
}
