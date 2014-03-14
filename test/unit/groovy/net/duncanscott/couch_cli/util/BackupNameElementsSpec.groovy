package net.duncanscott.couch_cli.util

import org.apache.log4j.Logger
import spock.lang.Specification

/**
 * Created by Duncan on 1/30/14.
 */
class BackupNameElementsSpec extends Specification {

    Logger logger = Logger.getLogger(BackupNameElementsSpec.class.name)

    def "backup names test"() {
        setup:
            BackupNameElements bne = new BackupNameElements()
            bne.backupStart = new Date()
            bne.tags = ['tag1','tag2']
            bne.databaseName = 'critical_data'
            bne.couchDbName = 'jovial'
            String backupName = bne.toBackupName()
            BackupNameElements deconstructedBackupName = BackupNameElements.deconstructBackupName(backupName)
        expect:
            logger.info "backup name: \"${backupName}\""
            deconstructedBackupName
            deconstructedBackupName.databaseName ==  bne.databaseName
            deconstructedBackupName.couchDbName == bne.couchDbName
            deconstructedBackupName.tags.sort() == bne.tags.sort()
            deconstructedBackupName.continuousBackup == Boolean.FALSE
            deconstructedBackupName.backupStart == bne.backupStart
    }
}
