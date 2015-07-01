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

import groovy.json.JsonBuilder
import groovy.json.StringEscapeUtils
import groovyx.net.http.EncoderRegistry
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.StackTraceUtils

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.*

class CouchApi {
	
	static final Logger logger = Logger.getLogger(CouchApi.class.name)
	static final String REPLICATOR = '_replicator'

	private httpBuillder(String databaseBaseUrl) {
		HTTPBuilder http = new HTTPBuilder(databaseBaseUrl)
		http.encoderRegistry = new EncoderRegistry(charset: 'UTF-8')
		return http
	}

	List<String> getDatabaseList(String databaseUrl) {
		HTTPBuilder http = httpBuillder(databaseUrl)
		List<String> databases = []
		
		http.request(GET,JSON) { req ->
			uri.path = '_all_dbs'
			response.success = { resp, json ->
			  json.each {databases << it?.toString()}
			}
		}
		return databases
	}


	private String startReplication(HTTPBuilder http, String databaseBaseUrl, String databaseName, String replicationDatabaseBaseUrl, String replicationDatabaseName, boolean continuous) {
		/*
		 * POST /_replicate
		 * {"source":"example-database","target":"http://example.org/example-database"}
		*/
		Map<String,Object> requestBody = [
			'source' : "${databaseBaseUrl}${databaseName}" ,
			'target' : "${replicationDatabaseBaseUrl}${replicationDatabaseName}",
			'create_target' : true,
			'continuous' : continuous
		]
		JsonBuilder json = new JsonBuilder(requestBody)
		logger.debug "posting replication request to ${replicationDatabaseBaseUrl}:\n${json.toPrettyString()}"
		String replicationId = null
		http.request( POST, JSON ) {
		  uri.path = REPLICATOR
		  body = json.toString()
		  response.success = { HttpResponseDecorator resp, respJson ->
			logger.debug "success response ${resp.status} to backup replication request:\n${respJson}"
			replicationId = respJson.id
		  }
		}
		return replicationId
	}
	
	String startPullReplication(String databaseBaseUrl, String databaseName, String replicationDatabaseBaseUrl, String replicationDatabaseName, boolean continuous = false) {
		HTTPBuilder http = httpBuillder(replicationDatabaseBaseUrl)  //pulling to backup database is better than pushing, especially when attachments need to be copied
		return startReplication(http, databaseBaseUrl, databaseName, replicationDatabaseBaseUrl, replicationDatabaseName, continuous)
	}
	
	String startPushReplication(String databaseBaseUrl, String databaseName, String replicationDatabaseBaseUrl, String replicationDatabaseName, boolean continuous = false) {
		HTTPBuilder http = httpBuillder(databaseBaseUrl)
		return startReplication(http, databaseBaseUrl, databaseName, replicationDatabaseBaseUrl, replicationDatabaseName, continuous)
	}

	private String doForPageReturnNextKey(String databaseBaseUrl, String databaseName, boolean includeDocs, int pageSize, Closure doThis, String startKey) {
		HTTPBuilder http = httpBuillder(databaseBaseUrl)
		int pageSizePluss1 = pageSize + 1
		if (pageSizePluss1 < 2) {
			pageSizePluss1 = 2
		}
		Map query = [ limit:pageSizePluss1, include_docs: includeDocs ]
		if (startKey) {
			query['startkey'] = "\"${StringEscapeUtils.escapeJavaScript(startKey)}\"" //express startKey as JSON string
		}
		int recordCount = 0
		def lastJson = null
		http.request( GET, JSON ) {
			uri.path = "${databaseName}/_all_docs"
			uri.query = query
			response.success = { HttpResponseDecorator resp, respJson ->
				respJson?.'rows'?.each { json ->
					recordCount++
					if (lastJson?.'id' && !(lastJson.'id'.startsWith('_'))) {
						doThis(lastJson)
					}
					lastJson = json
				}
			}
		}
		if (recordCount < pageSizePluss1) {
			if (lastJson?.'id' && !(lastJson.'id'.startsWith('_'))) {
				doThis(lastJson)
			}
			return null
		}
		return lastJson?.'key'
	}
	

	void doForAllRecords(String databaseBaseUrl, String databaseName, boolean includeDocs, int pageSize, Closure doThis) {
		String lastKey = doForPageReturnNextKey(databaseBaseUrl, databaseName, includeDocs, pageSize, doThis, null)
		while (lastKey) {
			lastKey = doForPageReturnNextKey(databaseBaseUrl, databaseName, includeDocs, pageSize, doThis, lastKey)
		}
	}
	

	void deleteRecord(String databaseBaseUrl, String database, String recordId, String revision) {
		String url = "${databaseBaseUrl}${database}/${recordId}?rev=${revision}"
		HTTPBuilder http = httpBuillder(url)
		http.request( DELETE, JSON ) {
			response.success = { HttpResponseDecorator resp, respJson ->
				logger.debug "deleted record ${url}"
			}
		}
	}


	void compactDatabase(String databaseBaseUrl, String database) {
		String url = "${databaseBaseUrl}${database}/_compact"
		HTTPBuilder http = httpBuillder(url)
		http.request( POST, JSON ) {
			response.success = { HttpResponseDecorator resp, respJson ->
				logger.debug "compacted database ${url}"
			}
		}
	}


	void compactViews(String databaseBaseUrl, String database, String designDocument) {
		String url = "${databaseBaseUrl}${database}/_compact/${designDocument}"
		HTTPBuilder http = httpBuillder(url)
		http.request( POST, JSON ) {
			response.success = { HttpResponseDecorator resp, respJson ->
				logger.debug "compacted views ${url}"
			}
		}
	}


    /*
    http://server-name:5984/db_name/_design/view_name
    */
    Map getDesignDocument(String databaseBaseUrl, String database, String designDocument) {
        String url = "${databaseBaseUrl}${database}/_design/${designDocument}"
        HTTPBuilder http = httpBuillder(url)
        Map designDoc = null
        http.request( GET, JSON ) {
            response.success = { HttpResponseDecorator resp, Map respJson ->
                designDoc = respJson
            }
        }
        return designDoc
    }


    boolean validateDesignDocumentName(String databaseBaseUrl, String database, String designDocument) {
        boolean valid = false
        Map designDoc = getDesignDocument(databaseBaseUrl,database,designDocument)
        if (designDoc?.'error') {
            valid = false
        } else if (designDoc?.'_id') {
            valid = true
        }
        return valid
    }


    void cleanupViews(String databaseBaseUrl, String database) {
        String url = "${databaseBaseUrl}${database}/_view_cleanup"
        HTTPBuilder http = httpBuillder(url)
        http.request( POST, JSON ) {
            response.success = { HttpResponseDecorator resp, respJson ->
                logger.debug "compacted views ${url}"
            }
        }
    }


	void deleteDatabase(String databaseBaseUrl, String databaseName) {
		String url = "${databaseBaseUrl}${databaseName}/"
		HTTPBuilder http = httpBuillder(url)
		http.request( DELETE, JSON ) {
		  response.success = { HttpResponseDecorator resp, respJson ->
			logger.debug "deleted database ${url}"
		  }
		}
	}
	
	void createDatabase(String databaseBaseUrl, String databaseName) {
		HTTPBuilder http = httpBuillder(databaseBaseUrl)
		http.request( PUT, JSON ) {
		  uri.path = databaseName
		  response.success = { HttpResponseDecorator resp, respJson ->
			  logger.debug "created database ${databaseBaseUrl}${databaseName}"
		  }
		}
	}
	
	
}
