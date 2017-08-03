/* Generated by YAKINDU Statechart Tools code generator. */
package org.yakindu.sct.generator.cpp.test;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.yakindu.sct.generator.c.gtest.GTest;
import org.yakindu.sct.generator.c.gtest.GTestRunner;
import org.yakindu.sct.generator.c.gtest.GTestHelper;

@GTest(
	statechartBundle = "org.yakindu.sct.test.models",
	sourceFile = "gtests/SameNameDifferentRegionTest/SameNameDifferentRegionTest.cc",
	program = "gtests/SameNameDifferentRegionTest/SameNameDifferentRegion",
	model = "testmodels/SCTUnit/SameNameDifferentRegion.sct",
	additionalFilesToCopy = {
		"libraryTarget/sc_runner.h",
		"libraryTarget/sc_runner.cpp"
	},
	additionalFilesToCompile = {
		"SameNameDifferentRegion.cpp",
		"sc_runner.cpp"
	}
)
@RunWith(GTestRunner.class)
public class SameNameDifferentRegionTest {
protected final GTestHelper helper = new GTestHelper(this);

	@Before
	public void setUp() {
		helper.generate();
		helper.compile();
	}

}
