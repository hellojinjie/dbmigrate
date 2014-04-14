package net.evermemo.dbmigrate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author hellojinjie
 * @date 2013-4-11
 */
public class Main {

	private static final Log log = LogFactory.getLog(Main.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		log.info("");
		log.info("DB Migration Tool V1.0");
		log.info("");
		
		Main main = new Main();
		main.run(args);
		
	}

	private void run(String[] args) {
		Options options = this.createOptions();
		
		String subCommand = null;
		if (args.length < 1) {
			this.printHelp(options);
		} 
		
		CommandLineParser commandParser = new BasicParser();
		CommandLine commandLine = null;
		try {
			String[] opts = new String[args.length - 1];
			/* array shift */
			System.arraycopy(args, 1, opts, 0, args.length - 1);
			commandLine = commandParser.parse(options, opts, false);
		} catch (ParseException e) {
			log.error(e.getMessage());
			this.printHelp(options);
		}
		
		subCommand = args[0];
		this.dispatchCommand(options, commandLine, subCommand);
	}
	
	private void dispatchCommand(Options options, CommandLine commandLine, String subCommand) {

		MigrateConfiguration config = new MigrateConfiguration("configure.xml");
		MigrateService migrateSevice = new MigrateService(config);
		
		if ("help".equalsIgnoreCase(subCommand)) {
			this.printHelp(options);
			
		} else if ("apply".equalsIgnoreCase(subCommand)) {
			migrateSevice.actionApply(commandLine.getOptionValue("d", null));
			
		} else if ("create".equalsIgnoreCase(subCommand)) {
			String patchName = this.clearWords(StringUtils.join(commandLine.getArgs(), "_"));
			if (patchName == null || "".equals(patchName)) {
				log.error("Error creating migration. Please give the migration name");
				System.exit(0);
			}
			migrateSevice.actionCreate(patchName,
					commandLine.getOptionValue("branch", null));
			
		} else if ("new".equalsIgnoreCase(subCommand)) {
			migrateSevice.actionNew(commandLine.getOptionValue("d", null));
			
		} else if ("history".equalsIgnoreCase(subCommand)) {
			migrateSevice.actionHistory(commandLine.getOptionValue("d", null));
			
		} else if ("mark".equalsIgnoreCase(subCommand)) {
			String version = commandLine.getOptionValue("v", null);
			if (version == null || "".equals(version)) {
				log.error("Error marking migration. Please give the migration name");
				System.exit(0);
			}
			migrateSevice.actionMark(commandLine.getOptionValue("d", null), version);
			
		} else {
			this.printHelp(options);
		}
	}
	
	/**
	 * Print help and exit.
	 * @param options
	 */
	private void printHelp(Options options) {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp("dbmigrate.{sh,bat} [apply|create|help|history|new|mark] [options]", "options:", options, "");
		System.exit(0);
	}

	private Options createOptions() {
		Options options = new Options();

		options.addOption("d", "database", true, "Which database to apply the SQL patch, " +
				"seperated by comma, all for all the databases");
		options.addOption("b", "branch", true, "Create a new branch for the SQL patch");
		options.addOption("v", "version", true,"Which version to mark, " +
				"seperated by comma, all for all the versions");
		options.addOption("h", "help", false,"Print this help message");

		return options;
	}
	
	private String clearWords(String words) {
		String result = null;
		
		Pattern pattern = Pattern.compile("[^a-zA-Z0-9]+");
		Matcher matcher = pattern.matcher(words);
		result = matcher.replaceAll("_");
		return result;
	}
}
