/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.pig.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.backend.hadoop.datastorage.ConfigurationUtil;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestHBaseStorage {

	private static final Log LOG = LogFactory.getLog(TestHBaseStorage.class);
	private static HBaseTestingUtility util;
    private static Configuration conf;

	private static PigServer pig;

	final static int NUM_REGIONSERVERS = 1;

	enum DataFormat {
		HBaseBinary, UTF8PlainText,
	}

	// Test Table constants
	private static final String TESTTABLE_1 = "pigtable_1";
	private static final String TESTTABLE_2 = "pigtable_2";
	private static final byte[] COLUMNFAMILY = Bytes.toBytes("pig");
	private static final String TESTCOLUMN_A = "pig:col_a";
	private static final String TESTCOLUMN_B = "pig:col_b";
	private static final String TESTCOLUMN_C = "pig:col_c";
	private static final int TEST_ROW_COUNT = 100;

	@BeforeClass
	public static void setUp() throws Exception {
        // This is needed by Pig
        MiniCluster cluster = MiniCluster.buildCluster();

        conf = cluster.getConfiguration();
        conf.setInt("mapred.map.max.attempts", 1);

        util = new HBaseTestingUtility(conf);
        util.startMiniZKCluster();
        util.startMiniHBaseCluster(1, 1);


        pig = new PigServer(ExecType.LOCAL,
				ConfigurationUtil.toProperties(conf));
	}

	@AfterClass
	public static void oneTimeTearDown() throws Exception {
        // In HBase 0.90.1 and above we can use util.shutdownMiniHBaseCluster()
        // here instead.
        MiniHBaseCluster hbc = util.getHBaseCluster();
        if (hbc != null) {
          hbc.shutdown();
          hbc.join();
        }
        util.shutdownMiniZKCluster();
    }

	@After
	public void tearDown() throws Exception {
        try {
            deleteAllRows(TESTTABLE_1);
        } catch (IOException e) {}
        try {
            deleteAllRows(TESTTABLE_2);
        } catch (IOException e) {}
		pig.shutdown();
	}

    // DVR: I've found that it is faster to delete all rows in small tables
    // than to drop them.
    private void deleteAllRows(String tableName) throws Exception {
        HTable table = new HTable(conf, tableName);
        ResultScanner scanner = table.getScanner(new Scan());
        List<Delete> deletes = Lists.newArrayList();
        for (Result row : scanner) {
            deletes.add(new Delete(row.getRow()));
        }
        table.delete(deletes);
    }

	/**
	 * load from hbase test
	 * 
	 * @throws IOException
	 */
	@Test
	public void testLoadFromHBase() throws IOException {
		prepareTable(TESTTABLE_1, true, DataFormat.UTF8PlainText);
		pig.registerQuery("a = load 'hbase://" + TESTTABLE_1 + "' using "
				+ "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
				+ TESTCOLUMN_A + " " + TESTCOLUMN_B + " " + TESTCOLUMN_C + " pig:col_d"
				+ "') as (col_a, col_b, col_c, col_d);");
		Iterator<Tuple> it = pig.openIterator("a");
		int count = 0;
		LOG.info("LoadFromHBase Starting");
		while (it.hasNext()) {
			Tuple t = it.next();
			String col_a = ((DataByteArray) t.get(0)).toString();
			String col_b = ((DataByteArray) t.get(1)).toString();
			String col_c = ((DataByteArray) t.get(2)).toString();
			Object col_d = t.get(3);       // empty cell
			Assert.assertEquals(count, Integer.parseInt(col_a));
			Assert.assertEquals(count + 0.0, Double.parseDouble(col_b), 1e-6);
			Assert.assertEquals("Text_" + count, col_c);
			Assert.assertNull(col_d);
			count++;
		}
		Assert.assertEquals(TEST_ROW_COUNT, count);
		LOG.info("LoadFromHBase done");
	}

	/**
	 * load from hbase test without hbase:// prefix
	 * 
	 * @throws IOException
	 */
	@Test
	public void testBackwardsCompatibility() throws IOException {
		prepareTable(TESTTABLE_1, true, DataFormat.UTF8PlainText);
		pig.registerQuery("a = load '" + TESTTABLE_1 + "' using "
				+ "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
				+ TESTCOLUMN_A + " " + TESTCOLUMN_B + " " + TESTCOLUMN_C
				+ "') as (col_a, col_b, col_c);");
		Iterator<Tuple> it = pig.openIterator("a");
		int count = 0;
        LOG.info("BackwardsCompatibility Starting");
		while (it.hasNext()) {
			Tuple t = it.next();
			String col_a = ((DataByteArray) t.get(0)).toString();
			String col_b = ((DataByteArray) t.get(1)).toString();
			String col_c = ((DataByteArray) t.get(2)).toString();

			Assert.assertEquals(count, Integer.parseInt(col_a));
			Assert.assertEquals(count + 0.0, Double.parseDouble(col_b), 1e-6);
			Assert.assertEquals("Text_" + count, col_c);
			count++;
		}
		Assert.assertEquals(TEST_ROW_COUNT, count);
        LOG.info("BackwardsCompatibility done");
	}

	/**
	 * load from hbase test including the row key as the first column
	 * 
	 * @throws IOException
	 */
	@Test
	public void testLoadFromHBaseWithRowKey() throws IOException {
		prepareTable(TESTTABLE_1, true, DataFormat.UTF8PlainText);
		pig.registerQuery("a = load 'hbase://" + TESTTABLE_1 + "' using "
				+ "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
				+ TESTCOLUMN_A + " " + TESTCOLUMN_B + " " + TESTCOLUMN_C
				+ "','-loadKey') as (rowKey,col_a, col_b, col_c);");
		Iterator<Tuple> it = pig.openIterator("a");
		int count = 0;
        LOG.info("LoadFromHBaseWithRowKey Starting");
		while (it.hasNext()) {
			Tuple t = it.next();
			String rowKey = ((DataByteArray) t.get(0)).toString();
			String col_a = ((DataByteArray) t.get(1)).toString();
			String col_b = ((DataByteArray) t.get(2)).toString();
			String col_c = ((DataByteArray) t.get(3)).toString();

			Assert.assertEquals("00".substring((count + "").length()) + count,
					rowKey);
			Assert.assertEquals(count, Integer.parseInt(col_a));
			Assert.assertEquals(count + 0.0, Double.parseDouble(col_b), 1e-6);
			Assert.assertEquals("Text_" + count, col_c);

			count++;
		}
		Assert.assertEquals(TEST_ROW_COUNT, count);
        LOG.info("LoadFromHBaseWithRowKey done");
	}

	/**
	 * Test Load from hbase with parameters lte and gte (01<=key<=98)
	 * 
	 */
	@Test
	public void testLoadWithParameters_1() throws IOException {
		prepareTable(TESTTABLE_1, true, DataFormat.UTF8PlainText);

		pig.registerQuery("a = load 'hbase://"
				+ TESTTABLE_1
				+ "' using "
				+ "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
				+ TESTCOLUMN_A
				+ " "
				+ TESTCOLUMN_B
				+ " "
				+ TESTCOLUMN_C
				+ "','-loadKey -gte 01 -lte 98') as (rowKey,col_a, col_b, col_c);");
		Iterator<Tuple> it = pig.openIterator("a");
		int count = 0;
		int next = 1;
        LOG.info("LoadFromHBaseWithParameters_1 Starting");
		while (it.hasNext()) {
			Tuple t = it.next();
			String rowKey = ((DataByteArray) t.get(0)).toString();
			String col_a = ((DataByteArray) t.get(1)).toString();
			String col_b = ((DataByteArray) t.get(2)).toString();
			String col_c = ((DataByteArray) t.get(3)).toString();

			Assert.assertEquals("00".substring((next + "").length()) + next,
					rowKey);
			Assert.assertEquals(next, Integer.parseInt(col_a));
			Assert.assertEquals(next + 0.0, Double.parseDouble(col_b), 1e-6);
			Assert.assertEquals("Text_" + next, col_c);

			count++;
			next++;
		}
		Assert.assertEquals(TEST_ROW_COUNT - 2, count);
        LOG.info("LoadFromHBaseWithParameters_1 done");
	}

	/**
	 * Test Load from hbase with parameters lt and gt (00<key<99)
	 */
	@Test
	public void testLoadWithParameters_2() throws IOException {
		prepareTable(TESTTABLE_1, true, DataFormat.UTF8PlainText);

		pig.registerQuery("a = load 'hbase://"
				+ TESTTABLE_1
				+ "' using "
				+ "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
				+ TESTCOLUMN_A
				+ " "
				+ TESTCOLUMN_B
				+ " "
				+ TESTCOLUMN_C
				+ "','-loadKey -gt 00 -lt 99') as (rowKey,col_a, col_b, col_c);");
		Iterator<Tuple> it = pig.openIterator("a");
		int count = 0;
		int next = 1;
        LOG.info("LoadFromHBaseWithParameters_2 Starting");
		while (it.hasNext()) {
			Tuple t = it.next();
			String rowKey = ((DataByteArray) t.get(0)).toString();
			String col_a = ((DataByteArray) t.get(1)).toString();
			String col_b = ((DataByteArray) t.get(2)).toString();
			String col_c = ((DataByteArray) t.get(3)).toString();

			Assert.assertEquals("00".substring((next + "").length()) + next,
					rowKey);
			Assert.assertEquals(next, Integer.parseInt(col_a));
			Assert.assertEquals(next + 0.0, Double.parseDouble(col_b), 1e-6);
			Assert.assertEquals("Text_" + next, col_c);

			count++;
			next++;
		}
		Assert.assertEquals(TEST_ROW_COUNT - 2, count);
        LOG.info("LoadFromHBaseWithParameters_2 done");
	}

	/**
	 * Test Load from hbase with parameters limit
	 */
	@Test
	public void testLoadWithParameters_3() throws IOException {
		prepareTable(TESTTABLE_1, true, DataFormat.UTF8PlainText);
		pig.registerQuery("a = load 'hbase://" + TESTTABLE_1 + "' using "
				+ "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
				+ TESTCOLUMN_A + " " + TESTCOLUMN_B + " " + TESTCOLUMN_C
				+ "','-loadKey -limit 10') as (rowKey,col_a, col_b, col_c);");
		Iterator<Tuple> it = pig.openIterator("a");
		int count = 0;
        LOG.info("LoadFromHBaseWithParameters_3 Starting");
		while (it.hasNext()) {
			Tuple t = it.next();
			String rowKey = ((DataByteArray) t.get(0)).toString();
			String col_a = ((DataByteArray) t.get(1)).toString();
			String col_b = ((DataByteArray) t.get(2)).toString();
			String col_c = ((DataByteArray) t.get(3)).toString();

			Assert.assertEquals("00".substring((count + "").length()) + count,
					rowKey);
			Assert.assertEquals(count, Integer.parseInt(col_a));
			Assert.assertEquals(count + 0.0, Double.parseDouble(col_b), 1e-6);
			Assert.assertEquals("Text_" + count, col_c);

			count++;
		}
		// 'limit' apply for each region and here we have only one region
		Assert.assertEquals(10, count);
        LOG.info("LoadFromHBaseWithParameters_3 done");
	}

	/**
     * Test Load from hbase with projection.
     */
    @Test
    public void testLoadWithProjection_1() throws IOException {
        prepareTable(TESTTABLE_1, true, DataFormat.HBaseBinary);
        scanTable1(pig, DataFormat.HBaseBinary);
        pig.registerQuery("b = FOREACH a GENERATE col_a, col_c;");
        Iterator<Tuple> it = pig.openIterator("b");
        int index = 0;
        LOG.info("testLoadWithProjection_1 Starting");
        while (it.hasNext()) {
            Tuple t = it.next();
            int col_a = (Integer) t.get(0);
            String col_c = (String) t.get(1);
            Assert.assertEquals(index, col_a);
            Assert.assertEquals("Text_" + index, col_c);
            Assert.assertEquals(2, t.size());
            index++;
        }
        Assert.assertEquals(100, index);
        LOG.info("testLoadWithProjection_1 done");
    }

    /**
     * Test Load from hbase with projection and the default caster.
     */
    @Test
    public void testLoadWithProjection_2() throws IOException {
        prepareTable(TESTTABLE_1, true, DataFormat.UTF8PlainText);
        scanTable1(pig, DataFormat.UTF8PlainText);
        pig.registerQuery("b = FOREACH a GENERATE col_a, col_c;");
        Iterator<Tuple> it = pig.openIterator("b");
        int index = 0;
        LOG.info("testLoadWithProjection_2 Starting");
        while (it.hasNext()) {
            Tuple t = it.next();
            int col_a = (Integer) t.get(0);
            String col_c = (String) t.get(1);
            Assert.assertEquals(index, col_a);
            Assert.assertEquals("Text_" + index, col_c);
            Assert.assertEquals(2, t.size());
            index++;
        }
        Assert.assertEquals(100, index);
        LOG.info("testLoadWithProjection_2 done");
    }

    /**
	 * Test Load from hbase using HBaseBinaryConverter
	 */
	@Test
	public void testHBaseBinaryConverter() throws IOException {
		prepareTable(TESTTABLE_1, true, DataFormat.HBaseBinary);
        scanTable1(pig, DataFormat.HBaseBinary);
		Iterator<Tuple> it = pig.openIterator("a");
		int index = 0;
        LOG.info("testHBaseBinaryConverter Starting");
		while (it.hasNext()) {
			Tuple t = it.next();
			String rowKey = (String) t.get(0);
			int col_a = (Integer) t.get(1);
			double col_b = (Double) t.get(2);
			String col_c = (String) t.get(3);

			Assert.assertEquals("00".substring((index + "").length()) + index,
					rowKey);
			Assert.assertEquals(index, col_a);
			Assert.assertEquals(index + 0.0, col_b, 1e-6);
			Assert.assertEquals("Text_" + index, col_c);
			index++;
		}
        LOG.info("testHBaseBinaryConverter done");
	}

	/**
	 * load from hbase 'TESTTABLE_1' using HBaseBinary format, and store it into
	 * 'TESTTABLE_2' using HBaseBinaryFormat
	 * 
	 * @throws IOException
	 */
	@Test
	public void testStoreToHBase_1() throws IOException {
		prepareTable(TESTTABLE_1, true, DataFormat.HBaseBinary);
		prepareTable(TESTTABLE_2, false, DataFormat.HBaseBinary);
        scanTable1(pig, DataFormat.HBaseBinary);
		pig.store("a", TESTTABLE_2,
				"org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
						+ TESTCOLUMN_A + " " + TESTCOLUMN_B + " "
						+ TESTCOLUMN_C + "','-caster HBaseBinaryConverter')");

        HTable table = new HTable(TESTTABLE_2);
		ResultScanner scanner = table.getScanner(new Scan());
		Iterator<Result> iter = scanner.iterator();
		int i = 0;
		for (i = 0; iter.hasNext(); ++i) {
			Result result = iter.next();
			String v = i + "";
			String rowKey = Bytes.toString(result.getRow());
            int col_a = Bytes.toInt(getColValue(result, TESTCOLUMN_A));
            double col_b = Bytes.toDouble(getColValue(result, TESTCOLUMN_B));
            String col_c = Bytes.toString(getColValue(result, TESTCOLUMN_C));

			Assert.assertEquals("00".substring(v.length()) + v, rowKey);
			Assert.assertEquals(i, col_a);
			Assert.assertEquals(i + 0.0, col_b, 1e-6);
			Assert.assertEquals("Text_" + i, col_c);
		}
		Assert.assertEquals(100, i);
	}

	/**
	 * load from hbase 'TESTTABLE_1' using HBaseBinary format, and store it into
     * 'TESTTABLE_2' using HBaseBinaryFormat projecting out column c
     *
     * @throws IOException
     */
    @Test
    public void testStoreToHBase_1_with_projection() throws IOException {
        System.getProperties().setProperty("pig.usenewlogicalplan", "false");
        prepareTable(TESTTABLE_1, true, DataFormat.HBaseBinary);
        prepareTable(TESTTABLE_2, false, DataFormat.HBaseBinary);
        scanTable1(pig, DataFormat.HBaseBinary);
        pig.registerQuery("b = FOREACH a GENERATE rowKey, col_a, col_b;");
        pig.store("b",  TESTTABLE_2,
                "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
                + TESTCOLUMN_A + " " + TESTCOLUMN_B +
        "','-caster HBaseBinaryConverter')");

        HTable table = new HTable(TESTTABLE_2);
        ResultScanner scanner = table.getScanner(new Scan());
        Iterator<Result> iter = scanner.iterator();
        int i = 0;
        for (i = 0; iter.hasNext(); ++i) {
            Result result = iter.next();
            String v = String.valueOf(i);
            String rowKey = Bytes.toString(result.getRow());
            int col_a = Bytes.toInt(getColValue(result, TESTCOLUMN_A));
            double col_b = Bytes.toDouble(getColValue(result, TESTCOLUMN_B));

            Assert.assertEquals("00".substring(v.length()) + v, rowKey);
            Assert.assertEquals(i, col_a);
            Assert.assertEquals(i + 0.0, col_b, 1e-6);
        }
        Assert.assertEquals(100, i);
    }

    /**
     * load from hbase 'TESTTABLE_1' using HBaseBinary format, and store it into
	 * 'TESTTABLE_2' using UTF-8 Plain Text format
	 * 
	 * @throws IOException
	 */
	@Test
	public void testStoreToHBase_2() throws IOException {
		prepareTable(TESTTABLE_1, true, DataFormat.HBaseBinary);
		prepareTable(TESTTABLE_2, false, DataFormat.HBaseBinary);
        scanTable1(pig, DataFormat.HBaseBinary);
		pig.store("a", TESTTABLE_2,
				"org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
						+ TESTCOLUMN_A + " " + TESTCOLUMN_B + " "
						+ TESTCOLUMN_C + "')");

        HTable table = new HTable(TESTTABLE_2);
		ResultScanner scanner = table.getScanner(new Scan());
		Iterator<Result> iter = scanner.iterator();
		int i = 0;
		for (i = 0; iter.hasNext(); ++i) {
			Result result = iter.next();
			String v = i + "";
			String rowKey = new String(result.getRow());
            int col_a = Integer.parseInt(new String(getColValue(result, TESTCOLUMN_A)));
            double col_b = Double.parseDouble(new String(getColValue(result, TESTCOLUMN_B)));
            String col_c = new String(getColValue(result, TESTCOLUMN_C));

			Assert.assertEquals("00".substring(v.length()) + v, rowKey);
			Assert.assertEquals(i, col_a);
			Assert.assertEquals(i + 0.0, col_b, 1e-6);
			Assert.assertEquals("Text_" + i, col_c);
		}
		Assert.assertEquals(100, i);
	}
    /**
     * load from hbase 'TESTTABLE_1' using HBaseBinary format, and store it into
     * 'TESTTABLE_2' using UTF-8 Plain Text format projecting column c
     *
     * @throws IOException
     */
    @Test
    public void testStoreToHBase_2_with_projection() throws IOException {
        prepareTable(TESTTABLE_1, true, DataFormat.HBaseBinary);
        prepareTable(TESTTABLE_2, false, DataFormat.UTF8PlainText);
        scanTable1(pig, DataFormat.HBaseBinary);
        pig.registerQuery("b = FOREACH a GENERATE rowKey, col_a, col_b;");
        pig.store("b", TESTTABLE_2,
                "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
                + TESTCOLUMN_A + " " + TESTCOLUMN_B + "')");

        HTable table = new HTable(TESTTABLE_2);
        ResultScanner scanner = table.getScanner(new Scan());
        Iterator<Result> iter = scanner.iterator();
        int i = 0;
        for (i = 0; iter.hasNext(); ++i) {
            Result result = iter.next();
            String v = i + "";
            String rowKey = new String(result.getRow());
            int col_a = Integer.parseInt(new String(getColValue(result, TESTCOLUMN_A)));
            double col_b = Double.parseDouble(new String(getColValue(result, TESTCOLUMN_B)));

            Assert.assertEquals("00".substring(v.length()) + v, rowKey);
            Assert.assertEquals(i, col_a);
            Assert.assertEquals(i + 0.0, col_b, 1e-6);
        }
        Assert.assertEquals(100, i);
    }

	/**
     * load from hbase 'TESTTABLE_1' using UTF-8 Plain Text format, and store it
     * into 'TESTTABLE_2' using UTF-8 Plain Text format projecting column c
     *
     * @throws IOException
     */
    @Test
    public void testStoreToHBase_3_with_projection_no_caster() throws IOException {
        prepareTable(TESTTABLE_1, true, DataFormat.UTF8PlainText);
        prepareTable(TESTTABLE_2, false, DataFormat.UTF8PlainText);
        scanTable1(pig, DataFormat.UTF8PlainText);
        pig.registerQuery("b = FOREACH a GENERATE rowKey, col_a, col_b;");
        pig.store("b", TESTTABLE_2,
                "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
                + TESTCOLUMN_A + " " + TESTCOLUMN_B + "')");

        HTable table = new HTable(TESTTABLE_2);
        ResultScanner scanner = table.getScanner(new Scan());
        Iterator<Result> iter = scanner.iterator();
        int i = 0;
        for (i = 0; iter.hasNext(); ++i) {
            Result result = iter.next();
            String v = i + "";
            String rowKey = new String(result.getRow());

            String col_a = new String(getColValue(result, TESTCOLUMN_A));
            String col_b = new String(getColValue(result, TESTCOLUMN_B));

            Assert.assertEquals("00".substring(v.length()) + v, rowKey);
            Assert.assertEquals(i + "", col_a);
            Assert.assertEquals(i + 0.0 + "", col_b);
        }
        Assert.assertEquals(100, i);
    }


    private void scanTable1(PigServer pig, DataFormat dataFormat) throws IOException {
        scanTable1(pig, dataFormat, "");
    }

    private void scanTable1(PigServer pig, DataFormat dataFormat, String extraParams) throws IOException {
        pig.registerQuery("a = load 'hbase://"
                + TESTTABLE_1
                + "' using "
                + "org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
                + TESTCOLUMN_A
                + " "
                + TESTCOLUMN_B
                + " "
                + TESTCOLUMN_C
                + "','-loadKey "
                + (dataFormat == DataFormat.HBaseBinary ? " -caster HBaseBinaryConverter" : "")
                + " " + extraParams + " "
                + "') as (rowKey:chararray,col_a:int, col_b:double, col_c:chararray);");
    }
    /**
	 * Prepare a table in hbase for testing.
	 * 
	 */
	private void prepareTable(String tableName, boolean initData,
			DataFormat format) throws IOException {
		// define the table schema
        HTable table = null;
        try {
            deleteAllRows(tableName);
        } catch (Exception e) {
            // It's ok, table might not exist.
        }
        try {
            table = util.createTable(Bytes.toBytesBinary(tableName),
                COLUMNFAMILY);
        } catch (Exception e) {
            table = new HTable(Bytes.toBytesBinary(tableName));
        }
		if (initData) {
			for (int i = 0; i < TEST_ROW_COUNT; i++) {
				String v = i + "";
				if (format == DataFormat.HBaseBinary) {
					// row key: string type
					Put put = new Put(Bytes.toBytes("00".substring(v.length())
							+ v));
					// col_a: int type
					put.add(COLUMNFAMILY, Bytes.toBytes("col_a"),
							Bytes.toBytes(i));
					// col_b: double type
					put.add(COLUMNFAMILY, Bytes.toBytes("col_b"),
							Bytes.toBytes(i + 0.0));
					// col_c: string type
					put.add(COLUMNFAMILY, Bytes.toBytes("col_c"),
							Bytes.toBytes("Text_" + i));
					table.put(put);
				} else {
					// row key: string type
					Put put = new Put(
							("00".substring(v.length()) + v).getBytes());
					// col_a: int type
					put.add(COLUMNFAMILY, Bytes.toBytes("col_a"),
							(i + "").getBytes()); // int
					// col_b: double type
					put.add(COLUMNFAMILY, Bytes.toBytes("col_b"),
							((i + 0.0) + "").getBytes());
					// col_c: string type
					put.add(COLUMNFAMILY, Bytes.toBytes("col_c"),
							("Text_" + i).getBytes());
					table.put(put);
				}
			}
			table.flushCommits();
		}
	}

	/**
     * Helper to deal with fetching a result based on a cf:colname string spec
     * @param result
     * @param colName
     * @return
	 */
    private static byte[] getColValue(Result result, String colName) {
        byte[][] colArray = Bytes.toByteArrays(colName.split(":"));
        byte[] val = result.getValue(colArray[0], colArray[1]);
        return result.getValue(colArray[0], colArray[1]);

}
}