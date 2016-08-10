package edu.wm.cs.semeru.CorpusGenerator.tests;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AllUnitTests extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite("All Unit Tests");
        
        suite.addTestSuite(CorpusGeneratorMethodLevelGranularityTest.class);
        suite.addTestSuite(CorpusGeneratorClassLevelGranularityTest.class);
        suite.addTestSuite(CorpusGeneratorFileLevelGranularityTest.class);
        
        return suite;
    }
}
