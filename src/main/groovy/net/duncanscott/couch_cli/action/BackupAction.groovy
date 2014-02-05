package net.duncanscott.couch_cli.action

/**
 * Created by Duncan on 2/2/14.

 Project: couch-cli

 couch-cli command line interface to Apache CouchDb databases
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


 Duncan Scott
 PostalAnnex
 785 E2 Oak Grove Road #229
 Concord, CA 94518-3617-US

 email:duncan@duncanscott.net
 */

import groovy.util.logging.Log4j
import net.duncanscott.couch_cli.client.CouchClient
import net.duncanscott.couch_cli.option.ContinuousOption
import net.duncanscott.couch_cli.option.NamedAndAllDatabasesOption
import net.duncanscott.couch_cli.option.PushOption
import net.duncanscott.couch_cli.option.TagOption
import net.duncanscott.couch_cli.option.TargetDatabaseOption
import net.duncanscott.couch_cli.util.BackupNameElements

@Log4j
class BackupAction extends AbstractAction {

    TargetDatabaseOption targetDatabaseOption
    ContinuousOption continuousOption
    PushOption pushOption
    NamedAndAllDatabasesOption databasesOption
    TagOption tagOption

    @Override
    String getDescription() {
        'Backup databases to --target with system generated prefix.  Databases must be identified via --name or --all.  Flags: --continuous, --push.'
    }

    void configureOptions() {
        targetDatabaseOption = new TargetDatabaseOption(this)
        continuousOption = new ContinuousOption(this)
        pushOption = new PushOption(this)
        databasesOption = new NamedAndAllDatabasesOption(this)
        tagOption = new TagOption(this)
    }

    @Override
	public int performAction() {
        ConfigObject sourceConfig = requireSubjectDatabase()
        ConfigObject targetConfig = targetDatabaseOption.requireTargetDatabase()
        boolean continuous = continuousOption.continuous
        boolean push = pushOption.push
        Set<String> tags = tagOption.tags
        log.debug "tags: ${tags}"

        Set<String> databases = []
        databasesOption.requireDatabases(sourceConfig).each { String name ->
            if ((name.startsWith('_') || name.startsWith(BackupNameElements.BACKUP)) && !(databasesOption.getNamedDatabases(sourceConfig).contains(name))) {
                message "skipping ${name}"
            } else {
                databases << name
            }
        }

		int returnCode = CouchClient.SUCCESS
		databases.each {String database ->
            BackupNameElements bne = new BackupNameElements()
            bne.couchDbName = sourceConfig.couchdb.name
            bne.databaseName = database
            bne.continuousBackup = continuous
            bne.tags = tags

            String backupName = bne.toBackupName()

            String replicationId = push ?
			    api.startPushReplication(sourceConfig.couchdb.url, database, targetConfig.couchdb.url, backupName, continuous) :
				api.startPullReplication(sourceConfig.couchdb.url, database, targetConfig.couchdb.url, backupName, continuous) ;

			if (replicationId) {
				message "${replicationId} started ${continuous?'continuous ':''}${push?'push':'pull'} replication of ${sourceConfig.couchdb.name}.${database} to ${targetConfig.couchdb.name}.${backupName}"
			} else {
				returnCode = CouchClient.FAILURE
				error "FAILED to start ${continuous?'continuous ':''}${push?'push':'pull'} replication of ${sourceConfig.couchdb.name}.${database} to ${targetConfig.couchdb.name}.${backupName}"
			}
		}
		return returnCode
	}
	
}
