import org.springframework.web.client.ResourceAccessException
import grails.converters.*

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON


includeTargets << grailsScript("_GrailsInit")

gartEM = null
magicLinkLeyend = "Server running. Browse to "  // strictly!! (fixed in Eclipse console??)


// --------------------------------------------------------------------------------------------------------------------

target(check2Cloud: "The description of the script goes here!" ) {
	

	depends ( createGartEM )
	
	def result = doREST( op: "check" )

	displayResult(result)

}

setDefaultTarget(check2Cloud)

// -------------------------------------------------------------------------------------

target(send2Cloud: "The description of the script goes here!") {

	depends ( createGartEM )
	
	def result = doREST( op: "ide", full: true )

	displayResult(result)

	if (result.success) displayResult( [success: "\n\nGArt IDE ${magicLinkLeyend}${result.link}" ] )

}

// --------------------------------------------------------------------------------------------------------------------

doREST = { args ->
	def clk = System.currentTimeMillis()
	def gartHost = 'http://localhost:8090/gart-ide'
	def (gartUser,gartPass) = (System.getenv("GART_AUTH")?:"demoUser:demoPass").split(':')
	def projectId = gartEM.appProyect['pid'] ? gartEM.appProyect['pid'] : 0
	if ( (argsMap["params"][1]?:'')=='update' && projectId==0) args.op = 'update'  
	boolean full = args?.full ? args.full : false
	def result = [:]
	restURL = "${gartHost}/rest/${gartUser}/${projectId}/${args.op}"
    def http = new HTTPBuilder(restURL)
    http.auth.basic gartUser, gartPass
	try {
		http.request(POST, groovyx.net.http.ContentType.JSON) { req ->
			body = gartEM.getProjectJSON(full)
			response.success = { resp, json ->
				result.json = json
				if (json.error) {
					result = [ error: json.error , exit: 1 ]
				} else {
					result.link = "${gartHost}/project/show/" + ( json.newId ?: projectId )
					result.success = json.success 
					if (json.upgrade) result = doUpgrade(json) << [link: result.link]
					result.lap = String.format("%-5.1f", (System.currentTimeMillis()-clk)/1000 )
				}
			}
			response.'401' = { resp -> 	result = [ error: "GArt authentication failed. Are your username and password correct? (set env. variable GART_AUTH=username:password)", exit: 1 ]	}
			response.'403' = { resp -> 	result = [ error: "You do not have permission in GArt", exit: 1 ] }
			response.failure = { resp, json -> result = [ error: "Connection for ${args.op} failed - status ${resp.status} - ${json.message}", exit: 1 ] }
		}
	} catch (org.apache.http.conn.HttpHostConnectException e) {
		result = [ error: "No connection is achieved with the application IDE. Check your Internet access connection, or try again later.", exit: 1 ]
	} catch (e) {
		result = [ error: "ERROR: $e", exit: 1 ]
	}
	result
}

// --------------------------------------------------------------------------------------------------------------------

doUpgrade = { json ->
	def file = new File(gartEM.appProyecFileName)
	file.withWriter { Writer writer -> writer.write( json.data.toString() ) }
	gartEM.loadAppProyect()
	if (!gartEM.haveError()) {
		if (json.newId) 
			[ success: json.success ]
		else
			[ success: 'Your application was upgraded from GArt IDE...' ]
	}else{
		gartEM.getWorkResult() << [exit: 1]
	}	
}

