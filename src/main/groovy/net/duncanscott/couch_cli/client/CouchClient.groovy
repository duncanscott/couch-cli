package net.duncanscott.couch_cli.client

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

import net.duncanscott.couch_cli.action.*
import net.duncanscott.couch_cli.util.CouchApi
import net.duncanscott.couch_cli.util.OnDemandCache
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.StackTraceUtils

class CouchClient {
	
	static final Logger logger = Logger.getLogger(CouchClient.class.name)

	static final int SUCCESS = 0
	static final int FAILURE = 1
	static final int NOHANDLER = 2

	OnDemandCache<CouchCliBuilder> cachedCliBuilder = new OnDemandCache<CouchCliBuilder>()
	OnDemandCache<ConfigObject> cachedCouchClientConfig = new OnDemandCache<ConfigObject>()
	OnDemandCache<URL> cachedCouchDbConfigurationUrl = new OnDemandCache<URL>()

    static Map<String, Class<? extends AbstractAction>> actions = [
        'backup':BackupAction,
        'cancelReplication':CancelReplicationAction,
        'deleteBackups':DeleteBackupsAction,
        'delete':DeleteDatabaseAction,
        'deleteRecords':DeleteRecordsAction,
        'dumpRecords':DumpRecordsAction,
        'list':ListAction,
        'listBackups':ListBackupsAction,
        'listRecords':ListRecordsAction,
        'replicate':ReplicateAction,
        'restore':RestoreAction
    ]

    static List<String> getActionNames() {
        return actions.keySet().sort()
    }

    static Collection<String> getActionDescriptions() {
        int maxLength = actionNames*.size().max()
        actionNames.collect { String action ->
            Class<? extends AbstractAction> actionClass = actions[action]
            "${action}" + (' ' * (maxLength - action.size())) + "   ${actionClass.newInstance().description}"
        }
    }

    String getUsage() {
        "${couchClientConfig.application.name} [<action>] [<subject couchdb>] [<args>]"
    }

    CouchCliBuilder getCliBuilder() {
		return cachedCliBuilder.fetch {
			CliBuilder cli = new CouchCliBuilder(stopAtNonOption:false)
            cli.usage = getUsage()
			return cli
		}
	}

	ConfigObject getCouchClientConfig() {
		return cachedCouchClientConfig.fetch {
			String configPath = 'config/CouchClientConfig.groovy'
			URL url = this.class.getClassLoader().getResource(configPath)
			if (!url) {
				configPath = 'src/main/resources/config/CouchClientConfig.groovy'
				url = new File(configPath).toURI().toURL()
			}
			if (!url) {
				throw new RuntimeException('unable to locate CouchClientConfig.groovy')
			}
			logger.debug "using couch client configuration: ${configPath}"
			return new ConfigSlurper().parse(url)
		}
	}
	
	URL getCouchDbConfigurationUrl() {
		return cachedCouchDbConfigurationUrl.fetch {
			URL url
			String configPath = System.getenv('COUCHCLI_CONFIG')
			if (configPath) {
				logger.debug "path to databases config defined by environment variable"
				url = new File(configPath).toURI().toURL()
			} else {
				logger.debug "using path to internal databases config"
				configPath = 'config/CouchDatabasesConfig.groovy'
				url = this.class.getClassLoader().getResource(configPath)
			}
			if (!url) {
				configPath = 'src/main/resources/config/CouchDatabasesConfig.groovy'
				url = new File(configPath).toURI().toURL()
			}
			logger.debug "using couch database configuration: ${configPath}"
			return url
		}
	}
	
	ConfigObject getConfiguration(String configurationName) {
		ConfigObject config = new ConfigSlurper(configurationName).parse(couchDbConfigurationUrl)
		if (config != null && !config.couchdb.name) {
			config.couchdb.name = configurationName
		}
		return config
	}

    ConfigObject requireConfiguration(String configurationName) {
        ConfigObject config = getConfiguration(configurationName)
        if (!config || !config.couchdb.url) {
            throw new CouchClientErrorMessage("invalid couchdb configuration \"${configurationName}\"")
        }
        return config
    }

    static void checkDelete(ConfigObject couchDb) {
        if (!couchDb.couchdb.deleteDatabase) {
            throw new CouchClientErrorMessage("delete not enabled for couchdb ${couchDb.couchdb.name}.  Configuration parameter \"couchdb.deleteDatabase\" must be set to true to enable delete.")
        }
    }

    static void checkDeleteRecords(ConfigObject couchDb) {
        if (!couchDb.couchdb.deleteRecords) {
            throw new CouchClientErrorMessage("delete of records not enabled for couchdb ${couchDb.couchdb.name}.  Configuration parameter \"couchdb.deleteRecords\" must be set to true to enable delete.")
        }
    }

    static void checkCancelReplication(ConfigObject couchDb) {
        if (!(couchDb.couchdb.cancelReplication || couchDb.couchdb.deleteRecords || couchDb.couchdb.deleteDatabase)) {
            throw new CouchClientErrorMessage("cancel replication not allowed for couchdb ${couchDb.couchdb.name}.  One of the following configuration parameters is required: \"couchdb.cancelReplication\", \"couchdb.deleteRecords\", \"couchdb.deleteDatabase\".")
        }
    }

    static void message(String message) {
		logger.info message
		System.out << "${message}\n"
	}

	static void error(String error) {
		logger.error error
		System.err << "error: ${error}\n"
	}

	int processCommandLineInstructions(args) {
		logger.debug "processing ${args}"
        CouchCliBuilder cli = getCliBuilder()
        Class<? extends AbstractAction> actionClass = null
        String actionName = null
        if (args) {
            actionName = args[0]
            actionClass = actions[actionName]
        }
        if (!actionClass) {
            cli.h(longOpt:'help', 'show usage information')
            cli.v(longOpt:'version', 'version of this client')
            if (!args) {
                cli.usage()
                return SUCCESS
            }
            OptionAccessor optionAccessor = cli.parse([args[0]])
            if (!optionAccessor) {
                return FAILURE
            }
            if (optionAccessor.h) {
                cli.usage()
                return SUCCESS
            }
            if (optionAccessor.v) {
                message "${couchClientConfig.application.name} version ${couchClientConfig.version}"
                return SUCCESS
            }
            error "unknown action \"${actionName}\""
            cli.usage()
            return NOHANDLER
        }

        AbstractAction action = actionClass.newInstance()
        action.actionName = actionName
        if (args?.size() > 1) {
            action.actionArgs = args.toList()[1..-1]
        }
        action.couchClient = this
        action.api = new CouchApi()
        int exitCode = FAILURE
        try {
            exitCode = action.doPerformAction()
        } catch (CouchClientErrorMessage errorMessage) {
            error errorMessage.message
            exitCode = errorMessage.exitCode
        }
        return exitCode
	}

	static void main(args) {
		int exitCode = FAILURE
		try {
			exitCode = new CouchClient().processCommandLineInstructions(args)
		} catch (Throwable t) {
			error "${StackTraceUtils.extractRootCause(t).class.simpleName}.  See log for details."
			logger.fatal "${t.class.simpleName}", StackTraceUtils.sanitizeRootCause(t)
		}
		System.exit(exitCode)
	}
	
}
