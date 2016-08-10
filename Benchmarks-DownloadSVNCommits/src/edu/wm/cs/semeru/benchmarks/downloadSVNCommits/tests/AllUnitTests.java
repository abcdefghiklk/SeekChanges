package edu.wm.cs.semeru.benchmarks.downloadSVNCommits.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllUnitTests extends TestCase
{
	public static Test suite()
	{
		TestSuite suite=new TestSuite("All Unit Tests");

		suite.addTestSuite(jEditSVNDownloadTest.class);
		suite.addTestSuite(ArgoUMLSVNDownloadTest.class);

		return suite;
	}
}
