package com.neulion.dbmigrate;

import static org.junit.Assert.*;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MigrateConfigurationTest {

	private static final Log log = LogFactory.getLog(MigrateConfigurationTest.class); 
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLoadConfiguration() {
		MigrateConfiguration config = new MigrateConfiguration();
		config.loadConfiguration("configure.xml");
		
		assertTrue(true);
	}

}
