package com.jesseyates.dynamo.iter;


@FunctionalInterface
public interface VoidCallWithArg<T> {

  void call(T arg);
}
