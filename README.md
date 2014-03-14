#couch-cli

Command line interface to facilitate CouchDB replication and other administrative tasks.  The gradle build generates both Unix and Windows executables.  Java
5+ is required.  (Tested on Java 6 and 7.)

##Build Locally
To install locally, you may cd to the project directory and type:

    gradle wrapper
    gradle installApp

##Sample Commands

    $ couch-cli list localhost
    _replicator
    _users

    $ couch-cli replicate production --name samples-submission --target localhost --continuous
    5c130b6f1d51a938cdf6eb2c7d00923e started continuous pull replication of production.samples-submission to localhost.samples-submission

    $ couch-cli list localhost
    _replicator
    _users
    samples-submission

    $ couch-cli backup localhost --name samples-submission --target backup --tag daily --tag test
    1274a0e175c373afa0a7123b33000e91 started pull replication of localhost.samples-submission to backup.backup_localhost_20140202193614528_test-daily_samples-submission

    $ couch-cli listBackups backup --tag daily
    backup_localhost_20140202193614528_test-daily_samples-submission

    $ couch-cli listRecords backup --name backup_localhost_20140202193614528_test-daily_samples-submission | wc -l
        113

    $ couch-cli listRecords localhost --name samples-submission | wc -l
        113

    $ couch-cli delete localhost --name samples-submission
    deleting database localhost.samples-submission

    $ couch-cli restore localhost --from backup --name backup_localhost_20140202193614528_test-daily_samples-submission
    5c130b6f1d51a938cdf6eb2c7d00978c started pull replication of backup.backup_localhost_20140202193614528_test-daily_samples-submission to localhost.samples-submission

    $ couch-cli listRecords localhost --name samples-submission | wc -l
        113

##Usage

    usage: couch-cli [<action>] [<subject couchdb>] [<args>]
     -h,--help      show usage information
     -v,--version   version of this client

    Actions are:

       backup              Backup databases to --target with system generated prefix.  Databases must be identified via --name or --all.  Flags: --continuous, --push.
       cancelReplication   Cancel all replication at specified couchdb (by deleting replication records in the _replicator database).
       delete              Delete databases at the specified couchdb.  Must specify individual databases via --name or --all.  CAREFULL WITH THIS ONE.
       deleteBackups       Delete backup databases at the specified couchdb.  Specify databases to include via the --tag, --olderThan, or --all options.
       deleteRecords       Delete records in databases at the specified couchdb.  Must specify individual databases via --name or --all.  CAREFULL.
       dumpRecords         Output JSON for all records in databases at the specified couchdb.  Must specify individual databases via --name.
       list                List all databases at specified couchdb
       listBackups         List backup databases at the specified couchdb.  Specify databases to include via the --tag, --olderThan, or --all options.
       listRecords         List all records in databases at the specified couchdb.  Must specify individual databases via --name or --all.
       replicate           Replicate databases to --target.  Identify databases via --name or --all.  Flags: --continuous, --push.
       restore             Restore the specified couchdb --from a backup.  Each database to restore must be identified via --name (--all is invalid). Flags: --push.

To use the tool, you should specify a path to a configuration file via environment property COUCHCLI_CONFIG.  This file identifies the CouchDB instances the tool
can work with and the operations that are permitted on each.  A sample configuration file is shown below.

Note that cancelReplication, delete, and deleteRecords actions must be enabled in the configuration file to allow the tool to perform these actions
on a particular CouchDB.

    $ env | grep COUCH
    COUCHCLI_CONFIG=/c/Users/Duncan/.couch-cli/CouchDatabasesConfig.groovy

    $ cat $COUCHCLI_CONFIG
    environments {
            localhost {
                    couchdb.url = 'http://localhost:5984/'
                    couchdb.deleteDatabase = true
                    couchdb.deleteRecords = true
            }
            development {
                    couchdb.url = 'http://development.server.org:5984/'
                    couchdb.deleteRecords = true
            }
            integration {
                    couchdb.url = 'http://integration.server.org:5984/'
                    couchdb.deleteRecords = true
            }
            production {
                    couchdb.url = 'http://production.server.org:5984/'
                    couchdb.deleteRecords = false
            }
            backup {
                    couchdb.url = 'http://backup.server.org:5984/'
                    couchdb.deleteDatabase = true
                    couchdb.deleteRecords = false
                    couchdb.cancelReplication = true
            }
    }


A useful feature is the ability to tag backups and to retrieve them by tag and date.  This facilitates automated creation of point-in-time backups
and cleanup of old backups by cron job.  Sample cron entries:

    #restart continuous backup of all production couchdb databases nightly (replication is continuous but new databases should be added)
    15 2 * * *  source /opt/tomcat/.env; /opt/tomcat/tools/couch-cli/bin/couch-cli replicate production --target backup --all --continuous

    #weekly point-in-time backup
    10 3 * * 0  source /opt/tomcat/.env; /opt/tomcat/tools/couch-cli/bin/couch-cli backup production --target backup --all --tag weekly

    #daily point-in-time backup
    10 3 * * 2-6  source /opt/tomcat/.env; /opt/tomcat/tools/couch-cli/bin/couch-cli backup production --target backup --all --tag daily

    #delete daily backups older than 5 days
    10 5 * * 2-6  source /opt/tomcat/.env; /opt/tomcat/tools/couch-cli/bin/couch-cli deleteBackups backup --tag daily --olderThan 5

    #delete all backups older than 30 days
    30 0 * * *  source /opt/tomcat/.env; /opt/tomcat/tools/couch-cli/bin/couch-cli deleteBackups backup --olderThan 30

Note, the --olderThan option includes only databases older than the specified number of days.  A --noTag option is also available to screen out
backups containing the specified tags.


##Notes

New features can easily be added by adding subclasses of AbstractAction.  New action subclasses must be registered
in the actions map of CouchClient.

The logging level is currently set to debug.  The log file will include URLs that might include passwords.  If the logging level is increased
to block the output from HTTPBuilder, URLs (and passwords) may still be logged in the case of errors.

##Issues

The code uses the Groovy HTTPBuilder to interact with CouchDB databases.  A known issue with  HTTPBuilder (due to a problem in the Java URL class) prevents it from properly
encoding URLs that contain a '+' character.  CouchDB allows database names that include the '+' character, but couch-cli will choke
on such database names.

Some replication behavior is unintuitive.  It is important to understand how CouchDB replication works.
If a local version of a database is ahead of another, the other database will not be replicated to the local database.  See the following:

    $ couch-cli list localhost
    _replicator
    _users
    samples-submission

    $ couch-cli delete localhost --name samples-submission
    deleting database localhost.samples-submission

    $ couch-cli list localhost
    _replicator
    _users

    $ couch-cli replicate production --name samples-submission --target localhost --continuous
    5c130b6f1d51a938cdf6eb2c7d008aab started continuous pull replication of production.samples-submission to localhost.samples-submission

    $ couch-cli list localhost
    _replicator
    _users

    $ couch-cli cancelReplication localhost
    canceling localhost._replicator/5c130b6f1d51a938cdf6eb2c7d0063a5
    canceling localhost._replicator/5c130b6f1d51a938cdf6eb2c7d007214
    canceling localhost._replicator/5c130b6f1d51a938cdf6eb2c7d007732
    canceling localhost._replicator/5c130b6f1d51a938cdf6eb2c7d00812b
    canceling localhost._replicator/5c130b6f1d51a938cdf6eb2c7d008aab

    $ couch-cli listRecords localhost --name _replicator

    $ couch-cli replicate production --name samples-submission --target localhost --continuous
    5c130b6f1d51a938cdf6eb2c7d00923e started continuous pull replication of production.samples-submission to localhost.samples-submission

    $ couch-cli listRecords localhost --name _replicator
    localhost._replicator/5c130b6f1d51a938cdf6eb2c7d00923e

    $ couch-cli list localhost
    _replicator
    _users
    samples-submission

    $ couch-cli backup localhost --name samples-submission --target backup --tag daily --tag test
    1274a0e175c373afa0a7123b33000e91 started pull replication of localhost.samples-submission to backup.backup_localhost_20140202193614528_test-daily_samples-submission

    $ couch-cli listBackups backup --tag daily
    backup_localhost_20140202193614528_test-daily_samples-submission

    $ couch-cli listRecords backup --name backup_localhost_20140202193614528_test-daily_samples-submission | wc -l
        113

    $ couch-cli listRecords localhost --name samples-submission | wc -l
        113

    $ couch-cli deleteRecords localhost --name samples-submission | wc -l
        113

    $ couch-cli listRecords localhost --name samples-submission | wc -l
          0

    $ couch-cli restore localhost --from backup --name backup_localhost_20140202193614528_test-daily_samples-submission
    5c130b6f1d51a938cdf6eb2c7d00974d started pull replication of backup.backup_localhost_20140202193614528_test-daily_samples-submission to localhost.samples-submission

    $ couch-cli listRecords localhost --name samples-submission | wc -l
          0

    $ couch-cli delete localhost --name samples-submission
    deleting database localhost.samples-submission

    $ couch-cli restore localhost --from backup --name backup_localhost_20140202193614528_test-daily_samples-submission
    5c130b6f1d51a938cdf6eb2c7d00978c started pull replication of backup.backup_localhost_20140202193614528_test-daily_samples-submission to localhost.samples-submission

    $ couch-cli listRecords localhost --name samples-submission | wc -l
        113

## License

 Copyright (C) 2014  Duncan Scott

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
