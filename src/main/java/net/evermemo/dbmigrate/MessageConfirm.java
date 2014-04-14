package net.evermemo.dbmigrate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MessageConfirm {

	private static final Log log = LogFactory.getLog(MessageConfirm.class); 
	
	private boolean interactive = true;

	public boolean confirm(String message) throws IOException {
		if (!interactive) {
			return true;
		}
		boolean result = false;
		log.info(message + " y/[n]:");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();
		if ("y".equalsIgnoreCase(line)) {
			result = true;
		}
		return result;
	}
	
	public boolean isInteractive() {
		return interactive;
	}

	public void setInteractive(boolean interactive) {
		this.interactive = interactive;
	}
	
}
