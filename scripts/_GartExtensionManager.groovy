import java.io.File;
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
	
	gartEM = GartExtensionManager.getInstance("$basedir")	// setting basedir for load Extension Files
	if (!grailsApp) {
		event 'StatusError', ["Before 'GartExtensionManager.getInstance' you must initialize scripting scope (grailsApplication not setted)"]
		exit 1
	}
	gartEM.grailsApp = grailsApp
	 
}

setDefaultTarget(createGartEM)



// ------------------------------------------------------------------------------------------

class GartExtensionManager {
	
	private static GartExtensionManager me 
	protected static extensionsFolderName = 'extensions'
	protected static artefactsFolderName = 'artefacts'
	protected static extensionsPath = "src/gart/templates/$extensionsFolderName"
	protected static artefactsPath = "src/gart/templates/$artefactsFolderName"
	//protected static webDesignPath = "src/web"
	static appProyecFileName = 'src/gart-design.json' 
	static baseDir
	
	boolean loadedFiles
	Map extensionFiles = [:]  // [ extensionFileName: [ js: ExtensionFile, html: ExtensionFile ] ]
	Map extensions = [:]
	Map extensionsByInto = [:]
	def appProyect
	def compileOnLoad = false
	def workMap = [errors: []]
	def grailsApp
	def classLoader
	def pluginManager
	
	GartExtensionManager(String baseDirectory=null) {  
		if (baseDirectory) loadExtensionFiles(baseDirectory)  
	}
	
	static synchronized GartExtensionManager getInstance(baseDirectory) {
		if (!me) me = new GartExtensionManager(baseDirectory)
		me
	}
	
	def loadExtensionFiles(baseDirectory, boolean compile = false) { 
		if (loadedFiles) return
		compileOnLoad = compile
		baseDir = baseDirectory
		def resourcesHtml = [], resourcesJS = []
		def resolver = new PathMatchingResourcePatternResolver()
		def templatesDirPath = "${baseDir}/${extensionsPath}"
	
		def templatesDir = new FileSystemResource(templatesDirPath)
		if (templatesDir.exists()) {
		  try {
			resourcesHtml.addAll(resolver.getResources("file:$templatesDirPath/**/*.html"))
			resourcesJS.addAll(resolver.getResources("file:$templatesDirPath/**/*.js"))
		  } catch (e) {
			event 'StatusError', ['Error while loading extensions from extensions folder', e]
		  }
		}
		
		// FIXME : enhancement debería ser tambien para templates .gsp y .groovy (todo el equipo completo :)
		resourcesJS.each {
			ExtensionFile ex = new ExtensionFile(this, it, "js")
			extensionFiles.put ex.extensionFileName, [ js: ex, html: null ]
		}
		resourcesHtml.each {
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
		extensionFiles.each {
			if (it.value.js) it.value.js.compile()
			if (it.value.html) it.value.html.compile()
		}
		println extensionFiles.size() + " extension files compiled (there are "+ extensions.size() + " extensions)"
	}
	
	/**
	 * Returns a List with Procedure names (descriptions & last date will be appended if needed).<br>
	 * @param order 'alfa' | 'priority' | default by date (desc)<br>
	 * @param withDesc false returns keys, true returns descriptive string (key - description - date)  
	 */
	def getProcedureNames( withDesc = true, order = 'date' ) {
		def procs = appProyect["procedures"].entrySet()
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
		def modelNames = [], model = []
		procedure.artefacts?.each { it.value?.extensions?.each { _drill4m(modelNames,it) } }
		modelNames.each {  
			def domain = it.indexOf('.') > 0 ? it : GrailsNameUtils.getClassNameRepresentation(it)
			model << (grailsApp.getDomainClass(domain))
		}
		model
	}

	def assembleProcedure(String procedureName) {
		
		def procedure = appProyect["procedures"]["${procedureName}"]
		
		if (!procedure) { workMap.errors << "Procedure $procedureName not found!" ; return }
		if (!procedure.artefacts) { workMap.errors << "Procedure $procedureName has no artefacts defined!" ; return } 

		def model = new Model( getModelForProcedure(procedure) )
		  
		def sufixName = have(procedure.sufixName) ? '-'+procedure.sufixName : ''
		def relPath = have(procedure.relPath) ? '/'+procedure.relPath : ''
		
		
		procedure.artefacts?.entrySet().sort{ (it.value.priority?:0) }.each {    // artefacts Map - order by artefact.priority if indicated
			
			def templateViewName = it.key 
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

			workMap = [ procedureName: procedureName, errors: [], embededCount: 0, templateText: new FileInputStream(file).getText(), domainClass: model[0], templateViewName: templateViewName, tfile: tfile, extInstanceNum: 0, intosUsed: [], reclaimsID: [] ]
			
			replacer( it.value )
			
			def toRemove = []
			workMap.templateText.findAll(/<!--\s*EMBED\s*[\w%\,\s\.-]*\s*-->/).each {
				if ( it ==~ /<!--\s*EMBED\s*[[\w\.-]+[,\s]+]+%REQ(UIRE)*D*\s*-->/ )	workMap.errors << "EMBED '${it}' is required into view ${workMap.templateViewName} for procedure ${procedureName}"
				else toRemove << it
			}
			toRemove.each { workMap.templateText = workMap.templateText.replaceAll((it+"\\s*"),"") }  // remove marks (remove all EMBEDs checking that are required)
			
			if (workMap.errors.size()>0) return
			
			
			// ---- no assembly errors, prepare and run the resulting script and write target file    
			
			workMap.templateText = workMap.embededCount > 0 ? "<% def ASK,EXT %>" + workMap.templateText : workMap.templateText

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
			
			def rec = { id, type='div' -> workMap.reclaimsID << ('{id:"'+id+'",type:"'+type+'"}'); '"'+id+'"' }
			
			def templateGenerator = new GartTemplateGenerator(classLoader, workMap)
			templateGenerator.grailsApplication = grailsApp
			templateGenerator.pluginManager = pluginManager
			templateGenerator.beforeMakeFile = { bindings -> bindings << [ ME : binds, GART : garts, REC : rec ] }
			templateGenerator.generateFile( model, targetFPath )

			//println "Procedure --> $procedureName - ${procedure.description} - Domain Model: " + model + " - Template :  " +templateViewName + "  - tfile: "+ tfile
		}
	}

//	/**
//	 * This methods is only called from 3rd parties scrips in order to support 
//	 * the traditional way that Grails and plugins generates artifacts (scaffolding oriented domains).
//	 * This is not how Gart generates artifacts and documents (templates assembly oriented to procedures), see: <b><code>assembleProcedure()</code></b> method.
//	 */
//	def assembleTemplate(GrailsDomainClass domainClass, String templateViewName, String templateText) {
//		String tfile = templateViewName.find(/\.\w+$/).substring(1).toLowerCase()
//		workMap = [ errors: [], embededCount: 0, templateText: templateText, domainClass: domainClass, templateViewName: templateViewName, tfile: tfile, extInstanceNum: 0, intosUsed: [] ]
//		if (appProyect) {
//			//println "MERGE --> "+ domainClass.fullName + " - VIEW:  " +templateViewName + "  - tfile: "+ tfile
//			try { replacer( appProyect["domains"]["${domainClass.fullName}"]["artefacts"]["${templateViewName}"] ) } catch(e) {}
//			
//			// remove all EMBEDs checking that are required
//			def toRemove = []
//			workMap.templateText.findAll(/<!--\s*EMBED\s*[\w%\,\s\.-]*\s*-->/).each {
//				if ( it ==~ /<!--\s*EMBED\s*[[\w\.-]+[,\s]+]+%REQ(UIRE)*D*\s*-->/ )	workMap.errors << "EMBED '${it}' is required into view ${workMap.templateViewName} for ${workMap.domainClass.fullName}"
//				else toRemove << it
//			}
//			toRemove.each { workMap.templateText = workMap.templateText.replaceAll((it+"\\s*"),"") }  // remove marks
//		}else {
//			workMap.errors << "Missing application file specification with templates assembly directives ($appProyecFileName) !" 
//		}	
//		if (workMap.errors.size() > 0) return null
//		return workMap.embededCount > 0 ? "<% def ASK,EXT %>\n" + workMap.templateText : workMap.templateText
//	}

	private replacer(extMap, deep=0) {	// static
		List extsList = extMap["extensions"]
		if (!extsList) return
		def how = workMap.procedureName ? "procedure $workMap.procedureName" : "domain ${workMap.domainClass.fullName}"
		def toRemove = []
		try {
			extsList.sort{ (it.priority?:0) }.each { proj_ext ->
				def ext = getExtension( proj_ext.name, workMap.tfile )
				if (!ext) { workMap.errors << "extension '${proj_ext.name}.${workMap.tfile}' not found!" }
				else if (ext.errors?.size()>0) { workMap.errors.addAll ext.errors }
				else {
					proj_ext.instance = proj_ext.instance ?: ( ++workMap.extInstanceNum > 1 ? "_${workMap.extInstanceNum}" : '') // tail from "_2" (_2..n) 
					ext.intos.each { eInto ->
						eInto.names.each { embedName ->
							if (!eInto.args.contains('%ONCE') || !workMap.intosUsed.contains(eInto.id)) { //was embedded a %ONCE-into?
								def patt = "<!--\\s*EMBED\\s*"+ embedName +"[\\w%\\,\\s\\.-]*-->", fstr
								if ( (fstr = workMap.templateText.find(patt)) ) {
									def pos = workMap.templateText.indexOf(fstr)
									def indent = '\n' + (workMap.templateText.find("\\t* *"+patt) - fstr)
									def code = "<% EXT='${proj_ext.instance}' %>" + indenterX( injectAsk2Code(eInto, proj_ext.intos?."$embedName"), indent )
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
	
	private injectAsk2Code( into, emb_proy ) {
		def a = [:]; String s=""
		into."asks".each { a.put( it.symbol, it.value ) }
		a.putAll ( emb_proy?."asks" ? ( emb_proy?."asks".findAll { a.containsKey(it.key) } ) : [:] )  // set w/projects values
		if (a.size()<1) return "<% ASK = [:] %>" + into.code
		a.each {
			def val
			if (it.value instanceof String && it.value.matches(/-?[\d]*.?[\d]+/)) val = it.value  // is numeric?
			else val = it.value=='true' ? 'true' : ( it.value=='false' ? 'false' : ( it.value==null ? 'null' : "'${it.value}'" ) ) // SPECIAL CASES true|false|null   
			s += "'${it.key}': $val, " 
		}
		"<% ASK = ["+ s +"] %>" + into.code
	}
	
	private getExtension(String name, String tfile) {
		def ext = extensions.get( "${name}.${tfile}" )  // compound key eg: "basic.SampleEmbed:MyEmbed.js"
		if (ext) return ext
		try { extensionFiles[(name.split(/:/)[0])]."${tfile}".compile() } catch (e) {  }  // compile on-demand
		return extensions.get( "${name}.${tfile}" )
	}
		
	private loadAppProyect() {
		def file
		String fn = appProyecFileName  
		if (!(file=new File(fn).exists())) { fn=fn.replaceFirst(/\.\w+$/,'.js'); file=new File(fn) }   // .json | .js -> both permitted  
		if (file.exists()) {
			def slurper = new groovy.json.JsonSlurper()
			appProyect = slurper.parseText( new FileInputStream(file).getText() )[0]
//			def builder = new groovy.json.JsonBuilder()
//			builder.setContent(appProyect)
//			println builder.toString()
		}
	}

	def printWorkResult() {
		def res = getWorkResult()
		if (res.error) println res.error else if (res.success) println res.success
	}
	
	def getWorkResult() {
		def rv = [:]
		def how = !workMap?.templateViewName ? null : ( workMap?.procedureName ? 'Procedure '+workMap?.procedureName : 'view '+workMap.templateViewName )
		if (workMap.errors.size()>0) {
			rv["error"] = "\t>THERE ARE "+ workMap.errors.size() +" ERRORS - Generation aborted"+ (how?" of $how":'') +"\n"
			workMap.errors.each { rv["error"] += "\t  - $it\n" }
		} else if (workMap.embededCount>0) {
			def secs = workMap.sectionsCount && workMap.sectionsCount > 0 ? " and ${workMap.sectionsCount} preexisting sections" : ''
			rv["success"] = "\t> "+ workMap.embededCount +" embed codes$secs has been used for $how\n"
		}
		rv
	}

	private boolean have(s) { s && s.length()>0 ? true : false }
	 
	def info(wcode) {
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
			it.value.each { e -> println "  ${e.ns}.${e.fileType}" }
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
		extensionFileName = relativePath + "." + file.name.substring(0,file.name.lastIndexOf("."))
		if (!lazy) load()
	}
	def load() {
		if (!loaded) content = new FileInputStream(file).getText('utf8')
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
			def name = (it.split(/-->/)[0].trim()+",").split(/,/)[0] // 1st arg
			// ****** parsear argumentos a0 : nombre, etc
			def ext_ns = "${extensionFileName}:${name}", intosNum = 0
			def extension = [name: name, ns: ext_ns, fileType: fileType, intos: [], extensionFile: this, errors: [] ]
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
				extension.errors << "EXTENSION '${extension.ns}.${extension.fileType}' do not have INTO's, should have at least one!"
			}
			eMan.extensions.put "${ext_ns}.${fileType}", extension  
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
				 	extension.errors << "ASK invalid in extension ${extension.ns}.${extension.fileType} $txtOnErr"
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
	Model(List c) { super(c) }
	def get(String n) {
		def rv, ite=iterator() 
		while (ite.hasNext()) {
			def x=ite.next()
			if(x.shortName||x.propertyName) {rv=x; break;} 
		}
		return rv
	} 
} 

