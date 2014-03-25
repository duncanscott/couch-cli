package net.duncanscott.couch_cli.util

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

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by Duncan on 1/30/14.
 */
class BackupNameElements {

    static final String BACKUP = 'backup'
    static final String CONTINUOUS = 'continuous'
    static final String backupNameRegex = '^' + BACKUP + '_([^_]+)_([^_]+)_([^_]*)_(.+)$'

    final SimpleDateFormat timestampFormat = new SimpleDateFormat('yyyyMMddHHmmssSSS')

    String couchDbName
    String databaseName
    Boolean continuousBackup = Boolean.FALSE
    Date backupStart
    Set<String> tags = []

    static BackupNameElements deconstructBackupName(String backupName) {
        Pattern backupNamePattern = Pattern.compile(backupNameRegex)
        Matcher m = backupNamePattern.matcher(backupName)
        if (m) {
            BackupNameElements bne = new BackupNameElements()
            List<String> matched =  m[0][1..-1]
            bne.couchDbName = matched[0]
            String timestampString = matched[1]
            if (CONTINUOUS == timestampString) {
                bne.continuousBackup = Boolean.TRUE
            } else {
                bne.backupStart = bne.timestampFormat.parse(timestampString)
            }
            matched[2]?.split('-')?.each { bne.tags << it }
            bne.databaseName = matched[3]
            return bne
        }
        return null
    }

    String toBackupName() {
        String timestamp = continuousBackup ? CONTINUOUS : timestampFormat.format(backupStart?:new Date())
        "${BACKUP}_${couchDbName}_${timestamp}_${tags?.join('-')}_${databaseName}"
    }

}
