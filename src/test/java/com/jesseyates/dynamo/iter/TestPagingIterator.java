package com.jesseyates.dynamo.iter;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestPagingIterator {

  @Test
  public void singleWriteAndDone() throws Exception {
    List<String> supply = Lists.newArrayList("one");
    BatchedPager<String> func = queue -> {
      String s = supply.remove(0);
      queue.add(s);
      return true;
    };
    PagingIterator<String> iter = iterator(2, func);
    assertTrue(iter.hasNext());
    assertEquals("one", iter.next());
    assertFalse(iter.hasNext());
  }

  @Test
  public void testPrefetch() throws Exception {
    AtomicInteger counter = new AtomicInteger(0);
    List<List<String>> supply = Lists.newArrayList(Lists.newArrayList("one", "two"), Lists
      .newArrayList("three"));
    BatchedPager<String> func = queue -> {
      counter.incrementAndGet();
      if (supply.size() == 0) {
        return true;
      }
      List<String> s = supply.remove(0);
      queue.addAll(s);
      return false;
    };
    int prefetch = 1;
    PagingIterator<String> iter = iterator(prefetch, func);
    assertTrue(iter.hasNext());
    assertEquals("one", iter.next());
    assertEquals("Didn't prefetch next batch after reading fetch amout: " + prefetch, 2,
      counter.get());
    assertEquals("two", iter.next());
    assertEquals("three", iter.next());
    assertFalse(iter.hasNext());
  }

  @Test
  public void testPrefetchCounter() throws Exception {
    AtomicInteger counter = new AtomicInteger(0);
    List<String> supply = Lists.newArrayList("one", "two");
    BatchedPager<String> func = queue -> {
      if (counter.get() == 0) {
        queue.add(supply.remove(0));
      } else if (counter.get() == 1) {
        queue.addAll(supply);
      } else if (counter.get() == 2) {
        return true;
      }
      counter.incrementAndGet();
      return false;
    };
    int prefetch = 0;
    PagingIterator<String> iter = iterator(prefetch, func);
    assertTrue(iter.hasNext());
    // one for the initial fetch, one for the prefetch. Basically the same as prefetch =1
    assertFetch(prefetch, 2, counter);
    assertEquals("one", iter.next());
    assertFetch(prefetch, 2, counter);
    assertEquals("two", iter.next());
    assertFetch(prefetch, 2, counter);
  }

  /**
   * Test the prefetch check conditional
   */
  @Test
  public void testPrefetchWhenNotClosed() {
    AtomicInteger counter = new AtomicInteger(0);
    List<String> supply = Lists.newArrayList("one");
    BatchedPager<String> func = strings -> {
      counter.incrementAndGet();
      if (supply.size() > 0) {
        strings.add(supply.remove(0));
      } else {
        return true;
      }
      return false;
    };
    int prefetch = 0;
    PagingIterator<String> iter = iterator(prefetch, func);
    assertTrue(iter.hasNext());
    assertFetch(prefetch, 2, counter);
    assertEquals("one", iter.next());
    assertFetch(prefetch, 2, counter);
  }

  @Test
  public void testNoPrefetchWhenClosed() throws Exception {
    AtomicInteger counter = new AtomicInteger(0);
    List<String> supply = Lists.newArrayList("one", "two", "three");
    BatchedPager<String> func = strings -> {
      counter.incrementAndGet();
      if (counter.get() == 1) {
        strings.add(supply.remove(0));
      } else if (counter.get() == 2) {
        strings.addAll(supply);
      } else if (counter.get() == 3) {
        return true;
      }

      return false;
    };
    int prefetch = 0;
    PagingIterator<String> iter = iterator(prefetch, func);
    assertTrue(iter.hasNext());
    // this is a little bit of a weird count. We do it once for the initial load, but then also
    // load again when we check against the counter, essentially the 0th prefetch. However, this
    // makes more sense logically that prefectching before checking if we need to load, since
    // that gets into a corner case with when prefetch = 1 where we don't actually do a prefetch
    // because we have data in the queue
    assertFetch(prefetch, 2, counter);
    assertEquals("one", iter.next());
    assertFetch(prefetch, 2, counter);
    assertEquals("two", iter.next());
    assertFetch(prefetch, 3, counter);
    assertEquals("three", iter.next());
    assertFetch(prefetch, 3, counter);
  }

  private void assertFetch(int prefetch, int count, AtomicInteger counter) {
    assertEquals("Fetched wrong number of times, with prefetch: " + prefetch, count, counter.get());
  }

  @Test(expected = NoSuchElementException.class)
  public void testExceptionWhenNoMoreElements() throws Exception {
    BatchedPager<String> func = strings -> true;
    PagingIterator<String> iter = iterator(1, func);
    assertFalse(iter.hasNext());
    iter.next();
  }

  /**
   * If the page size is mis-configured, we will never go an prefetch data. However, because the
   * prefetch can be completely asynchronously, we have to provide a mechanism to notify the
   * pager that we are done with this batch. Internally, we then need to check and see if there
   * are outstanding requests and make a new one if needed.
   *
   * @throws Exception on failure
   */
  @Test(timeout = 10000l)
  public void testBatchSizeLessThanPageSize() throws Exception {
    AtomicInteger counter = new AtomicInteger(0);
    List<String> supply = Lists.newArrayList("one", "two");
    BatchedPager<String> func = strings -> {
      counter.incrementAndGet();
      if (counter.get() == 1) {
        strings.add(supply.remove(0));
        return false;
      } else if (counter.get() == 2) {
        strings.addAll(supply);
        return true;
      }

      throw new RuntimeException("Should not reach here!");
    };
    int prefetch = 3;
    PagingIterator<String> iter = iterator(prefetch, func);
    assertTrue(iter.hasNext());
    assertFetch(prefetch, 1, counter);
    assertEquals("one", iter.next());
    assertFetch(prefetch, 1, counter);
    assertEquals("two", iter.next());
    assertFetch(prefetch, 2, counter);
    assertFalse(iter.hasNext());
  }

  @Test
  public void testAsyncPager() throws Exception {
    long sleep = 1000;
    String message = "async-message";
    PagingRunner<String> runner = new BasePager<String>() {

      boolean first = true;

      @Override
      protected void page(Queue<String> queue) {
        if (!first) {
          complete();
          return;
        }
        first = false;
        Thread t = new Thread(() -> {
          try {
            Thread.currentThread().sleep(sleep);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }

          queue.add(message);
          batchComplete();
        });
        t.start();
      }
    };
    PageManager<String> manager = new PageManager<>(runner);
    Iterator<String> iter = new PagingIterator<>(1, manager);
    assertEquals(message, iter.next());
    assertFalse(iter.hasNext());
  }

  private <T> PagingIterator<T> iterator(int prefetch, BatchedPager<T> func) {
    PageManager<T> manager = new PageManager<>(Lists.newArrayList(new PageRunner<>(func)));
    return new PagingIterator<>(prefetch, manager);
  }

  private static class PageRunner<T> extends BasePager<T> {

    private final BatchedPager<T> pager;

    private PageRunner(BatchedPager<T> pager) {
      this.pager = pager;
    }

    @Override
    protected void page(Queue<T> queue) {
      if (pager.page(queue)) {
        complete();
      }
    }
  }

  @FunctionalInterface
  private interface BatchedPager<T> {
    boolean page(Queue<T> queue);
  }
}
