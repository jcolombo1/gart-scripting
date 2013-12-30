import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.FileSystemResource

includeTargets << grailsScript("_GrailsInit")

gartEM = null
grailsApp = null

target(createGartEM: "Create Gart Extension Manager instance") {
	
	if (gartEM) return
	
	if (!grailsApp) {
		event 'StatusError', ["Before 'GartExtensionManager.getInstance' you must initialize scripting scope (grailsApplication not setted)"]
		exit 1
	}
	
	gartEM = GartExtensionManager.getInstance(grailsApp, "$basedir")	// setting basedir for load Extension Files
	 
}

setDefaultTarget(createGartEM)



// ------------------------------------------------------------------------------------------

class GartExtensionManager {
	private static GartExtensionManager me 
	protected static extensionsFolderName = 'extensions'
	protected static artefactsFolderName = 'artefacts'
	protected static extensionsPath = "src/gart/templates/$extensionsFolderName"
	protected static artefactsPath = "src/gart/templates/$artefactsFolderName"
	static appProyecFileName = 'src/gart-design.json' 
//	static appProyecFileName = 'src/gart-xxxxxx.json' 
	static baseDir
	
	boolean loadedFiles, compiledAll
	Map extensionFiles = [:]  // [ extensionFileName: [ js: ExtensionFile, html: ExtensionFile ] ]
	Map extensions = [:]
	Map extensionsByInto = [:]
	def appProyect
	def compileOnLoad = false
	def workMap = [errors: []]
	def grailsApp
	def classLoader
	def pluginManager
	
	GartExtensionManager(grailsApplication, String baseDirectory=null) {
		grailsApp = grailsApplication  
		if (baseDirectory) loadExtensionFiles(baseDirectory)  
	}
	
	static synchronized GartExtensionManager getInstance(grailsApplication, baseDirectory) {
		if (!me) me = new GartExtensionManager(grailsApplication, baseDirectory)
		me
	}
	
	def haveError() {
		extensions.findAll{ it.value.extensionFile.compiled && it.value.errors?.size()>0 }.each { workMap.errors.addAll it.value.errors ; it.value.errors = [] }  //trasmit errors
		(workMap.errors.size() > 0)
	}

	
	def loadResources(List types, String dirPath, String endOfName="") {
		def resmap = [:]
		def resolver = new PathMatchingResourcePatternResolver()
		def dir = new FileSystemResource(dirPath)
		if (dir.exists()) {
			types.each {
				try {
					resmap[(it)] = ( resolver.getResources("file:$dirPath/**/*$endOfName.$it") ?: [] )
				} catch (e) {
					event 'StatusError', ['Error while loading files of dir $dirPath', e]
				}
			}
		}
		resmap
	}

	def loadExtensionFiles(baseDirectory, boolean compile = false) { 
		if (loadedFiles) return
		compileOnLoad = compile
		baseDir = baseDirectory
		def templatesDirPath = "${baseDir}/${extensionsPath}"
		
		def resmap = loadResources( ['html', 'js'], templatesDirPath )

		// FIXME : enhancement debería ser tambien para templates .gsp y .groovy (todo el equipo completo :)
		resmap.js.each {
			ExtensionFile ex = new ExtensionFile(this, it, "js")
			extensionFiles.put ex.extensionFileName, [ js: ex, html: null ]
		}
		resmap.html.each {
			ExtensionFile ex = new ExtensionFile(this, it, "html")
			if (extensionFiles.containsKey(ex.extensionFileName)) extensionFiles[(ex.extensionFileName)].html = ex
			else extensionFiles.put ex.extensionFileName, [ js: null, html: ex ]
		}
				
		if (compileOnLoad) compileAll()
		
		loadAppProyect()
		
		loadedFiles = true
		extensionFiles
	}

	def compileAll() {
		if (!compiledAll) {
			extensionFiles.each {
				if (it.value.js) it.value.js.compile()
				if (it.value.html) it.value.html.compile()
			}
		}
		compiledAll = true
		//println extensionFiles.size() + " extension files compiled (there are "+ extensions.size() + " extensions)"
	}
	
	/**
	 * Returns a List with Procedure names (descriptions & last date will be appended if needed).<br>
	 * @param order 'alfa' | 'priority' | default by date (desc)<br>
	 * @param withDesc false returns keys, true returns descriptive string (key - description - date)  
	 */
	def getProcedureNames( withDesc = true, order = 'date' ) {
		def procs = appProyect["content"]["procedures"].entrySet()
		if (order=='date') procs = procs.sort{ it.value?.history?.date }.reverse()
		else if (order=='alfa') procs = procs.sort{ it.key }
		else procs = procs.sort{ it.value?.priority }
		LinkedList rv = new LinkedList()
		if (withDesc) procs.each { rv << "\"${it.key}\" : ${it.value?.description} - ${it.value?.history?.date[0..9]} ${it.value?.history?.date[11..15]}" }
		else procs.each { rv << it.key }
		rv
	}

	private _drill4m =  { model, extension ->   // recursive drilling
		extension.domains?.each { model << it.value } 
		extension.extensions?.each { _drill4m(model,it) }
	}
	
	def getModelForProcedure(procedure) {
		LinkedHashSet modelNames = []   // no Dups! 
		def model = []
		procedure.artefacts?.each { it.value?.extensions?.each { _drill4m(modelNames,it) } }
		modelNames.each {  
			def domain = it.indexOf('.') > 0 ? it : GrailsNameUtils.getClassNameRepresentation(it)
			model << (grailsApp.getDomainClass(domain))
		}
		model
	}

	def assembleProcedure(String procedureName) {
		
		def procedure = appProyect["content"]["procedures"]["${procedureName}"]
		
		if (!procedure) { workMap.errors << "Procedure $procedureName not found!" ; return }
		if (!procedure.artefacts) { workMap.errors << "Procedure $procedureName has no artefacts defined!" ; return } 

		def model = new Model( getModelForProcedure(procedure) )
		  
		def sufixName = have(procedure.sufixName) ? '-'+procedure.sufixName : ''
		def relPath = have(procedure.relPath) ? '/'+procedure.relPath : ''
		
		
		procedure.artefacts?.entrySet().sort{ (it.value.priority?:0) }.each {    // artefacts Map - order by artefact.priority if indicated
			
			def templateViewName = it.key.replace('|','.') 
			def tfile = templateViewName.substring(templateViewName.lastIndexOf('.')+1).toLowerCase();
			def path_js = "/js${relPath}"
			def path_css = "/css${relPath}"
			def pathf = tfile=='html' ? relPath : ( tfile=='css' ? path_css : path_js )
			def camelpn = procedureName[0].toLowerCase() + procedureName[1..-1]
			
			def destDir = "$baseDir/web-app$pathf"						// full fsystem path to destination dir
			def targetFPath = "$destDir/$camelpn$sufixName.$tfile"		// full fsystem pathname of target file 
			
			File file
			
			def fn = baseDir +'/'+ artefactsPath +'/'+ templateViewName
			if (!(file=new File(fn)).exists()) { workMap.errors << "Missing template file $fn for assemble procedure $procedureName !"; return }

			workMap = [ procedureName: procedureName, errors: [], embededCount: 0, templateText: new FileInputStream(file).getText(), domainClass: model[0], templateViewName: templateViewName, tfile: tfile, extInstanceNum: 0, intosUsed: [], reclaimsID: [], askstr: [:] ]
			
			replacer( it.value )
			
			def toRemove = []
			workMap.templateText.findAll(/<!--\s*EMBED\s*[\w%\,\s\.-]*\s*-->/).each {
				if ( it ==~ /<!--\s*EMBED\s*[[\w\.-]+[,\s]+]+%REQ(UIRE)*D*\s*-->/ )	workMap.errors << "EMBED '${it}' is required into view ${workMap.templateViewName} for procedure ${procedureName}"
				else toRemove << it
			}
			toRemove.each { workMap.templateText = workMap.templateText.replaceAll((it+"\\s*"),"") }  // remove marks (remove all EMBEDs checking that are required)
			
			if (workMap.errors.size()>0) return
			
			
			// ---- no assembly errors, prepare and run the resulting script and write target file    
			
			workMap.templateText = workMap.embededCount > 0 ? "<% def ASK=[:],I; " + templateAsks() +"%>"+ workMap.templateText : workMap.templateText
			//println "----------------------------\n" + workMap.templateText +"\n--------------------------------"

			def binds = [ 	
							description: procedure.description ?: '' ,
							name	   : procedureName ,  															// 	MyProcedureName
							template   : templateViewName,															//  basic/generic.js
							path_html  : relPath,																	//	/mypath		|| none
							path_js    : path_js,																	//	/js/mypath
							path_css   : path_css,																	// 	/css/mypath
							doc_name   : "$camelpn$sufixName",    													// 	myProcedureName-sufix
							doc_fname  : "$camelpn$sufixName.$tfile",												// 	myProcedureName-sufix.js
							doc_path     : "$pathf",																//	/js/mypath													 
							doc_fpath    : "$pathf/$camelpn$sufixName.$tfile",										// 	/js/mypath/myProcedureName-sufix.js
							doc_fpathHTML: "${relPath}/${camelpn}${sufixName}.html",								// 	/mypath/myProcedureName-sufix.html
							doc_fpathCSS : "${path_css}/${camelpn}${sufixName}.css",								// 	/js/mypath/myProcedureName-sufix.js
							doc_fpathJS  : "${path_js}/${camelpn}${sufixName}.js",									// 	/css/mypath/myProcedureName-sufix.css
						]
			def garts = [
							appName		 : grailsApp.metadata['app.name'],
							appVersion	 : grailsApp.metadata['app.version'],
							design	 	 : appProyect,
							path_js      : "/js",
							path_css     : "/css",
						]
			
			if (!(file=new File(destDir)).exists()) file.mkdirs()
			
			def rec = { id, type='div' ->
				def bid = id[0]!='#' ? id : id[1..-1] 
				workMap.reclaimsID << ('{id:"'+bid+'",type:"'+type+'"}')
				'"'+id+'"' 
			}

			def templateGenerator = new GartTemplateGenerator(classLoader, workMap)
			templateGenerator.grailsApplication = grailsApp
			templateGenerator.pluginManager = pluginManager
			templateGenerator.beforeMakeFile = { bindings -> bindings << [ ME : binds, GART : garts, REC : rec ] }
			templateGenerator.generateFile( model, targetFPath )

			//println "Procedure --> $procedureName - ${procedure.description} - Domain Model: " + model + " - Template :  " +templateViewName + "  - tfile: "+ tfile
		}
	}

	private replacer(extMap, deep=0) {	// static
		List extsList = extMap["extensions"]
		if (!extsList) return
		def how = workMap.procedureName ? "procedure $workMap.procedureName" : "domain ${workMap.domainClass.fullName}"
		def toRemove = []
		try {
			extsList.sort{ (it.priority?:0) }.each { proj_ext ->
				def ext = getExtension( proj_ext.name, workMap.tfile )
				if (!ext) { haveError(); workMap.errors << "extension '${proj_ext.name}|${workMap.tfile}' not found!" }
				else if (ext.errors?.size()>0) { workMap.errors.addAll ext.errors }
				else {
//					proj_ext.instance = proj_ext.instance ?: ( ++workMap.extInstanceNum > 1 ? "_${workMap.extInstanceNum}" : '') // tail from "_2" (_2..n) 
					proj_ext.instance = proj_ext.instance ?: ++workMap.extInstanceNum		// tail from _1  
					ext.intos.each { eInto ->
						eInto.names.each { embedName ->
							saveAsks(proj_ext.instance, eInto, proj_ext.intos?."$embedName")
							if (!eInto.args.contains('%ONCE') || !workMap.intosUsed.contains(eInto.id)) { //was embedded a %ONCE-into?
								def patt = "<!--\\s*EMBED\\s*"+ embedName +"[\\w%\\,\\s\\.-]*-->", fstr
								if ( (fstr = workMap.templateText.find(patt)) ) {
									def pos = workMap.templateText.indexOf(fstr)
									def indent = '\n' + (workMap.templateText.find("\\t* *"+patt) - fstr)
									//def code = "<% EXT='${proj_ext.instance}' %>" + indenterX( injectAsk2Code(eInto, proj_ext.intos?."$embedName"), indent )
									def code = "<% I=${proj_ext.instance} %>" + indenterX( eInto.code, indent )
									workMap.templateText = workMap.templateText.substring(0, pos) + code + indent + workMap.templateText.substring(pos)
									workMap.intosUsed << eInto.id	// for anti-dup into's %ONCE
									workMap.embededCount++
									toRemove << fstr
								}else if (!eInto.args.contains('%OPTATIVE')){
									workMap.errors << "EMBED '${embedName}' not found into view ${workMap.templateViewName} for $how (INTO is not %OPTATIVE)"
								}
							}	
						}
					}
				}
			}.each {
				replacer(it, ++deep)	//recursive
			}
		}catch (e) { println e  }
		toRemove.each { workMap.templateText = workMap.templateText.replaceAll((it+"\\s*"),"") }  // remove marks
	} 

	private String indenterX(String s, String i) { s.endsWith("\n") ? (new String(s.replace('\n',i).reverse()-i.reverse()).reverse() + '\n') : s.replace('\n',i) }

	private saveAsks( instance, into, emb_proj ) {
		def a = [:]; String s=""
		into."asks".each { a.put( it.symbol, it.value ) }
		a.putAll ( emb_proj?."asks" ? ( emb_proj?."asks".findAll { a.containsKey(it.key) } ) : [:] )  // set w/projects values
		if (a.size()<1) return "<% ASK = [:] %>" + into.code
		a.each {
			def val
			if (it.value instanceof String && it.value.matches(/-?[\d]*.?[\d]+/)) val = it.value  // is numeric?
			else val = it.value=='true' ? 'true' : ( it.value=='false' ? 'false' : ( it.value==null ? 'null' : "'${it.value}'" ) ) // SPECIAL CASES true|false|null
			s += "'${it.key}': $val, "
		}
		workMap.askstr[instance] = (workMap.askstr[instance]?:'') + s 
	}
	private templateAsks() {
		def rv =''
		workMap.askstr.each { k,v -> // k=instance
			rv += "ASK["+k+"]=["+ v +"]; "
		}
		rv
	}

//	private injectAsk2Code( into, emb_proy ) {
//		def a = [:]; String s=""
//		into."asks".each { a.put( it.symbol, it.value ) }
//		a.putAll ( emb_proy?."asks" ? ( emb_proy?."asks".findAll { a.containsKey(it.key) } ) : [:] )  // set w/projects values
//		if (a.size()<1) return "<% ASK = [:] %>" + into.code
//		a.each {
//			def val
//			if (it.value instanceof String && it.value.matches(/-?[\d]*.?[\d]+/)) val = it.value  // is numeric?
//			else val = it.value=='true' ? 'true' : ( it.value=='false' ? 'false' : ( it.value==null ? 'null' : "'${it.value}'" ) ) // SPECIAL CASES true|false|null   
//			s += "'${it.key}': $val, " 
//		}
//		"<% ASK = ["+ s +"] %>" + into.code
//	}
	
	private getExtension(String name, String tfile) {
		String k = "${name}|${tfile}"
		def ext = extensions.get( k )  // compound key eg: "basic.SampleEmbed:MyEmbed|js"
		if (ext) return ext
		try { extensionFiles[(name.split(/:/)[0])]."${tfile}".compile() } catch (e) {  }  // compile on-demand
		return extensions.get( k ) //"${name}|${tfile}" )
	}
		
	private loadAppProyect() {
		def file = new File(appProyecFileName)
		if (!file.exists()) {
			appProyect = [ id: 0, projectName: grailsApp.metadata.get('app.name'), history:[ milestone: 0 ], content: [ procedures: [:] ], client: [:] ] 
			def builder = new groovy.json.JsonBuilder()
			builder.setContent(appProyect)
			file = file.asWritable("UTF-8")
			file.withWriter { Writer writer -> writer.write( builder.toString() ) }
		} else {	
			def slurper = new groovy.json.JsonSlurper()
			appProyect = slurper.parseText( new FileInputStream(file).getText() )
			if (!(appProyect instanceof Map)) { workMap.errors << "Structure of directives file '${file.name}' is invalid (should be Map)." ; return } 
		}
	}

	Map getProjectJSON(boolean full = false) {
		if (!loadedFiles) return [:]
		def gm = grailsApp.metadata, gv = pluginManager.getGrailsPlugin('gart-scripting').version
		def project = [ id: appProyect.id, history: appProyect.history, content: [:] ]
		def artfs = [], extns = [:], doms = [:]
		if (full) {
			project = appProyect
			doms  = getDomainsMap()
			artfs = getArtefactsMap()
			extns = getExtensionsMap()
		}
		project['projectName'] = gm.get('app.name')  // overwrite name (as it may have changed)
		project.content << [ versions: [ gartV: gv, version: gm.get('app.version'), grailsV: gm.get('app.grails.version'), javaV: System.getProperty('java.version'), servletV: gm.get('app.servlet.version') ] ]
		project.client = [ domains: doms, artefacts: artfs, extensions: extns ]
		project
	}

	// only for send to ide
	private Map getDomainsMap() {
		def doms = [:]
		grailsApp.domainClasses.sort{ it.propertyName }.each { d ->
			//println "******************** ${d.propertyName} ************************"
			def props = []
			d.properties.each {
				def rel = it.manyToMany ? 'M:M' : it.manyToOne ? 'M:1' : it.oneToMany ? '1:M' : it.oneToOne ? '1:1' : null
				def os = rel ? ( rel[0]=='M' ? it.type.name : d.getRelatedClassType(it.name).name ) : it.type.simpleName  
				def p =  [ 	name: it.name, type: it.type.simpleName, naturalName: it.naturalName, fieldName: it.fieldName, persistent: it.persistent, optional: it.optional, coll: it.basicCollectionType ]
				if (rel) p << [ association: it.association, owningSide: it.owningSide, rel: rel, otherSide: os, embedded: it.embedded ] 
				else if (it.enum) p << [ enum: true, enumValues: it.type.values() ]
				
				def fc = d.getConstrainedProperties().get(it.name), cons, x, order = 9999
				if (fc) {
					order = fc.getOrder()
					cons = [ nullable: fc.isNullable(), display: fc.isDisplay(), editable: fc.isEditable() ]
					[ 	{ if(p.type=='String' && (x=(fc.isCreditCard() ? 'creditCard' : fc.isEmail() ? 'email' : fc.isPassword() ? 'password' : fc.isUrl() ? 'url' : null))) return [ validateAs: x ] else return null },
						{ fc.getInList() ? [ inList: fc.getInList() ] : null },
						{ fc.getSize() ? [ size: fc.getSize() ] : fc.getRange() ? [ range: fc.getRange().toString() ] : (fc.getMin()||fc.getMax()) ? [ min: fc.getMin(), max: fc.getMax() ] : null },
						{ (fc.getMinSize()||fc.getMaxSize()) ? [ minSize: fc.getMinSize(), maxSize: fc.getMaxSize() ] : null },
						{ p.type=='String' ? (fc.getMatches() ? [ matches: fc.getMatches() ] : null) : null },
						{ fc.getNotEqual() ? [ notEqual: fc.getNotEqual() ] : null },
						{ fc.getScale() ? [ scale: fc.getScale() ] : null },
						{ fc.getAttributes() ? [ attributes: fc.getAttributes() ] : null },
						{ fc.getFormat() ? [ attributes: fc.getFormat() ] : null },
						{ fc.getWidget() ? [ widget: fc.getWidget() ] : null },
					].each { if((x=it.call())) cons << x  }  // only add necesary constraints
					p << [ constraints: cons ]
				}
				p << [ order: order ]
				props << p  
				//println p 
			}
			def dom = [props: props, fullName: d.fullName, propertyName: d.propertyName, naturalName: d.naturalName, 
					  shortName: d.shortName, abstract: d.isAbstract(), identifier: d.identifier?.name, mappingStrategy: d.getMappingStrategy() ]
			if (d.hasSubClasses()) dom << [ hasSubClasses: d.hasSubClasses(), root: d.root, subClasses: (d.getSubClasses()*.fullName) ]
			doms << [ (d.propertyName) : dom ]
		}
		doms
	}
	
	// only for send to ide
	private Map getExtensionsMap() {
		if (!loadedFiles) return [:]
		compileAll()
		def rv = [:]
		extensions.each { k,v -> 
			def i=[]
			v.intos?.each { i << [names: it.names, args: it.args, asks: it.asks ] }
			rv.put( k, [ intos: i, description: v.description, family: v.family ] )	
		}
		rv
	}

	// only for send to ide
	private Map getArtefactsMap() {
		if (!loadedFiles) return []
		def templatesDirPath = "${baseDir}/${artefactsPath}"
		def resmap = loadResources( [ 'html', 'js' ], templatesDirPath )
		def templatesDir = new File(templatesDirPath)
		def artefacts = [:]
		resmap.each { k, v ->  // k = .ext; v = Resource
			v.each {
				def relfpath = it.file.canonicalPath[(templatesDir.canonicalPath.length()+1)..-1].replace('\\','/')
				def p = relfpath.lastIndexOf(".")
				def ns = relfpath[0..(p-1)] + "|$k"
				def templateText = new FileInputStream(it.file).getText()
				def embeds = []
				templateText.findAll(/<!--\s*EMBED\s*[\w%\,\s\.-]*\s*-->/).each { embeds << parseEmbed(it) }
				artefacts << [ (ns): [ embeds: embeds ] ]  
			}
		}
		artefacts
	}
	
	// only for send to ide called by getArtefactsMap()
	def private parseEmbed(embedText) {
		def p = embedText.split(/(<!--\s*EMBED|,|-->)/)[1..-1]*.trim()
		def req = p.any{ it.startsWith('%REQ') }
		[ name: p[0], required: req ]
	}

	def printWorkResult() {
		def res = getWorkResult()
		if (res.error) println res.error else if (res.success) println res.success
	}
	
	def getWorkResult() {
		def rv = [:]
		def how = !workMap?.templateViewName ? null : ( workMap?.procedureName ? 'Procedure: '+workMap?.procedureName : 'View: '+workMap.templateViewName )
		if (workMap.errors.size()>0) {
			rv["error"] = "... THERE ARE "+ workMap.errors.size() +" ERRORS - Generation aborted"+ (how?" of $how":'') +"\n"
			workMap.errors.each { rv["error"] += "\t  - $it\n" }
		} else if (workMap.embededCount>0) {
			def secs = workMap.sectionsCount && workMap.sectionsCount > 0 ? " and ${workMap.sectionsCount} preexisting sections" : ''
			rv["success"] = "... "+ workMap.embededCount +" embed codes$secs have been used to assemble $how\n"
		}
		rv
	}

	private boolean have(s) { s && s.length()>0 ? true : false }
	 
	def info(wcode=false) {
		println "\n" + ("*"*80) + "\ngart-design.json: " + appProyect +"\n" + ("*"*80)
		extensionFiles.each { println "extensionFile: \"" + it.key +"\" ( " + (it.value?.js?"with":"No") + " Javascript, " + (it.value.html?"with":"No") +" Html template/s )" }
		println "*"*80
		extensions.each {
			def e = it.value
			//[name: name, ns: ext_ns, fileType: fileType, intos: [], extensionFile: this ]
			println "EXTENSION: ${it.key}"
			e.intos.each {
				println "   INTO: "+ it.names + (it.args.size()>0?" - Args: "+it.args :"")
				if (it.asks) println "      ASKs: "+ it.asks 
				if (wcode) print "   code: "+ ("."*60) +"\n" + it.code + "<<--eot--->>\n"
				println ("."*60)
			}
		}
		println "*"*80
		extensionsByInto.each {
			println "Extensions for INTO: ${it.key}"
			it.value.each { e -> println "  ${e.ns}|${e.fileType}" }
			println ""
		}
		println "*"*80
	}
	
}


// ------------------------------------------------------------------------------------------

class ExtensionFile {
	GartExtensionManager eMan
	String fileType
	File file
	String relativePath
	String extensionFileName
	String content
	def lazy = true, loaded = false, compiled
	
	ExtensionFile(GartExtensionManager em, Resource res, String t) {
		eMan = em
		fileType = t
		file = res.file
		def s = this.file.parentFile.canonicalPath, efn = GartExtensionManager.extensionsFolderName  
		relativePath = s.substring( Math.max( s.indexOf("$efn\\"), s.indexOf("$efn/") )+efn.length()+1 )
		extensionFileName = relativePath + "/" + file.name.substring(0,file.name.lastIndexOf("."))
		if (!lazy) load()
	}
	def load() {
		if (!loaded) content = new FileInputStream(file).getText()
		loaded = true
	}
	String getContent() {
		load()
		content
	}
	def compile() {
		if(!fileType) return
		def patt = /<!--\s*EXTENSION\s*/
		if (compiled || !getContent().find(patt)) return
		def xs = getContent().split(patt)[1..-1]
		xs.each {
			def name, family, description, extension, ext_ns, intosNum = 0
			try { 
				(name, family, description) = (it.split(/-->/)[0].trim()+",").split(/,/) 
				ext_ns = "${extensionFileName}:${name}"
				extension = [name: name, ns: ext_ns, fileType: fileType, intos: [], extensionFile: this, errors: [], family: family, description: description ]
				def intos = it.split(/<!--\s*INTO\s*/)
				if (intos.size()>1) {
					intos[1..-1].each {
						LinkedList asks = []
						def all = it.split(/<!--\s*ENDINTO/)[0]
						def inames = all.split(/-->/)[0].split(/,/)*.trim() // eg [ embed1, embed2, ...]
						def args = inames.findAll{ (it ==~ /%[\w\.-]+/) }
						inames = inames - args
						def code = obtainAsks( all.substring(all.indexOf("-->")+4), asks, extension, "at INTO $inames" )
						extension.intos << [ names: inames, args: args*.toUpperCase(), code: code, asks: asks, id: ext_ns+'#'+(++intosNum) ]
						inames.each { n ->
							List l = eMan.extensionsByInto.get(n) ?: {eMan.extensionsByInto.put(n,[]); eMan.extensionsByInto.get(n)}.call()
							l << extension
						}
					}
				} else {
					extension.errors << "COMPILING extension '${extension.ns}|${extension.fileType}' do not have INTO's, should have at least one!"
				}
				String k = "${ext_ns}|${fileType}"
				eMan.extensions.put( k, extension )  
			} catch (e) {
				String ns = "${extensionFileName}:???|$fileType"
				def er = [ "COMPILING extension '$ns' invalid format! (see: name, family, description in its header)" ]
				eMan.extensions.put( (ns), [name: '???', ns: ns, fileType: fileType, extensionFile: this, errors: er ] )  
			}
		}
		compiled = true
	}
	
	private obtainAsks( String code, List asksList, extension, txtOnErr ) {
		def asks = code.split(/<!--\s*ASK\s*/) , last
		def clo = { v,i -> v.size()>i ? (v[i]!='null'?v[i]:null) : null }
		if (asks.size()>1) {
			asks[1..-1].each {
				def v = it.split(/-->/)[0].split(/^"|"[ ,]+"|"[ ,]*$/) 
				if ( clo(v,1)?:null ) {
					def x = [ symbol: (clo(v,1)?:null), prompt: (clo(v,2)?:null), value: (clo(v,3)?:null), ide: (clo(v,4)?:null) ]
					asksList << x
					last = it.split(/-->/)[1..-1].join("-->") 
				}else{
				 	extension.errors << "ASK invalid in extension ${extension.ns}|${extension.fileType} $txtOnErr"
				}  
				  
			}
		}
		trimX( last ?: code )
	} 

	private String trimX(String s) { s.replaceAll(/[\n\r]+[ \t]*/,"@!#&<>").endsWith("@!#&<>") ? s.trim()+'\n' : s.trim() }
	
}

// ------------------------------------------------------------------------------------------
// wrapper to list model to get by name (any: short or prop name) 

class Model extends LinkedList {
	Model(Collection c) { super(c) }
	def get(String n) {
		def rv, ite=iterator(), full=n.indexOf('.')>0?true:false 
		while (ite.hasNext()) {
			def x=ite.next()
			//println x.fullName +"    "+ x.shortName +"  "+ x.propertyName + "   "+ n
			if(full) { if(x.fullName==n) {rv=x; break;} } 
			else { if(x.shortName==n||x.propertyName==n) {rv=x; break;} } 
		}
		return rv
	} 
} 

