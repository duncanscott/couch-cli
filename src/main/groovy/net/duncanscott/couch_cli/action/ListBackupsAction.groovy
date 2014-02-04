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
import net.duncanscott.couch_cli.client.CouchClientErrorMessage
import net.duncanscott.couch_cli.option.AllOption
import net.duncanscott.couch_cli.option.NoTagOption
import net.duncanscott.couch_cli.option.OlderThanOption
import net.duncanscott.couch_cli.option.TagOption
import net.duncanscott.couch_cli.util.BackupNameElements

class ListBackupsAction extends AbstractAction {

    OlderThanOption olderThanOption
    TagOption tagOption
    AllOption allOption
    NoTagOption noTagOption

    @Override
    String getDescription() {
        'List backup databases at the specified couchdb.  Specify databases to include via the --tag, --olderThan, or --all options.'
    }

    @Override
    void configureOptions() {
        tagOption = new TagOption(this)
        olderThanOption = new OlderThanOption(this)
        allOption = new AllOption(this)
        noTagOption = new NoTagOption(this)
    }

    Set getBackupDatabases() {
        Set<String> databases = []
        ConfigObject config = requireSubjectDatabase()
        Set tags = tagOption.tags
        Set noTags = noTagOption.noTags
        Integer days = olderThanOption.olderThan
        Date now = new Date()
        boolean all = allOption.all
        if (!(all || tags || days != null)) {
            throw new CouchClientErrorMessage('please specify backup databases to include via the --tag, --olderThan, or --all options.')
        }
        if (all && (tags || days != null)) {
            throw new CouchClientErrorMessage('--all is incompatible with --tag and --olderThan.')
        }
        api.getDatabaseList(config.couchdb.url).findAll{it.startsWith(BackupNameElements.BACKUP)}.each { String databaseName ->
            BackupNameElements bne = BackupNameElements.deconstructBackupName(databaseName)
            if(bne) {
                if (tags) {
                    if(tags.find{String tag -> !bne.tags.contains(tag)}) {
                        return //backup does not contain all specified tags, skip
                    }
                }
                if (days != null) {
                    if (!bne.backupStart) {
                        return //skip backup
                    }
                    Calendar c = Calendar.getInstance();
                    c.setTime(bne.backupStart)
                    c.add(Calendar.DATE, days)
                    if (c.getTime() > now) {
                        return //skip, not old enough
                    }
                }
                if (noTags) {
                    if (bne.tags.find{noTags.contains(it)}) {
                        return //skip, backup tag matches noTag
                    }
                }
                if (all || tags || days != null) {
                    databases << databaseName //backup selected
                }
            }
        }
        return databases
    }

    @Override
	int performAction() {
        backupDatabases.each {
            message it
        }
		return CouchClient.SUCCESS
	}
	
}
