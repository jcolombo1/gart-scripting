import java.awt.event.ItemEvent;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.scaffolding.*    // IMPORTANT !!


//includeTargets << grailsScript("_GrailsCreateArtifacts")
//includeTargets << grailsScript("_GrailsGenerate")
//includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsInit")

includeTargets << new File("${gartScriptingPluginDir}/scripts/_GartCloudHelper.groovy")
includeTargets << new File("${gartScriptingPluginDir}/scripts/_GartTemplateGenerator.groovy")
includeTargets << new File("${gartScriptingPluginDir}/scripts/_GartExtensionManager.groovy")

def iam = "GArt"

gartEM = null

/**
 * Generate corresponding artefacts defined in the underlying procedure
 *
 * @author Jorge Colombo
 *
 * @since 0.1
 */


target(gartRun: "Generates Procedure artefacts") {

	depends(checkVersion, parseArguments, loadApp, createGartEM )
	
	if (!gartEM.haveError()) {

		gartEM.classLoader = classLoader
		gartEM.pluginManager = pluginManager

		def name = argsMap["params"][0]
		
		if (name=='ide') {
			depends send2Cloud
			return
		}
	
		depends check2Cloud
		
		if ( ! gartEM.getProcedureNames(false).contains(name) ) {
			def result = [ error: "Procedure '$name' not found!\n"+ help() , exit: 1 ]
			displayResult(result)
		}
	
		gartEM.assembleProcedure(name)
	}
	
	displayResult(gartEM.getWorkResult())
	
}

setDefaultTarget(gartRun)

// -------------------------------------------------------------------------------------

displayResult = { result ->
	if (result) {
		if ( result.error )
			event 'StatusError', [ "$iam : " + result.error ]
		else
			event 'StatusFinal', [ "$iam : "+ result.success + (result.lap?" in ${result.lap} segs.":'') ]
			
		if (result.exit)
			exit result.exit
	}
}

// -------------------------------------------------------------------------------------

help = {
	def tips = []
	gartEM.getProcedureNames().each { tips << it }
	tips.size()>0 ? "\nYou have the following Procedures:\n\n  > " + tips.join("\n  > ") : "\n\t(You still have not defined Procedures. You must define at least one.)"
}

