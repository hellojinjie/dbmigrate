package net.evermemo.dbmigrate;

import java.io.File;

public interface ScriptRunner {

	public void runScript(File scriptFile, DatabaseInfo dbInfo) throws Exception;
	
	public class ScriptRunnerException extends RuntimeException {

		private static final long serialVersionUID = 1L;
		
		public ScriptRunnerException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public ScriptRunnerException(String message) {
			super(message);
		}
	}
	
}
