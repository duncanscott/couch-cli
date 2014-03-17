package net.duncanscott.couch_cli.action

import net.duncanscott.couch_cli.client.CouchClient
import net.duncanscott.couch_cli.util.CouchApi
import org.apache.log4j.Logger
import spock.lang.Specification

class CreateAndDeleteIntegrationSpec extends Specification {
	
	static Logger logger = Logger.getLogger(CreateAndDeleteIntegrationSpec.class.name)
	CouchClient client  = new CouchClient()
	CouchApi api = new CouchApi()
	
	static String CREATE = 'create'
	static String DELETE = 'delete'
	
	def "create database and delete test"() {
		setup:
			String database = 'localhost'
			String newName = 'testing-testing-123'
			ConfigObject config = client.getConfiguration(database)
			Set<String> databaseNames = []
			List<String> args = "${database} --name ${newName}".split(/\s+/)
			CreateDatabaseAction createAction = new CreateDatabaseAction(couchClient:client,api:api,actionName:CREATE)
			DeleteDatabaseAction deleteAction = new DeleteDatabaseAction(couchClient:client,api:api,actionName:DELETE)
			List<AbstractAction> actions = []
			actions << deleteAction
			actions << createAction
			actions.each { AbstractAction action ->
				action.actionArgs += args
			}
		expect:
			config.couchdb.url
			actions.each { AbstractAction action ->
				assert action.actionName
				Class actionClass = CouchClient.actions[action.actionName]
				assert actionClass
			}
		when:
			if (!api.getDatabaseList(config.couchdb.url).contains(newName)) {
				createAction.doPerformAction()
			}
		then:
			api.getDatabaseList(config.couchdb.url).contains(newName)
		when:
			deleteAction.doPerformAction()
		then:
			!api.getDatabaseList(config.couchdb.url).contains(newName)
		when:
			createAction.doPerformAction()
		then:
			api.getDatabaseList(config.couchdb.url).contains(newName)
		when:
			deleteAction.doPerformAction()
		then:
			!api.getDatabaseList(config.couchdb.url).contains(newName)
	}
	
}
