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
import net.duncanscott.couch_cli.option.NamedAndAllDatabasesOption
import net.duncanscott.couch_cli.util.CouchApi

@Log4j
class DeleteDatabaseAction extends AbstractAction {

    NamedAndAllDatabasesOption databasesOption

    @Override
    String getDescription() {
        'Delete databases at the specified couchdb.  Must specify individual databases via --name or --all.  CAREFULL WITH THIS ONE.'
    }

    void configureOptions() {
        databasesOption = new NamedAndAllDatabasesOption(this)
    }

	void restoreReplicatorDatabase(ConfigObject couchDb) {
		api.createDatabase(couchDb.couchdb.url, CouchApi.REPLICATOR)
		message "restored database ${couchDb.couchdb.name}.${CouchApi.REPLICATOR}"
	}
	
	void deleteReplicatorDatabase(ConfigObject couchDb) {
		message "deleting database ${couchDb.couchdb.name}.${CouchApi.REPLICATOR}"
		api.deleteDatabase(couchDb.couchdb.url, CouchApi.REPLICATOR)
	}
	
	void deleteDatabases(ConfigObject couchDb, Collection<String> databases) {
		databases?.each { String database ->
			if (database && !database.startsWith('_')) {
				message "deleting database ${couchDb.couchdb.name}.${database}"
				api.deleteDatabase(couchDb.couchdb.url, database)
			}
		}
	}

	@Override
	int performAction() {
		ConfigObject couchDb = requireSubjectDatabase()
		CouchClient.checkDelete(couchDb)
		try {
			Set<String> databases = databasesOption.getDatabases(couchDb)
			if (databases.contains(CouchApi.REPLICATOR)) {
				deleteReplicatorDatabase(couchDb)
			}
			databases.findAll{ it.startsWith('_') && it != CouchApi.REPLICATOR}.each { String database ->
				message "skipping database ${couchDb.couchdb.name}.${database}"
			}
			deleteDatabases(couchDb, databases)
		} finally {
			try {
				restoreReplicatorDatabase(couchDb)
			} catch (Throwable t) {
				log.error "restoration of replicator failed", t
			}
		}
		return CouchClient.SUCCESS
	}
	
}
