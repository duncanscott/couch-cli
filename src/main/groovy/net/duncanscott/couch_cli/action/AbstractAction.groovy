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
import net.duncanscott.couch_cli.client.CouchClientErrorMessage
import net.duncanscott.couch_cli.util.CouchApi

@Log4j
abstract class AbstractAction {

	CouchClient couchClient
    CliBuilder cliBuilder
	OptionAccessor optionAccessor
	CouchApi api

    String actionName
    List<String> actionArgs = []

    abstract String getDescription()
    abstract int performAction()
    abstract void configureOptions()

    String getUsage() {
        "${couchClient.couchClientConfig.application.name} ${actionName} <subject couchdb> ${(cliBuilder?.options?.options)?' [<args>]':''}"
    }

    String getSubjectDatabaseName() {
        if (actionArgs) {
            return actionArgs[0]
        }
        return null
    }

	ConfigObject requireSubjectDatabase() {
		if (subjectDatabaseName) {
			return couchClient.requireConfiguration(subjectDatabaseName)
		}
		throw new CouchClientErrorMessage("no database defined for ${actionName}")
	}

    void message(String message) {
        CouchClient.message(message)
    }

    void error(String message) {
        CouchClient.error(message)
    }

	final int doPerformAction() {
        log.debug "performing action ${actionName} with args ${actionArgs}"
        cliBuilder = new CliBuilder(stopAtNonOption:false)
        configureOptions()
        cliBuilder.usage = getUsage()
        if (!actionArgs) {
            cliBuilder.usage()
            return CouchClient.FAILURE
        }
        optionAccessor = cliBuilder.parse(actionArgs)
        if (!optionAccessor) {
            return CouchClient.FAILURE
        }
        return performAction()
    }

}
