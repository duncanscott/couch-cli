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

import net.duncanscott.couch_cli.client.CouchCliBuilder
import net.duncanscott.couch_cli.client.CouchClient
import net.duncanscott.couch_cli.client.CouchClientErrorMessage
import net.duncanscott.couch_cli.client.CouchOption
import net.duncanscott.couch_cli.util.CouchApi

class BaseAction {
	
	CouchOption option
	CouchClient couchClient
	CouchCliBuilder cliBuilder
	OptionAccessor optionAccessor
	CouchApi api
	
	String getOptionArgument() {
		def arg = optionAccessor.getProperty(option.longOpt)
		if (arg) {
			return (String) arg
		}
		return null
	}
	
	String getActionName() {
		return option?.longOpt
	}
	
	String getSourceDatabaseName() {
		def databaseName = getOptionArgument()
		if (databaseName) {
			return databaseName.toString()
		}
		return null
	}
	
	String getFromDatabaseName() {
		def databaseName = optionAccessor.from
		if (databaseName) {
			return databaseName.toString()
		}
		return null
	}
	
	String getTargetDatabaseName() {
		def databaseName = optionAccessor.target
		if (databaseName) {
			return databaseName.toString()
		}
		return null
	}
	
	ConfigObject requireConfiguration(String configurationName) {
		ConfigObject config = couchClient.getConfiguration(configurationName)
		if (!config) {
			throw new CouchClientErrorMessage("unknown couchdb configuration \"${configurationName}\"")
		}
		if (!config.couchdb.url) {
			throw new CouchClientErrorMessage("configuration \"${configurationName}\" is invalid.  Parameter couchdb.url is missing.")
		}
		if (!config.couchdb.name) {
			throw new CouchClientErrorMessage("configuration \"${configurationName}\" is invalid.  Parameter couchdb.name is missing.")
		}
		return config
	}
	
	ConfigObject requireSourceDatabase() {
		String configName = getSourceDatabaseName()
		if (configName) {
			return requireConfiguration(configName)
		}
		throw new CouchClientErrorMessage("no database defined for ${actionName}.")
	}
	
	ConfigObject requireFromDatabase() {
		String configName = getFromDatabaseName()
		if (configName) {
			return requireConfiguration(configName)
		}
		throw new CouchClientErrorMessage("database to import from not defined.  Please identify database via the --from option.")
	}
	
	ConfigObject requireTargetDatabase() {
		String configName = getTargetDatabaseName()
		if (configName) {
			return requireConfiguration(configName)
		}
		throw new CouchClientErrorMessage("target database not defined.  Please identify the database to replicate to via the --target option.")
	}
	
	boolean getPush() {
		return optionAccessor.push
	}
	
	boolean getContinuous() {
		if (optionAccessor.continuous) {
			return true
		}
		return false
	}

	boolean getAll() {
		if (optionAccessor.all) {
			if (optionAccessor.name) {
				throw new CouchClientErrorMessage("incompatible options --all and --name.  Please select either all databases with --all or individual databases via --name.")
			}
			return true
		}
		return false
	}

	boolean getName() {
		if (optionAccessor.name) {
			boolean all = getAll() //throw error if both options are set
			return true
		}
		return false
	}
	
	List<String> getNames() {
		List<String> nameArgs = []
		if (name) { //error check implicit
			nameArgs += optionAccessor.names
		}
		return nameArgs
	}
	
	
	private List<String> extractDatabasesFromClientRequest(ConfigObject config, boolean ignoreErrors) {
		Set<String> namedDatabases = names.toSet()
		if (!namedDatabases && !all) {
			if (!ignoreErrors) {
				throw new CouchClientErrorMessage("no databases named.  Please identify one or more databases at couchdb ${config.couchdb.name} via either the --all or --name option.")
			}
		}
		List<String> allDatabases = api.getDatabaseList(config.couchdb.url)
		List<String> databases = null
		if (namedDatabases) {
			List<String> badNames = namedDatabases.minus(allDatabases).toList()
			if (badNames && !ignoreErrors) {
				throw new CouchClientErrorMessage("invalid database names ${badNames}")
			}
			databases = namedDatabases.intersect(allDatabases).toList()
		} else {
			//assert all
			databases = allDatabases
		}
		if (!databases && !ignoreErrors) {
			throw new CouchClientErrorMessage("no databases")
		}
		return databases
	}
	
	List<String> getDatabases(ConfigObject config) {
		return extractDatabasesFromClientRequest(config, true)
	}
	
	List<String> requireDatabases(ConfigObject config) {
		return extractDatabasesFromClientRequest(config, false)
	}
	
	List<String> requireTargetDatabases() {
		ConfigObject config = requireTargetDatabase()
		return requireDatabases(config)
	}
	
	List<String> requireFromDatabases() {
		ConfigObject config = requireFromDatabase()
		return requireDatabases(config)
	}
	
	List<String>  requireSourceDatabases() {
		ConfigObject config = requireSourceDatabases()
		return requireDatabases(config)
	}
	
	int performAction() {
		couchClient.error "no handler for action ${actionName} implemented"
		return couchClient.NOHANDLER
	}
	
	
}
