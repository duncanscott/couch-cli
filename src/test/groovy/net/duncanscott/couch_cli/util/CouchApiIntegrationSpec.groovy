package net.duncanscott.couch_cli.util

import net.duncanscott.couch_cli.client.CouchClient
import org.apache.log4j.Logger
import spock.lang.Specification

class CouchApiIntegrationSpec extends Specification {
	
	CouchClient client  = new CouchClient()
	CouchApi api = new CouchApi()
	Logger logger = Logger.getLogger(CouchApi.class.name)
	
	def "getDatabaseList test"() {
		setup:
			String database = 'localhost'
			ConfigObject config = client.getConfiguration(database)
			List<String> databaseList = null
		expect:
			config.couchdb.url
		when:
			databaseList = api.getDatabaseList(config.couchdb.url)
		then:
			databaseList
			
	}
	
}
