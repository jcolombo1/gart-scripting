//
// This script is executed by Grails after plugin was installed to project.
// This script is a Gant script so you can use all special variables provided
// by Gant (such as 'baseDir' which points on project base dir). You can
// use 'ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
//
//    ant.mkdir(dir:"${basedir}/grails-app/jobs")
//

/**
 *
 * @author <a href='mailto:jcolombo@ymail.com'>Jorge Colombo</a>
 *
 * @since 0.1
 */
 
	ant.mkdir ( dir: "${basedir}/src/gart/templates/artefacts" )
	ant.mkdir ( dir: "${basedir}/src/gart/templates/extensions" )
	ant.mkdir ( dir: "${basedir}/src/templates/scaffolding" )
	
	ant.copy  ( todir: "${basedir}/src/gart/templates/artefacts", overwrite: true ) {
	    fileset dir: "${pluginBasedir}/src/gart/templates/artefacts"
	}
	
	ant.copy  ( todir: "${basedir}/src/gart/templates/extensions", overwrite: true ) {
		fileset dir: "${pluginBasedir}/src/gart/templates/extensions"
	}
	
	ant.copy  ( todir: "${basedir}/src/templates/scaffolding", overwrite: false ) {
		fileset dir: "${pluginBasedir}/src/templates/scaffolding"
	}

	ant.copy(file: "${pluginBasedir}/src/gart-design.json", todir: "${basedir}/src", overwrite: false )
	
	
println """\

*********************************************************

                          GART

             Templating Engine of Assemble  

           Thanks for installing GART Plugin
Artifacts  are localized in /src/gart/templates/artifacts
Extensions are localized in /src/gart/templates/extensions

                   ... have fun ! ...

               JAC - jcolombo@ymail.com

*********************************************************
"""


