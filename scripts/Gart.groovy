import java.awt.event.ItemEvent;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.scaffolding.*    // IMPORTANT !!


//includeTargets << grailsScript("_GrailsCreateArtifacts")
//includeTargets << grailsScript("_GrailsGenerate")
//includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsInit")

includeTargets << new File("${gartScriptingPluginDir}/scripts/_GartExtensionManager.groovy")
includeTargets << new File("${gartScriptingPluginDir}/scripts/_GartTemplateGenerator.groovy")

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

	//depends(checkVersion, parseArguments, packageApp)
	
	depends(loadApp, compile, createGartEM)

	def name = argsMap["params"][0]

	gartEM = GartExtensionManager.getInstance()
	
	if ( ! gartEM.getProcedureNames(false).contains(name) ) {
		def result = [ error: "Procedure '$name' not found!\n"+ help() , exit: 1 ]
		displayResult(result)
	}

	gartEM.classLoader = classLoader
	gartEM.pluginManager = pluginManager
	
	gartEM.assembleProcedure(name)
	
	displayResult(gartEM.getWorkResult())
	
}

setDefaultTarget(gartRun)

// -------------------------------------------------------------------------------------

displayResult = { result ->
	if (result) {
		if ( result.error )
			event 'StatusError', [ "$iam : " + result.error ]
		else
			event 'StatusFinal', [ "$iam : "+ result.success ]
			
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

