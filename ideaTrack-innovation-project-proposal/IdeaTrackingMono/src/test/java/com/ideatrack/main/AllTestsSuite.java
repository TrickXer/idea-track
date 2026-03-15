package com.ideatrack.main;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("All Tests Suite - IdeaTracking Application")
@SelectPackages({
    "com.ideatrack.main.controller",
    "com.ideatrack.main.repository", 
    "com.ideatrack.main.service",
    "com.ideatrack.main.data",
    "com.ideatrack.main"
})
public class AllTestsSuite {
	
}
