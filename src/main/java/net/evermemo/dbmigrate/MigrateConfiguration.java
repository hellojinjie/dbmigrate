package net.evermemo.dbmigrate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

public class MigrateConfiguration {

	private String migrationDirectory;
	private Map<String, DatabaseInfo> databases = new LinkedHashMap<String, DatabaseInfo>();
	private List<String> defaultDatabases = new ArrayList<String>();

	public MigrateConfiguration() {
		
	}
	
	public MigrateConfiguration(String configFilename) {
		this.loadConfiguration(configFilename);
	}
	
	public void loadConfiguration(String configFilename) {
		XMLConfiguration config = null;
		try {
			config = new XMLConfiguration(configFilename);
			config.setThrowExceptionOnMissing(true);
		} catch (ConfigurationException e) {
			throw new RuntimeException(e);
		}
		migrationDirectory = config.getString("migrations");

		List<HierarchicalConfiguration> fields = config
				.configurationsAt("databases.apply-to");
		for (HierarchicalConfiguration sub : fields) {
			String databaseAlias = sub.getString("[@name]");
			DatabaseInfo dbInfo = new DatabaseInfo();
			dbInfo.host = sub.getString("host");
			dbInfo.port = sub.getString("port", "3306");
			dbInfo.database = sub.getString("database");
			dbInfo.user = sub.getString("user");
			dbInfo.password = sub.getString("password");
			dbInfo.alias = databaseAlias;
			if (sub.getBoolean("[@default]", false)) {
				defaultDatabases.add(databaseAlias);
			}
			this.databases.put(databaseAlias, dbInfo);
		}
	}
	
	public String getMigrationDirectory() {
		return migrationDirectory;
	}

	public void setMigrationDirectory(String migrationDirectory) {
		this.migrationDirectory = migrationDirectory;
	}

	public Map<String, DatabaseInfo> getDatabases() {
		return databases;
	}

	public void setDatabases(Map<String, DatabaseInfo> databases) {
		this.databases = databases;
	}

	public List<String> getDefaultDatabases() {
		return defaultDatabases;
	}

	public void setDefaultDatabases(List<String> defaultDatabases) {
		this.defaultDatabases = defaultDatabases;
	}
}
