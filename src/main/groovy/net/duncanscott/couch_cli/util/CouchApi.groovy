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
import groovyx.net.http.EncoderRegistry
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import org.apache.log4j.Logger

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*

class CouchApi {
	
	static final Logger logger = Logger.getLogger(CouchApi.class.name)
	static final String REPLICATOR = '_replicator'
	
	List<String> getDatabaseList(String databaseUrl) {
		HTTPBuilder http = new HTTPBuilder(databaseUrl)
		http.encoderRegistry = new EncoderRegistry( charset: 'UTF-8' )
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
		HTTPBuilder http = new HTTPBuilder(replicationDatabaseBaseUrl)  //pulling to backup database is better than pushing, especially when attachments need to be copied
		http.encoderRegistry = new EncoderRegistry( charset: 'UTF-8' )
		return startReplication(http, databaseBaseUrl, databaseName, replicationDatabaseBaseUrl, replicationDatabaseName, continuous)
	}
	
	String startPushReplication(String databaseBaseUrl, String databaseName, String replicationDatabaseBaseUrl, String replicationDatabaseName, boolean continuous = false) {
		HTTPBuilder http = new HTTPBuilder(databaseBaseUrl)
		http.encoderRegistry = new EncoderRegistry( charset: 'UTF-8' )
		return startReplication(http, databaseBaseUrl, databaseName, replicationDatabaseBaseUrl, replicationDatabaseName, continuous)
	}

	private String doForPageReturnNextKey(String databaseBaseUrl, String databaseName, boolean includeDocs, int pageSize, Closure doThis, String startKey) {
		HTTPBuilder http = new HTTPBuilder(databaseBaseUrl)
		http.encoderRegistry = new EncoderRegistry( charset: 'UTF-8' )
		int pageSizePluss1 = pageSize + 1
		if (pageSizePluss1 < 2) {
			pageSizePluss1 = 2
		}
		Map query = [ limit:pageSizePluss1, include_docs: includeDocs ]
		if (startKey) {
			query['startkey'] = "\"${groovy.json.StringEscapeUtils.escapeJavaScript(startKey)}\"" //express startKey as JSON string 
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
		HTTPBuilder http = new HTTPBuilder(url)
		http.encoderRegistry = new EncoderRegistry( charset: 'UTF-8' )
		http.request( DELETE, JSON ) {
			response.success = { HttpResponseDecorator resp, respJson ->
				logger.debug "deleted record ${url}"
			}
		}
	}
	
	void deleteDatabase(String databaseBaseUrl, String databaseName) {
		String url = "${databaseBaseUrl}${databaseName}/"
		HTTPBuilder http = new HTTPBuilder(url)
		http.encoderRegistry = new EncoderRegistry( charset: 'UTF-8' )
		http.request( DELETE, JSON ) {
		  response.success = { HttpResponseDecorator resp, respJson ->
			logger.debug "deleted database ${url}"
		  }
		}
	}
	
	void createDatabase(String databaseBaseUrl, String databaseName) {
		HTTPBuilder http = new HTTPBuilder(databaseBaseUrl)
		http.encoderRegistry = new EncoderRegistry( charset: 'UTF-8' )
		http.request( PUT, JSON ) {
		  uri.path = databaseName
		  response.success = { HttpResponseDecorator resp, respJson ->
			  logger.debug "created database ${databaseBaseUrl}${databaseName}"
		  }
		}
	}
	
	
}
