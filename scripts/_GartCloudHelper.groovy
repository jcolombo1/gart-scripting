import org.springframework.web.client.ResourceAccessException
import grails.converters.*

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON


includeTargets << grailsScript("_GrailsInit")

gartEM = null
magicLinkLeyend = "Server running. Browse to "  // strictly!! (fixed in Eclipse console??)


// --------------------------------------------------------------------------------------------------------------------

target(check2Cloud: "Check for Sync" ) {
	
	depends ( createGartEM )
	
	doREST( op: "check" )

}

setDefaultTarget(check2Cloud)

// -------------------------------------------------------------------------------------

target(send2Cloud: "Sync & access to GArt-IDE") {

	depends ( createGartEM )
	
	def result = doREST( op: "ide", full: true )

	if (result.success) displayResult( [success: "\n\nGArt IDE ${magicLinkLeyend}${result.link}" ] )

}

// --------------------------------------------------------------------------------------------------------------------

doREST = { args ->
	
	def clk = System.currentTimeMillis()
	def confirm = '... Even so, do you want to continue? (Note: may be out of sync between local project and Gart-IDE) : '
	def gartHost = grailsSettings.config.grails.gart.host ?: ( System.getenv("GART_HOST") ?: 'http://gart-ide.cloudfoundry.com' ) 
	def (gartUser,gartPass) = grailsSettings.config.grails.gart.authentication ?: (System.getenv("GART_AUTH")?:"demoUser:demoPass").split(':')

	def projectId = gartEM.appProyect['id'] ? gartEM.appProyect['id'] : 0
	if ( (argsMap["params"][1]?:'')=='update' && projectId!=0) args.op = 'update'
	boolean full = (args.full || args.op=='update') ? true : false
	def result = [:]
	def projectBody = gartEM.getProjectJSON(full)
	if (!gartEM.haveError()) {
		def link = "${gartHost}/project"
		restURL = "${gartHost}/rest/${gartUser}/${projectId}/${args.op}"
		println "$gartUser : $gartPass - $restURL"
		def http = new HTTPBuilder(restURL)
		http.auth.basic gartUser, gartPass
		try {
			http.request(POST, groovyx.net.http.ContentType.JSON) { req ->
				body = projectBody
				response.success = { resp, json ->
					result.json = json
					if (json.error) {
						result = [ error: json.error , exit: 1 ]
					} else {
						link = "${gartHost}/project/show/" + ( json.newId ?: projectId )
						result.success = json.success
						if (json.upgrade) result = doUpgrade(json)
						result.lap = String.format("%-5.1f", (System.currentTimeMillis()-clk)/1000 )
						result << [link: link]
					}
				}
				response.'401' = { resp -> 	result = [ error: "Authentication failed. Are your username and password correct? (try again or must set settings.groovy)", exit: 1 ]	}
				response.'403' = { resp -> 	result = [ error: "You don't have GArt'permission (try accessing by web)", exit: 1 ] }
				response.failure = { resp, json -> result = [ error: "Connection for ${args.op} failed - status ${resp.status} - ${json.message}", confirm: confirm, link: link ] }
			}
		} catch (e) {
			println e
			result = [ error: "No connection is achieved to GArt-IDE. Check your Internet access connection, or try again later.", confirm: confirm, link: link ]
		}
	} else {
		result = gartEM.getWorkResult()
	}
	
	displayResult(result)
	result
}

// --------------------------------------------------------------------------------------------------------------------

doUpgrade = { json ->
	def file = new File(gartEM.appProyecFileName)
	file.withWriter('UTF-8') { Writer writer -> writer.write( json.data.toString() ) }
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

