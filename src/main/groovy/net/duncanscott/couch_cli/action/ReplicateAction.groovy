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
import net.duncanscott.couch_cli.option.ContinuousOption
import net.duncanscott.couch_cli.option.NamedAndAllDatabasesOption
import net.duncanscott.couch_cli.option.PushOption
import net.duncanscott.couch_cli.option.TargetDatabaseOption

@Log4j
class ReplicateAction extends AbstractAction {

    TargetDatabaseOption targetDatabaseOption
    ContinuousOption continuousOption
    PushOption pushOption
    NamedAndAllDatabasesOption databasesOption

    @Override
    String getDescription() {
        'Replicate specified database to --target (flags: --continuous, --push).'
    }

    @Override
    void configureOptions() {
        targetDatabaseOption = new TargetDatabaseOption(this)
        continuousOption = new ContinuousOption(this)
        pushOption = new PushOption(this)
        databasesOption = new NamedAndAllDatabasesOption(this)
    }

    @Override
	public int performAction() {
		ConfigObject sourceConfig = requireSubjectDatabase()
		ConfigObject targetConfig = targetDatabaseOption.requireTargetDatabase()
		boolean continuous = continuousOption.continuous
		boolean push = pushOption.push

        Set<String> databases = []
        databasesOption.requireDatabases(sourceConfig).each { String name ->
            if (name.startsWith('_') && !(databasesOption.getNamedDatabases(sourceConfig).contains(name))) {
                message "skipping ${name}"
            } else {
                databases << name
            }
        }

		databases.each {String database ->
			log.info "starting ${push?'push':'pull'} replication of database ${database} from ${sourceConfig.couchdb.name} to ${targetConfig.couchdb.name}"
			if(push) {
				String replicationId = api.startPushReplication(sourceConfig.couchdb.url, database, targetConfig.couchdb.url, database, continuous)
				message "${replicationId} started ${continuous?'continuous ':''}push replication of ${sourceConfig.couchdb.name}.${database} to ${targetConfig.couchdb.name}.${database}"
			} else {
				String replicationId = api.startPullReplication(sourceConfig.couchdb.url, database, targetConfig.couchdb.url, database, continuous)
				message "${replicationId} started ${continuous?'continuous ':''}pull replication of ${sourceConfig.couchdb.name}.${database} to ${targetConfig.couchdb.name}.${database}"
			}
		}

		return couchClient.SUCCESS
	}
	
}
