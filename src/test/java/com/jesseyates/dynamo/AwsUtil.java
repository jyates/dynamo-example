package com.jesseyates.dynamo;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;

public class AwsUtil {
  public static AWSCredentialsProvider getFakeProvider() {
      return new StaticCredentialsProvider(
              new BasicAWSCredentials("AKIAIZFKPYAKBFDZPAEA", "18S1bF4bpjCKZP2KRgbqOn7xJLDmqmwSXqq5GAWq"));

  }
}
