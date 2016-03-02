package com.homeadvisor.kafdrop.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConsumerPartitionVOTest
{
   private void doLagTest(long first, long last, long offset, long expectedLag)
   {
      final ConsumerPartitionVO partition = new ConsumerPartitionVO("test", "test", 0);
      partition.setFirstOffset(first);
      partition.setSize(last);
      partition.setOffset(offset);
      assertEquals("Unexpected lag", expectedLag, partition.getLag());
   }
   @Test
   public void testGetLag() throws Exception
   {
      doLagTest(0, 0, 0, 0);
      doLagTest(-1, -1, -1, 0);
      doLagTest(5, 10, 8, 2);
      doLagTest(5, 10, 2, 5);
      doLagTest(6, 6, 2, 0);
      doLagTest(5, 10, -1, 5);
   }
}