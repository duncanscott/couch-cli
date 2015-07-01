package net.duncanscott.couch_cli.action

import groovy.util.ConfigObject;
import net.duncanscott.couch_cli.option.NameOption
import org.apache.commons.cli.Option
import net.duncanscott.couch_cli.client.CouchClient
 
class CreateDatabaseAction extends AbstractAction {

	NameOption nameOption
	
	@Override
	public String getDescription() {
		'Create database(s) at the specified couchdb.  New databases are named via the --name option.'
	}

	void createDatabase(ConfigObject couchDb, String name) {
		api.createDatabase(couchDb.couchdb.url, name)
		message "created database ${couchDb.couchdb.name}.${name}"
	}
	
	@Override
	public void configureOptions() {
		nameOption = new NameOption(this)
	}

	@Override
	public int performAction() {
		ConfigObject couchDb = requireSubjectDatabase()
		nameOption.names.each { String name ->
			createDatabase(couchDb,name)
		}
		return CouchClient.SUCCESS
	}

}
