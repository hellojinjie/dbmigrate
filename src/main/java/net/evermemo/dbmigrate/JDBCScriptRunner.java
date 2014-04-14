package net.evermemo.dbmigrate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;

@Deprecated
public class JDBCScriptRunner implements ScriptRunner {

	@Override
	public void runScript(File scriptFile, DatabaseInfo dbInfo)
			throws Exception {
		Connection conn = new MigrateService().getConnection(dbInfo);
		try {
			QueryRunner runner = new QueryRunner();
			conn.setAutoCommit(false);
			List<String> queries = this.getQueries(scriptFile.getCanonicalPath());
			for (String query : queries) {
				runner.update(conn, query);
			}
			conn.commit();
		} catch (Exception e) {
			conn.rollback();
			throw e;
		} finally {
			DbUtils.close(conn);
		}
	}
	
	private List<String> getQueries(String filename) throws IOException {
		List<String> queries = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line = br.readLine();
		String query = "";
		while (line != null) {
			if (line.startsWith("--")) {
				if (!"".equals(query)) {
					queries.add(query);
				}
				query = "";
			} else {
				query += line;
				query += "\n";
			}
			line = br.readLine();
		}
		if (!"".equals(query)) {
			queries.add(query);
		}
		br.close();
		return queries;
	}

}
