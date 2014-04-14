# Database Migration Tool

## System Requirement
    1. Java 1.6+
    2. MySQL Client 5.5+
    
## Configuration
    1. rename configure.xml-template to configure.xml
    2. modify configure.xml to suit your own needs
    3. for legacy database, we must first initialize the database: 
        make sure the status of your database table structure and procedure are at the base line, then execute the 2 SQL:
            CREATE TABLE tbl_migration (version VARCHAR(200) NOT NULL PRIMARY KEY, apply_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
            INSERT INTO tbl_migration VALUE ('m000000_000000_base.sql', NULL)
    

## Usage
    d:\tools\dbmigrate>dbmigrate.bat

    DB Migration Tool V0.1

    usage: dbmigrate.{sh,bat} [apply|create|help|history|new|mark] [options]
    options:
     -d,--database <arg>   Which database to apply the SQL patch, seperated by
                           comma, all for all the databases
     -h,--help             Print this help message
     -v,--version <arg>    Which version to mark, seperated by comma, all for
                           all the versions

The following steps show how we can use database migration during development:

    Tim creates a new migration (e.g. create a new table)
    Tim commits the new migration into source control system (e.g. SVN, GIT)
    Tim sends all team members an email to tell them I have created a new migration
    Doug updates from source control system and receives the new migration
    Doug applies the migration to his local development database

In the following, we will describe how to use this tool.

1. Creating Migrations
To create a new migration (e.g. create a news table), we run the following command:
    dbmigrate.bat create <name>
The required name parameter specifies a very brief description of the migration (e.g. create_news_table).
    dbmigrate.bat create create_news_table
The above command will create under the ./migrations directory a new file named m101129_185401_create_news_table.sql which is a blank file.
You should fill the blank file with standard SQL code to include the actual database migration. The SQL code should compatible with the MySQL command-line tool.
Notice that the file name is of the pattern m<timestamp>_<name>, where <timestamp> refers to the UTC timestamp (in the format of yymmdd_hhmmss) when the migration is created, and <name> is taken from the command's name parameter.

2. Applying Migrations 
To apply all available new migrations (i.e., make the local database up-to-date), run the following command:
    dbmigrate apply
The command will show the list of all new migrations. If you confirm to apply the migrations, it will run the run the SQL code in MySQL command-line tool, one after another, in the order of the timestamp value in the file name.
After applying a migration, the migration tool will keep a record in a database table named tbl_migration. This allows the tool to identify which migrations have been applied and which are not. If the tbl_migration table does not exist, the migrations will been abort. you need to create the tbl_migration table manually.

3. Showing Migration Information 
Besides applying and create migrations, the migration tool can also display the migration history and the new migrations to be applied.
    dbmigrate history 
    dbmigrate new 
The first command shows the migrations that have been applied, while the second command shows the migrations that have not been applied.

4. Modifying Migration History 
Sometimes, we may want to modify the migration history to a specific migration version without actually applying the relevant migrations. This often happens when developing a new migration. We can use the following command to achieve this goal.
    dbmigrate mark -v 101129_185401
This command is very similar to `dbmigrate apply` to command, except that it only modifies the migration history table to the specified version without applying the migrations.



