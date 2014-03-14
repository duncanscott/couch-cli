package net.duncanscott.couch_cli.client

import org.apache.log4j.Logger
import spock.lang.Specification

class CouchClientSpec extends Specification {
	
	CouchClient client = new CouchClient()
	Logger logger = Logger.getLogger(CouchClientSpec.class.name)
	
	def "getActionNames test"() {
		setup:
			List<String> actionNames = client.getActionNames()
			logger.info "actions: ${actionNames}"
		expect:
			actionNames
	}
	
	def "getUsage test"() {
		setup:
			String usage = client.getUsage()
			logger.info "usage: ${usage}"
		expect:
			usage
	}
	
	def "load configuration URL"() {
		setup:
			URL url = client.getCouchDbConfigurationUrl()
			logger.info "config URL: ${url}"
		expect:
			url
	}
	
	
}
