package net.duncanscott.couch_cli.action

import net.duncanscott.couch_cli.client.CouchClient
import net.duncanscott.couch_cli.util.CouchApi
import org.apache.log4j.Logger
import spock.lang.Specification

class CreateAndDeleteIntegrationSpec extends Specification {
	
	CouchClient client  = new CouchClient()
	CouchApi api = new CouchApi()
	
	def "create database and delete test"() {
		setup:
			String database = 'localhost'
			ConfigObject config = client.getConfiguration(database)
			Set<String> databaseNames = []
		expect:
			config.couchdb.url
	}
	
}
