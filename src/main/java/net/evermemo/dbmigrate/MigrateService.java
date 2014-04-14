package net.evermemo.dbmigrate;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class MigrateService {

	private static final Log log = LogFactory.getLog(MigrateService.class);
	
	private MigrateConfiguration config;
	private MessageConfirm messageConfirm = new MessageConfirm();
	private ScriptRunner scriptRunner = new MySQLClientScriptRunner();
	
	public MigrateService() {
		
	}
	
	public MigrateService(MigrateConfiguration config) {
		this.config = config;
	}
	
	public void actionApply(String databases) {
		List<DatabaseInfo> dbs = this.getDatabaseInfos(databases);
		for (DatabaseInfo db : dbs) {
			try {
				this.applyDatabaseMigration(db);
			} catch (Exception e) {
				log.error("Error apply migration to " + db.alias + ": " + e.getMessage());
				log.error("");
				continue;
			}
		}
	}
	
	public void actionNew(String databases) {
		List<DatabaseInfo> dbs = this.getDatabaseInfos(databases);
		for (DatabaseInfo info : dbs) {
			try {
				this.listNewMigration(info);
			} catch (Exception e) {
				log.error("Error occur while listing new migration of " + info.alias 
						+ ": " + e.getMessage());
				log.error("");
			}
		}
	}
	
	public void actionHistory(String databases) {
		List<DatabaseInfo> dbs = this.getDatabaseInfos(databases);
		for (DatabaseInfo info : dbs) {
			try {
				this.listMigrateHistoryTable(info);
			} catch (Exception e) {
				log.error("Error occur while listing migration history of " + info.alias 
						+ ": " + e.getMessage());
				log.error("");
			}
		}
	}
	
	public void actionMark(String databases, String version) {
		if (version.length() != 13) {
			log.error("Error version of migration");
			log.error("");
			return;
		}
		List<DatabaseInfo> dbs = this.getDatabaseInfos(databases);
		for (DatabaseInfo info : dbs) {
			try {
				this.printLogSeperator(info);
				if (messageConfirm.confirm("Set migration history at " + version + "?")) {
					this.markMigration(info, version);
				}
			} catch (Exception e) {
				log.error("Error occur while mark migration of " + info.alias 
						+ ": " + e.getMessage());
				log.error("");
			}
		}
	}
	
	/**
	 * Create a new patch, branch has not been implemented.
	 * @param patchName
	 * @param branch
	 */
	public void actionCreate(String patchName, String branch) {
		Date current = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat();
		dateFormat.applyPattern("yyMMdd_HHmmss_");
		String migrationName = "m" + dateFormat.format(current);
		migrationName += patchName;
		
		String filename = config.getMigrationDirectory() + "/" + migrationName + ".sql";
		
		File file = new File(filename);
		try {
			if (messageConfirm.confirm("Create new migration '" + file.getCanonicalPath() + "'?")) {
				file.createNewFile();
				log.info("Create migration successfully");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void listNewMigration(DatabaseInfo dbInfo) {
		this.printLogSeperator(dbInfo);
		String[] migrations = this.getNewMigrations(dbInfo);
		if (migrations.length == 0) {
			log.info("No new migrations, " + dbInfo.alias + " is up-to-date");
			log.info("");
		} else {
			log.info("Found " + migrations.length + " new migration:");
			for (String migration : migrations) {
				log.info("\t" + migration);
			}
			log.info("");
		}
	}
	
	private void markMigration(DatabaseInfo dbInfo, String version) throws Exception {
		String[] migrations = this.getNewMigrations(dbInfo);
		boolean exists = false;
		List<String> migrationToMark = new ArrayList<String>();
		for (String m : migrations) {
			migrationToMark.add(m);
			if (m.startsWith("m" + version)) {
				exists = true;
				break;
			}
		}
		if (exists) {
			Connection conn = this.getConnection(dbInfo);
			QueryRunner runner = new QueryRunner();
			for (String migration : migrationToMark) {
				runner.update(conn, RECORD_VERSION, migration);
			}
			DbUtils.close(conn);
			log.info("Set migration history at " + version + " successfully");
		} else {
			log.error("Error version of migration");
			log.error("");
		}
	}
	
	private String[] getNewMigrations(DatabaseInfo dbInfo) {
		File migrationDir = new File(config.getMigrationDirectory());
		String[] migrations = migrationDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.startsWith("m") && name.endsWith(".sql")) {
					return true;
				}
				return false;
			}
		});
		List<String> migrationsList = new ArrayList<String>();
		
		List<MigrationTable> history = this.getMigrateHistoryTable(dbInfo);
		if (history.size() == 0) {
			migrationsList = Arrays.asList(migrations);
		} else {
			MigrationTable latestMigration = history.get(0);
			for (String s : migrations) {
				if (s.compareTo(latestMigration.version) > 0) {
					migrationsList.add(s);
				}
			}
		}
		
		String[] result = migrationsList.toArray(new String[0]);
		Arrays.sort(result);
		return result;
	}
	
	private void applyDatabaseMigration(DatabaseInfo dbInfo) throws Exception {
		Connection conn = this.getConnection(dbInfo);
		QueryRunner runner = new QueryRunner();
		String[] migrations = this.getNewMigrations(dbInfo);
		this.printLogSeperator(dbInfo);
		if (migrations.length == 0) {
			log.info("No new migrations, " + dbInfo.alias + " is up-to-date");
			log.info("");
		} else {
			log.info("Total " + migrations.length + " new migration to be applied:");
			for (String migration : migrations) {
				log.info("\t" + migration);
			}
			log.info("");
			if (!messageConfirm.confirm("Apply above migration? ")) {
				return;
			}
			for (String migration : migrations) {
				try {
					log.info("\tappling " + migration);
					scriptRunner.runScript(new File(config.getMigrationDirectory() + "/" + migration), dbInfo);
					runner.update(conn, RECORD_VERSION, migration);
				} catch (Exception e) {
					log.error("Error ocurr while appling: " + migration);
					throw e;
				}
			}
			DbUtils.closeQuietly(conn);
			log.info("Apply " + migrations.length + " migrations to " + dbInfo.alias + " successfully");
			log.info("");
		}
	}

	private List<DatabaseInfo> getDatabaseInfos(String databases) {
		List<DatabaseInfo> dbs = new ArrayList<DatabaseInfo>();
		if (databases == null || "".equals(databases)) {
			/* for the default databases */
			for (String alias : config.getDefaultDatabases()) {
				dbs.add(config.getDatabases().get(alias));
			}
		} else if ("all".equalsIgnoreCase(databases)) {
			/* query all databases */
			dbs.addAll(config.getDatabases().values());
		} else {
			/* query specific databases, separated by comma */
			String[] ds = databases.split(",");
			for (String s : ds) {
				if (config.getDatabases().containsKey(s)) {
					dbs.add(config.getDatabases().get(s));
				} else {
					log.warn("Database alias " + s + " NOT FOUND");
					log.warn("");
				}
			}
		}
		return dbs;
	}
	
	private void listMigrateHistoryTable(DatabaseInfo dbInfo) {
		List<MigrationTable> result = this.getMigrateHistoryTable(dbInfo);
		this.printLogSeperator(dbInfo);
		log.info("Total " + result.size() + " migrations have been applied before:");
		log.info("\tapply time\t\tversion");
		for (MigrationTable row : result) {
			log.info("\t" + row.getApply_time() + "\t" + row.getVersion());
		}
		log.info("");
	}
	
	private void printLogSeperator(DatabaseInfo dbInfo) {
		StringBuilder logSeperator = new StringBuilder();
		for (int i = 0; i < 60; i++) {
			logSeperator.append("=");
		}
		log.info(logSeperator.toString());
		log.info("Database Ailas: " + MessageFormat.format("{0} ({1}:{2}/{3})", 
				dbInfo.alias, dbInfo.host, dbInfo.port, dbInfo.database));
		log.info(logSeperator.toString());
	}
	
	private List<MigrationTable> getMigrateHistoryTable(DatabaseInfo dbInfo) {
		Connection conn = this.getConnection(dbInfo);
		QueryRunner runner = new QueryRunner();
		try {
			long count = runner.query(conn, COUNT_MIGRATE_HISTORY_TABLE, new ScalarHandler<Long>());
			if (count == 0) {
				throw new RuntimeException("Missing table tbl_migration. Abort!");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} 
		List<MigrationTable> result = null;
		try {
			ResultSetHandler<List<MigrationTable>> handler = new BeanListHandler<MigrationTable>(MigrationTable.class);
			result = runner.query(conn, SELECT_MIGRATE_HISTORY_TABLE, handler);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			DbUtils.closeQuietly(conn);
		}
		return result;
	}
	
	public Connection getConnection(DatabaseInfo dbInfo) {
		Connection conn = null;
	    Properties connectionProps = new Properties();
	    connectionProps.put("user", dbInfo.user);
	    connectionProps.put("password", dbInfo.password);
	    String jdbcUrl = "jdbc:mysql://{0}:{1}";
	    jdbcUrl = MessageFormat.format(jdbcUrl, dbInfo.host, dbInfo.port);
        try {
			conn = DriverManager.getConnection(jdbcUrl, connectionProps);
			QueryRunner runner = new QueryRunner();
			Long count = runner.query(conn, IS_DATABASE_EXISTS, new ScalarHandler<Long>(), dbInfo.database);
			if (count == 0) {
				this.createDatabase(conn, dbInfo);
			} else {
				conn.setCatalog(dbInfo.database);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	    return conn;
	}
	
	private void createDatabase(Connection conn, DatabaseInfo dbInfo) throws IOException, SQLException {
		QueryRunner runner = new QueryRunner();
		if (messageConfirm.confirm("Database " + dbInfo.database + " on " + dbInfo.host 
				+ ":" + dbInfo.port + " is not exists. Create it?" )) {
			runner.update(conn, CREATE_DATABASE.replace("?", dbInfo.database));
			conn.setCatalog(dbInfo.database);
			runner.update(conn, MigrateService.CREATE_MIGRATE_HISTORY_TABLE);
		} else {
			throw new RuntimeException("Database " + dbInfo.database + " on " + dbInfo.host 
				+ ":" + dbInfo.port + " is not exists");
		}
	}
	
	public MessageConfirm getMessageConfirm() {
		return messageConfirm;
	}

	public void setMessageConfirm(MessageConfirm messageConfirm) {
		this.messageConfirm = messageConfirm;
	}

	public ScriptRunner getScriptRunner() {
		return scriptRunner;
	}

	public void setScriptRunner(ScriptRunner scriptRunner) {
		this.scriptRunner = scriptRunner;
	}

	public static class MigrationTable {
		private String version;
		private Date apply_time;
		
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public Date getApply_time() {
			return apply_time;
		}
		public void setApply_time(Date apply_time) {
			this.apply_time = apply_time;
		}
	}
	
	private static final String CREATE_MIGRATE_HISTORY_TABLE = "CREATE TABLE tbl_migration " +
			"(version VARCHAR(200) NOT NULL PRIMARY KEY, " +
			"apply_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
	
	private static final String COUNT_MIGRATE_HISTORY_TABLE = "SELECT COUNT(1) " +
			"FROM information_schema.TABLES " +
			"WHERE table_schema = DATABASE() AND table_name = 'tbl_migration'";
	
	private static final String SELECT_MIGRATE_HISTORY_TABLE = "SELECT VERSION, apply_time " +
			"FROM tbl_migration ORDER BY VERSION DESC";
	
	private static final String RECORD_VERSION = "INSERT INTO tbl_migration VALUE (?, NULL)";
	
	private static final String IS_DATABASE_EXISTS = "SELECT COUNT(*) " +
			"FROM information_schema.TABLES WHERE table_schema = ?";
	
	private static final String CREATE_DATABASE = "CREATE DATABASE ?";
}
