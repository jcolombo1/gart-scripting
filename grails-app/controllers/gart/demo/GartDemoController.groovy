package gart.demo

import grails.util.Environment

import grails.converters.JSON
import grails.validation.ValidationErrors
import groovy.json.JsonBuilder;

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.springframework.dao.DataIntegrityViolationException

class GartDemoController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

	def beforeInterceptor = { log.debug("--> ${actionName.toUpperCase()} "+(request.xhr?"Ajax ":"")+"params: "+ params) }
	def afterInterceptor  = { log.debug("  <-- ${actionName.toUpperCase()} params: "+ params) }

    def index() {
		redirect(uri: "/index.html")   // redirect(action: "list", params: params)
    }
	
    def list() {
		demoInit()
		println params
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		def cbk = params['callback']
		def rv = ( GartDemo.list(params) as JSON )
		println " ----> "+rv
		if (cbk) rv = cbk+'('+ rv +')' 
		//render(contentType: "text/json") { GartDemo.list(params) }
		render rv
    }
	
	
	
	private demoInit() {
		
		if ( GartDemo.list().size() > 0 ) return
		
		switch (Environment.current) {
			case Environment.PRODUCTION:
				break
			case Environment.DEVELOPMENT:
			case Environment.TEST:
				[
					[ name: "John Backus", email: "JohnBackus@amail.com"],
					[ name: "James Gosling", email: "JamesGosling@amail.com"],
					[ name: "John Kemeny", email: "JohnKemeny@amail.com" ],
					[ name: "Dennis Ritchie", email: "DennisRitchie@amail.com" ],
					
				].each {
					new GartDemo( it ).save(failOnError:true)
				}
				break
		}
	}
	
}
