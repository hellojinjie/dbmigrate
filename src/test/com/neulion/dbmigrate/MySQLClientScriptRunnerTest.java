package com.neulion.dbmigrate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.neulion.dbmigrate.ScriptRunner.ScriptRunnerException;

public class MySQLClientScriptRunnerTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRunScript() throws Exception {
		DatabaseInfo dbInfo = new DatabaseInfo();
		dbInfo.user = "root";
		dbInfo.password = "123456";
		dbInfo.host = "127.0.0.1";
		
		ProcessBuilder pb = new ProcessBuilder();
		pb.command("mysql.exe", "-uroot", "-p123456");
		pb.redirectErrorStream(true);
		
		Process process = pb.start();
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		bw.write("create database fffff;");
		bw.close();
		
		int returnCode = process.waitFor();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		StringBuilder message = new StringBuilder();
		String line = br.readLine();
		while (line != null) {
			message.append(line);
			message.append("\n");
			line = br.readLine();
		}
		br.close();
		if (returnCode != 0) {
			throw new ScriptRunnerException(message.toString());
		}
	}

}
