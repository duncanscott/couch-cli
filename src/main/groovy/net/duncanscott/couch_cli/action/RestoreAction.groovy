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

import net.duncanscott.couch_cli.client.CouchClient
import net.duncanscott.couch_cli.option.FromDatabaseOption
import net.duncanscott.couch_cli.option.NamedDatabasesOption
import net.duncanscott.couch_cli.option.PushOption
import net.duncanscott.couch_cli.util.BackupNameElements

class RestoreAction extends AbstractAction {

    FromDatabaseOption fromDatabaseOption
    NamedDatabasesOption databasesOption
    PushOption pushOption

    @Override
    String getDescription() {
        'Restore the specified couchdb --from a backup.  Each database to restore must be identified via --name (--all is invalid). Flags: --push.'
    }

    void configureOptions() {
        fromDatabaseOption = new FromDatabaseOption(this)
        databasesOption = new NamedDatabasesOption(this)
        pushOption = new PushOption(this)
    }

    @Override
	public int performAction() {
        ConfigObject sourceConfig = requireSubjectDatabase()
        ConfigObject fromConfig = fromDatabaseOption.requireFromDatabase()
        boolean push = pushOption.push
        boolean continuous = false

		int returnCode = CouchClient.SUCCESS
		databasesOption.requireNamedDatabases(fromConfig).each {String backupName ->
            BackupNameElements bne = BackupNameElements.deconstructBackupName(backupName)
			if (!bne) {
				error "invalid backup database name \"${backupName}\""
				return
			}
			//message "starting ${push?'push':'pull'} restore of ${sourceConfig.couchdb.name}.${bne.databaseName} from ${fromConfig.couchdb.name}.${backupName}"
			String replicationId = push ?
				api.startPushReplication(fromConfig.couchdb.url, backupName, sourceConfig.couchdb.url, bne.databaseName, continuous) :
				api.startPullReplication(fromConfig.couchdb.url, backupName, sourceConfig.couchdb.url, bne.databaseName, continuous) ;

			if (replicationId) {
				message "${replicationId} started ${push?'push':'pull'} replication of ${fromConfig.couchdb.name}.${backupName} to ${sourceConfig.couchdb.name}.${bne.databaseName}"
			} else {
				returnCode = CouchClient.FAILURE
				error "FAILED to start ${push?'push':'pull'} replication of ${fromConfig.couchdb.name}.${backupName} to ${sourceConfig.couchdb.name}.${bne.databaseName}"
			}
		}
		return returnCode
	}
	
}
