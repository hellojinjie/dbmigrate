package net.evermemo.dbmigrate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;

public class MySQLClientScriptRunner implements ScriptRunner {

	@Override
	public void runScript(File scriptFile, DatabaseInfo dbInfo)
			throws Exception {
		
		String command = "mysql -c -u{0} -p{1} -h{2} -P{3} -D{4}";
		command = MessageFormat.format(command, dbInfo.user, dbInfo.password, 
				dbInfo.host, dbInfo.port, dbInfo.database);

		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(command.split(" "));
		processBuilder.redirectErrorStream(true);
		
		Process process = processBuilder.start();
		
		String script = this.getFileContent(scriptFile);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		bw.write(script);
		bw.close();
		
		int returnCode = process.waitFor();
		if (returnCode != 0) {
			BufferedReader resultReader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			StringBuilder message = new StringBuilder();
			String resultLine = resultReader.readLine();
			while (resultLine != null) {
				message.append(resultLine);
				message.append("\n");
				resultLine = resultReader.readLine();
			}
			resultReader.close();
			throw new ScriptRunnerException(message.toString());
		}
	}

	private String getFileContent(File file) throws Exception {
		StringBuilder builder = new StringBuilder();
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = br.readLine();
		while (line != null) {
			builder.append(line);
			builder.append("\n");
			line = br.readLine();
		}
		br.close();
		
		return builder.toString();
	}
	
}
