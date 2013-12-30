import groovy.text.SimpleTemplateEngine;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsTemplateGenerator
import org.codehaus.groovy.control.CompilationFailedException
//import org.springframework.core.io.AbstractResource


// ------------------------------------------------------------------------------------------


target(intiGartTG: "NONE") { }

setDefaultTarget(intiGartTG)

class GartTemplateGenerator extends DefaultGrailsTemplateGenerator {
	
	def beforeMakeFile

	private Map workMap
	
	private boolean oldExists
	private String oldContentFile, newContentFile
	private File destFile
	
	GartTemplateGenerator(ClassLoader classLoader, Map workMap) {
		super(classLoader)
		workMap.put("sectionsCount", 0)
		this.workMap = workMap 
	}

	void generateFile(List domainModel, String destFullName) {
		destFile = new File(destFullName)
		if((oldExists = destFile.exists())) oldContentFile = new File(destFullName).getText()
		
		generate( domainModel )
		
		if ( workMap.errors.size() == 0 ) 
			destFile.withWriter('UTF-8') { Writer writer -> writer.write( newContentFile ) }
	}

	void generate(List domainModel) {
		def domainClass = domainModel[0]

		if(workMap.templateText) {
			def t = engine.createTemplate(workMap.templateText)
			def multiPart = []  
			domainModel.each { multiPart << it.properties.any { it.type == ([] as Byte[]).class || it.type == ([] as byte[]).class } }
			boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
			def packageName = domainClass?.packageName ? "<%@ page import=\"${domainClass.fullName}\" %>" : ""
			def binding = [
					pluginManager: pluginManager,
					grailsApplication: grailsApplication,
					packageName: packageName,
					domainClass: domainClass,
					multiPart: multiPart,
					className: domainClass?.shortName,
					propertyName:  domainClass ? getPropName(domainClass) : '' ,
					//renderEditor: renderEditor,
					MODEL:domainModel
			]

			if (beforeMakeFile) beforeMakeFile( binding ) // to append other bindings from caller
			  
			try {
				newContentFile = t.make(binding).toString()	// make target file content
			} catch (e) {
				workMap.errors << ("Compiler Error >> "+ e.message)
				return
			}	
			
			if ( workMap.tfile == 'html' && workMap.reclaimsID.size() > 0 ) {
				def fd = newContentFile.find("</[hH][tT][mM][lL]>")
				if (fd) newContentFile = newContentFile.replace(fd, _reclaimsJSCode()+'</html>')
			} 
			
			if (oldExists) {		

				def sections = oldContentFile.split(/<!--\s*section\s+/) 
				if (sections.size()>1) {
					def toInc = [:] , fd, cont
					sections[1..-1].each {
						def sec
						try { sec = new Integer(it.split(/\s*-->/)[0]) } catch(e) { println "err $e in $it"}
						cont = it.substring(it.indexOf("-->")+3)
						if ((fd = cont.find(/<!--\s*\/\s*-->/)))  toInc.put( sec, cont.substring(0,cont.indexOf(fd)) )
					}
					toInc.each { k, v ->
						if ((fd = newContentFile.find("<!--\\s*section\\s+"+k+"\\s*-->"))) {
							cont = fd + v + "<!-- / -->"
							newContentFile = newContentFile.replace(fd,cont)
							//println "Replace seccion $k"
							workMap.sectionsCount++
						}else {
							workMap.errors << "Mark \"<!-- section $k -->\" not found to merge the existing section in "+destFile.absolutePath +" (suggestions: '${workMap.templateViewName}' changed, or section number $k does not exist in it)"
						}
					}
				}
			}
		}
	}

	private String getPropName(GrailsDomainClass domainClass) { "${domainClass.propertyName}${domainSuffix}" }
	
	private String _reclaimsJSCode() {
		def req = 'Estimated Designer, I need you to add and locate the following elements into html:'
		def alert = """
	var reclaims='', nojq=function(id){return id.charAt(0)=='#'?id.slice(1):id};
	for (i=0;i<designTips.needed.length;i++) {
		if (!document.getElementById(nojq(designTips.needed[i].id))) reclaims+=designTips.needed[i].id+'  [ '+designTips.needed[i].type+' ]\\n\\t';
	}
	if (reclaims!='') alert('$req\\n\\n\\t'+reclaims);
""" 
		def js = 'var designTips={needed: new Array('+ workMap.reclaimsID.join(',') +')};\n' 
		js += "\$(document).ready(function(){$alert});\n"
		return "<script type='text/javascript'>\n$js</script>\n"
	}
}

