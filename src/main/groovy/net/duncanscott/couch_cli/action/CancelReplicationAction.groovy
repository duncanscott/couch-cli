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
import net.duncanscott.couch_cli.util.CouchApi

class CancelReplicationAction extends AbstractAction {

    @Override
    String getDescription() {
        'Cancel all replication at specified couchdb (by deleting replication records in the _replicator database).'
    }

    @Override
    void configureOptions() {}

    static void cancelReplication(ConfigObject config, CouchApi api) {
        CouchClient.checkCancelReplication(config)
        boolean includeDocs = false
        int pageSize = 1

        api.doForAllRecords(config.couchdb.url, CouchApi.REPLICATOR, includeDocs, pageSize) { json ->
            CouchClient.message "canceling ${config.couchdb.name}.${net.duncanscott.couch_cli.util.CouchApi.REPLICATOR}/${json.id}"
            api.deleteRecord(config.couchdb.url, CouchApi.REPLICATOR, json.id, json.'value'.'rev')
        }
    }

    @Override
    int performAction() {
        ConfigObject config = requireSubjectDatabase()
        cancelReplication(config,api)
        return CouchClient.SUCCESS
    }


}
