/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pig.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.pig.impl.PigContext;

import junit.framework.TestCase;

import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.io.FileLocalizer;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.util.PropertiesUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestPigServer extends TestCase {
    private PigServer pig = null;
    static MiniCluster cluster = MiniCluster.buildCluster();
    private File stdOutRedirectedFile;

    @Before
    @Override
    public void setUp() throws Exception{
        pig = new PigServer(ExecType.MAPREDUCE, cluster.getProperties());
        stdOutRedirectedFile = new File("stdout.redirected");
        // Create file if it does not exist
        try {
            if(!stdOutRedirectedFile.createNewFile())
                fail("Unable to create input files");
        } catch (IOException e) {
            fail("Unable to create input files:" + e.getMessage());
        }
    }
    
    @After
    @Override
    public void tearDown() throws Exception{
        pig = null;
        stdOutRedirectedFile.delete();
    }
    
    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        cluster.shutDown();
    }
    
    private final static String FILE_SEPARATOR = System.getProperty("file.separator");
    
    // make sure that name is included or not (depending on flag "included") 
    // in the given list of stings
    private static void verifyStringContained(List<URL> list, String name, boolean included) {
        Iterator<URL> iter = list.iterator();
        boolean nameIsSubstring = false;
        int count = 0;
        
        while (iter.hasNext()) {
            if (iter.next().toString().contains(name)) {
                nameIsSubstring = true;
                ++count;
            }
        }
        
        if (included) {
            assertTrue(nameIsSubstring);
            assertTrue(count == 1);
        }
        else {
            assertFalse(nameIsSubstring);
        }
    }
    
    // creates an empty jar file
    private static void createFakeJarFile(String location, String name) 
            throws IOException {
        assertFalse((new File(name)).canRead());
        
        System.err. println("Location: " + location);
        assertTrue((new File(location)).mkdirs());
        
        assertTrue((new File(location + FILE_SEPARATOR + name)).
                    createNewFile());
    }
    
    // dynamically add more resources to the system class loader
    private static void registerNewResource(String file) throws Exception {
        URL urlToAdd = new File(file).toURI().toURL();
        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Method addMethod = URLClassLoader.class.
                            getDeclaredMethod("addURL",
                                              new Class[]{URL.class});
        addMethod.setAccessible(true);
        addMethod.invoke(sysLoader, new Object[]{urlToAdd});
    }
    
    /**
     * The jar file to register is not present
     */
    @Test
    public void testRegisterJarFileNotPresent() throws Throwable {
        // resister a jar file that does not exist
        
        String jarName = "BadFileNameTestJarNotPresent.jar";
        
        // jar name is not present to start with
        verifyStringContained(pig.getPigContext().extraJars, jarName, false);

        boolean exceptionRaised = false;
        try {
            pig.registerJar(jarName);
        }
        catch (IOException e) {
            exceptionRaised = true;
        }        
        assertTrue(exceptionRaised);
        verifyStringContained(pig.getPigContext().extraJars, jarName, false);
    }

    /**
     * Jar file to register is not present in the system resources
     * in this case name of jar file is relative to current working dir
     */
    @Test
    public void testRegisterJarLocalDir() throws Throwable {
        String dir1 = "test1_register_jar_local";
        String dir2 = "test2_register_jar_local";
        String jarLocation = dir1 + FILE_SEPARATOR +
                              dir2 + FILE_SEPARATOR;
        String jarName = "TestRegisterJarLocal.jar";
        
        
        createFakeJarFile(jarLocation, jarName);
        
        verifyStringContained(pig.getPigContext().extraJars, jarName, false);
        
        boolean exceptionRaised = false;
        try {
            pig.registerJar(jarLocation + jarName);
        }
        catch (IOException e) {
            exceptionRaised = true;
        }        
        assertFalse(exceptionRaised);
        verifyStringContained(pig.getPigContext().extraJars, jarName, true);

        // clean-up
        assertTrue((new File(jarLocation + jarName)).delete());
        (new File(dir1 + FILE_SEPARATOR + dir2)).delete();
        (new File(dir1)).delete();
    }

    /**
     * Jar file is located via system resources
     * Test verifies that even with multiple resources matching,
     * only one of them is registered.
     */
    @Test
    public void testRegisterJarFromResources () throws Throwable {
        String dir = "test_register_jar_res_dir";
        String subDir1 = "test_register_jar_res_sub_dir1";
        String subDir2 = "test_register_jar_res_sub_dir2";
        String jarName = "TestRegisterJarFromRes.jar";
        String jarLocation1 = dir + FILE_SEPARATOR + subDir1 + FILE_SEPARATOR;
        String jarLocation2 = dir + FILE_SEPARATOR + subDir2 + FILE_SEPARATOR;
        
        
        createFakeJarFile(jarLocation1, jarName);
        createFakeJarFile(jarLocation2, jarName);
        
        verifyStringContained(pig.getPigContext().extraJars, jarName, false);
        
        registerNewResource(jarLocation1);
        registerNewResource(jarLocation2);
        
        boolean exceptionRaised = false;
        try {
            pig.registerJar(jarName);
        }
        catch (IOException e) {
            exceptionRaised = true;
        }
        assertFalse(exceptionRaised);
        verifyStringContained(pig.getPigContext().extraJars, jarName, true);

        // clean-up
        assertTrue((new File(jarLocation1 + jarName)).delete());
        assertTrue((new File(jarLocation2 + jarName)).delete());
        (new File(jarLocation1)).delete();
        (new File(jarLocation2)).delete();
        (new File(dir)).delete();
    }

    /**
     * Use a resource inside a jar file.
     * Verify that the containing jar file is registered correctly.
     * @throws Exception
     */
    @Test
    public void testRegisterJarResourceInJar() throws Throwable {
        String dir = "test_register_jar_res_in_jar";
        String subDir = "sub_dir";
        String jarName = "TestRegisterJarNonEmpty.jar";
        String className = "TestRegisterJar";
        String javaSrc = "package " + subDir + "; class " + className + " { }";

        
        // create dirs
        (new File(dir + FILE_SEPARATOR + subDir)).mkdirs();

        // generate java file
        FileOutputStream outStream = 
            new FileOutputStream(new File(dir + FILE_SEPARATOR + subDir +
                                    FILE_SEPARATOR + className + ".java"));
        
        OutputStreamWriter outWriter = new OutputStreamWriter(outStream);
        outWriter.write(javaSrc);
        outWriter.close();
        
        // compile
        int status;
        status = Util.executeJavaCommand("javac " + dir + FILE_SEPARATOR + subDir +
                               FILE_SEPARATOR + className + ".java");
        assertTrue(status==0);

        // remove src file
        (new File(dir + FILE_SEPARATOR + subDir +
                  FILE_SEPARATOR + className + ".java")).delete();

        // generate jar file
        status = Util.executeJavaCommand("jar -cf " + dir + FILE_SEPARATOR + jarName + " " +
                              "-C " + dir + " " + subDir);
        assertTrue(status==0);
        
        // remove class file and sub_dir
        (new File(dir + FILE_SEPARATOR + subDir +
                  FILE_SEPARATOR + className + ".class")).delete();
        (new File(dir + FILE_SEPARATOR + subDir)).delete();
        
        // register resource
        registerNewResource(dir + FILE_SEPARATOR + jarName);
        
        // load the specific resource
        boolean exceptionRaised = false;
        try {
            pig.registerJar("sub_dir/TestRegisterJar.class");
        }
        catch (IOException e) {
            exceptionRaised = true;
        }
        
        // verify proper jar file is located
        assertFalse(exceptionRaised);
        verifyStringContained(pig.getPigContext().extraJars, jarName, true);

        // clean up Jar file and test dir
        (new File(dir + FILE_SEPARATOR + jarName)).delete();
        (new File(dir)).delete();
    }

    @Test
    public void testDescribeLoad() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        System.setOut(out);
        pig.dumpSchema("a") ;
        out.close(); // Remember this!
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("a: {field1: int,field2: float,field3: chararray}") == true);
        }
        reader.close();
    }

    @Test
    public void testDescribeFilter() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = filter a by field1 > 10;") ;
        System.setOut(out);
        pig.dumpSchema("b") ;
        out.close(); // Remember this!
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("b: {field1: int,field2: float,field3: chararray}") == true);
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeDistinct() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = distinct a ;") ;
        System.setOut(out);
        pig.dumpSchema("b") ;
        out.close(); // Remember this!
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("b: {field1: int,field2: float,field3: chararray}") == true);
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeSort() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = order a by * desc;") ;
        System.setOut(out);
        pig.dumpSchema("b") ;
        out.close(); // Remember this!
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("b: {field1: int,field2: float,field3: chararray}") == true);
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeLimit() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = limit a 10;") ;
        System.setOut(out);
        pig.dumpSchema("b") ;
        out.close(); // Remember this!
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("b: {field1: int,field2: float,field3: chararray}") == true);
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeForeach() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = foreach a generate field1 + 10;") ;
        System.setOut(out);
        pig.dumpSchema("b") ;
        out.close(); 
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("b: {int}") == true);
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeForeachFail() throws Throwable {

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = foreach a generate field1 + 10;") ;
        try {
            pig.dumpSchema("c") ;
            fail("Error expected");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unable to describe schema for alias c"));
        }
    }

    @Test
    public void testDescribeForeachNoSchema() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' ;") ;
        pig.registerQuery("b = foreach a generate *;") ;
        System.setOut(out);
        pig.dumpSchema("b") ;
        out.close(); 
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("Schema for b unknown.") == true);
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeCogroup() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = load 'b' as (field4, field5: double, field6: chararray );") ;
        pig.registerQuery("c = cogroup a by field1, b by field4;") ;
        System.setOut(out);
        pig.dumpSchema("c") ;
        out.close(); 
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("c: {group: int,a: {field1: int,field2: float,field3: chararray},b: {field4: bytearray,field5: double,field6: chararray}}") == true);
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeCross() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = load 'b' as (field4, field5: double, field6: chararray );") ;
        pig.registerQuery("c = cross a, b;") ;
        System.setOut(out);
        pig.dumpSchema("c") ;
        out.close(); 
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("c: {a::field1: int,a::field2: float,a::field3: chararray,b::field4: bytearray,b::field5: double,b::field6: chararray}") == true);
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeJoin() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = load 'b' as (field4, field5: double, field6: chararray );") ;
        pig.registerQuery("c = join a by field1, b by field4;") ;
        System.setOut(out);
        pig.dumpSchema("c") ;
        out.close(); 
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertEquals("c: {a::field1: int,a::field2: float,a::field3: chararray,b::field4: bytearray,b::field5: double,b::field6: chararray}", s );
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeUnion() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (field1: int, field2: float, field3: chararray );") ;
        pig.registerQuery("b = load 'b' as (field4, field5: double, field6: chararray );") ;
        pig.registerQuery("c = union a, b;") ;
        System.setOut(out);
        pig.dumpSchema("c") ;
        out.close(); 
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            assertTrue(s.equals("c: {field1: int,field2: double,field3: chararray}") == true);
        }
        fileWithStdOutContents.close();
    }

    @Test
    public void testDescribeComplex() throws Throwable {
        PrintStream console = System.out;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(stdOutRedirectedFile)));

        pig.registerQuery("a = load 'a' as (site: chararray, count: int, itemCounts: bag { itemCountsTuple: tuple (type: chararray, typeCount: int, f: float, m: map[]) } ) ;") ;
        pig.registerQuery("b = foreach a generate site, count, FLATTEN(itemCounts);") ;
        System.setOut(out);
        pig.dumpSchema("b") ;
        out.close();
        System.setOut(console);

        String s;
        InputStream fileWithStdOutContents = new DataInputStream( new BufferedInputStream( new FileInputStream(stdOutRedirectedFile)));
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileWithStdOutContents));
        while ((s = reader.readLine()) != null) {
            // strip away the initial schema alias and the
            // curlies surrounding the schema to construct
            // the schema object from the schema string
            s = s.replaceAll("^.*\\{", "");
            s = s.replaceAll("\\}$", "");
            Schema actual = Util.getSchemaFromString( s);
            Schema expected = Util.getSchemaFromString(
                    "site: chararray,count: int," +
                    "itemCounts::type: chararray,itemCounts::typeCount: int," +
                    "itemCounts::f: float,itemCounts::m: map[ ]");
            assertEquals(expected, actual);
        }
        fileWithStdOutContents.close();
    }
    
    @Test
    public void testParamSubstitution() throws Exception{
        // using params map
        PigServer pig=new PigServer(ExecType.LOCAL);
        Map<String,String> params=new HashMap<String, String>();
        params.put("input", "test/org/apache/pig/test/data/passwd");
        File scriptFile=Util.createFile(new String[]{"a = load '$input' using PigStorage(':');"});
        pig.registerScript(scriptFile.getAbsolutePath(),params);
        Iterator<Tuple> iter=pig.openIterator("a");
        int index=0;
        List<Tuple> expectedTuples=Util.readFile2TupleList("test/org/apache/pig/test/data/passwd", ":");
        while(iter.hasNext()){
            Tuple tuple=iter.next();
            assertEquals(tuple.get(0).toString(), expectedTuples.get(index).get(0).toString());
            index++;
        }
        
        // using param file
        pig=new PigServer(ExecType.LOCAL);
        List<String> paramFile=new ArrayList<String>();
        paramFile.add(Util.createFile(new String[]{"input=test/org/apache/pig/test/data/passwd2"}).getAbsolutePath());
        pig.registerScript(scriptFile.getAbsolutePath(),paramFile);
        iter=pig.openIterator("a");
        index=0;
        expectedTuples=Util.readFile2TupleList("test/org/apache/pig/test/data/passwd2", ":");
        while(iter.hasNext()){
            Tuple tuple=iter.next();
            assertEquals(tuple.get(0).toString(), expectedTuples.get(index).get(0).toString());
            index++;
        }
        
        // using both param value and param file, param value should override param file
        pig=new PigServer(ExecType.LOCAL);
        pig.registerScript(scriptFile.getAbsolutePath(),params,paramFile);
        iter=pig.openIterator("a");
        index=0;
        expectedTuples=Util.readFile2TupleList("test/org/apache/pig/test/data/passwd", ":");
        while(iter.hasNext()){
            Tuple tuple=iter.next();
            assertEquals(tuple.get(0).toString(), expectedTuples.get(index).get(0).toString());
            index++;
        }
    }
    
    @Test
    public void testPigProperties() throws Throwable {
        File defaultPropertyFile = new File("pig-default.properties");
        File propertyFile = new File("pig.properties");
        File cliPropertyFile = new File("commandLine_pig.properties");
        
        Properties properties = PropertiesUtil.loadDefaultProperties();
        assertTrue(properties.getProperty("test123")==null);

        PrintWriter out = new PrintWriter(new FileWriter(defaultPropertyFile));
        out.println("test123=defaultproperties");
        out.close();
        
        properties = PropertiesUtil.loadDefaultProperties();
        assertTrue(properties.getProperty("test123").equals("defaultproperties"));

        out = new PrintWriter(new FileWriter(propertyFile));
        out.println("test123=properties");
        out.close();

        properties = PropertiesUtil.loadDefaultProperties();
        assertTrue(properties.getProperty("test123").equals("properties"));
        
        out = new PrintWriter(new FileWriter(cliPropertyFile));
        out.println("test123=cli_properties");
        out.close();

        properties = PropertiesUtil.loadDefaultProperties();
        PropertiesUtil.loadPropertiesFromFile(properties,
                "commandLine_pig.properties");
        assertTrue(properties.getProperty("test123").equals("cli_properties"));
        
        defaultPropertyFile.delete();
        propertyFile.delete();
        cliPropertyFile.delete();
    }

    @Test
    public void testPigTempDir() throws Throwable {
        File defaultPropertyFile = new File("pig-default.properties");
        PrintWriter out = new PrintWriter(new FileWriter(defaultPropertyFile));
        out.println("pig.temp.dir=/opt/temp");
        out.close();
        Properties properties = PropertiesUtil.loadDefaultProperties();
        PigContext pigContext=new PigContext(ExecType.LOCAL, properties);
        pigContext.connect();
        FileLocalizer.setInitialized(false);
        String tempPath= FileLocalizer.getTemporaryPath(pigContext).toString();
        assertTrue(tempPath.startsWith("file:/opt/temp"));
        defaultPropertyFile.delete();
        FileLocalizer.setInitialized(false);
    }

}
