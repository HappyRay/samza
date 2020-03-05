package org.apache.samza.storage.kv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.samza.SamzaException;
import org.apache.samza.metrics.Counter;
import org.apache.samza.metrics.Gauge;
import org.apache.samza.metrics.MetricsRegistry;
import org.apache.samza.metrics.MetricsRegistryMap;
import org.apache.samza.metrics.MetricsVisitor;
import org.apache.samza.metrics.SamzaHistogram;
import org.apache.samza.metrics.Timer;
import org.apache.samza.serializers.Serde;
import org.apache.samza.serializers.StringSerde;
import org.apache.samza.storage.kv.inmemory.InMemoryKeyValueStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestKeyValueSizeHistogramMetric {

  private static String storeName = "testStore";
  private static MetricsRegistryMap metricsRegistry = new MetricsRegistryMap();
  private static KeyValueStoreMetrics keyValueStoreMetrics = new KeyValueStoreMetrics(storeName, metricsRegistry);
  private static SerializedKeyValueStoreMetrics serializedKeyValueStoreMetrics =
      new SerializedKeyValueStoreMetrics(storeName, metricsRegistry);
  private static Serde<String> stringSerde = new StringSerde();
  private static Random random = new Random();

  private KeyValueStore<String, String> store = null;

  @Before
  public void setup() {
    KeyValueStore<byte[], byte[]> kvStore = new InMemoryKeyValueStore(keyValueStoreMetrics);
    KeyValueStore<String, String> serializedStore =
        new SerializedKeyValueStore<>(kvStore, stringSerde, stringSerde, serializedKeyValueStoreMetrics);
    store = new NullSafeKeyValueStore<>(serializedStore);
  }

  @Test
  public void testHistogramMetric() {

    List<String> keys = new ArrayList<>();
    List<String> values = new ArrayList<>();

    for (int i = 0; i < 1000; i++) {
      keys.add(getRandomString());
      values.add(getRandomString());
    }

    Collections.shuffle(keys);
    Collections.shuffle(values);

    for (int i = 0; i < keys.size(); i++) {
      store.put(keys.get(i), values.get(i));
    }

    metricsRegistry.getGroup("org.apache.samza.storage.kv.SerializedKeyValueStoreMetrics").forEach((name, metric) -> {
      if(name.contains("size-bytes-histogram")){
        System.out.println(name);
        metric.visit(new MetricsVisitor() {

          @Override
          public void counter(Counter counter) {

          }

          @Override
          public <T> void gauge(Gauge<T> gauge) {
              System.out.println(gauge.getValue());
              Assert.assertNotEquals(0.0D, (Double)gauge.getValue(), 0.0001);
          }

          @Override
          public void timer(Timer timer) {

          }
        });
      }
    });

  }

  private String getRandomString() {
    int leftLimit = 97; // letter 'a'
    int rightLimit = 122; // letter 'z'
    int maxLength = 1000;

    String generatedString = random.ints(leftLimit, rightLimit + 1)
        .limit(random.nextInt(maxLength))
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();

    return generatedString;
  }

  @After
  public void teardown() {
    try {
      store.close();
    } catch (SamzaException e) {
      e.printStackTrace();
    }
  }
}
