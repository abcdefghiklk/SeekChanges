package edu.wm.cs.semeru.corpus.corpusAndQueriesConverterToVSMFormat.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllUnitTests extends TestCase
{
	public static Test suite()
	{
		TestSuite suite=new TestSuite("All Unit Tests");

		suite.addTestSuite(System2Test.class);

		return suite;
	}
}
