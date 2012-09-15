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
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		render(contentType: "text/json") { GartDemo.list(params) }
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
	
//    def save() {
//      def jsonObject = JSON.parse(params.gartdemo)
//      GartDemo gartdemoInstance = new GartDemo(jsonObject)
//      if (!gartdemoInstance.save(flush: true)) {
//        ValidationErrors validationErrors = gartdemoInstance.errors
//        render validationErrors as JSON
//      }
//      render gartdemoInstance as JSON
//    }
//    
//    def show() {
//      def gartdemoInstance = GartDemo.get(params.id)
//      if (!gartdemoInstance) {
//        flash.message = message(code: 'default.not.found.message', args: [message(code: 'gartdemo.label', default: 'GartDemo'), params.id])
//        render flash as JSON
//      }
//      render GartDemoInstance as JSON
//    }
//
//    def update() {
//      def jsonObject = JSON.parse(params.gartdemo)
//      GartDemo gartdemoReceived = new GartDemo(jsonObject)
//
//        def gartdemoInstance = GartDemo.get(jsonObject.id)
//        if (!gartdemoInstance) {
//            flash.message = message(code: 'default.not.found.message', args: [message(code: 'gartdemo.label', default: 'GartDemo'), params.id])
//            render flash as JSON
//        }
//
//        if (jsonObject.version) {
//          def version = jsonObject.version.toLong()
//          if (gartdemoInstance.version > version) {
//            gartdemoInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
//                          [message(code: 'gartdemo.label', default: 'GartDemo')] as Object[],
//                          "Another user has updated this GartDemo while you were editing")
//                ValidationErrors validationErrors = gartdemoInstance.errors
//                render validationErrors as JSON
//                return
//            }
//        }
//
//        gartdemoInstance.properties = gartdemoReceived.properties
//
//        if (!gartdemoInstance.save(flush: true)) {
//          ValidationErrors validationErrors = gartdemoInstance.errors
//          render validationErrors as JSON
//        }
//		    render gartdemoInstance as JSON
//    }
//
//    def delete() {
//      def gartdemoId = params.id
//      def gartdemoInstance = GartDemo.get(params.id)
//      if (!gartdemoInstance) {
//        flash.message = message(code: 'default.not.found.message', args: [message(code: 'gartdemo.label', default: 'GartDemo'), params.id])
//        render flash as JSON
//      }
//      try {
//            gartdemoInstance.delete(flush: true)
//      }
//      catch (DataIntegrityViolationException e) {
//        flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'gartdemo.label', default: 'GartDemo'), params.id])
//        render flash as JSON
//      }
//      render gartdemoInstance as JSON
//    }
}
