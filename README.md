<blockquote><b>NOTICES:</b>
<ul>
<li>This plugin is not even a release for production. Still missing some devices to be used correctly, 
which are in full design and development.</li><br>

<li><b>This framework was intended primarily to generate code avoiding the need for GSP in the view layer. 
Thus arises the view layer is composed of artifacts deployed directly at the client side, so that among 
other things, facilitate the creation of applications for mobile devices (or hybrid) with their respective 
libraries and configuration. However it is possible to generate all, even GSP.</b></li> 
<ul>
</blockquote>


# GArt Engine, what is it?

GArt Grails plugin provides a mechanism that allows the generation of artefacts based on "Artifacts templates" and "Extensions templates". 
Gart obey the directives given by the developer right through a file named "Design Directives", which specializes each implemention 
instance of Extensions (similar to the concept of development by components). 

Artifact generation is procedures oriented. "Procedures" are a specification into "directives" of a set of logically related artefacts (at least one) 
that are generated jointly (html, js, css, etc.).

GArt script generates artefacts at development time similar to scripts 'generate-xxx'. But one of the important differences with respect to the scaffolding, 
is that you can modify the "directives" and rebuild procedures as needed, iteratively. The specificities that are part of the directives are as fine grain as are 
prescribed in the Extensions & Artefacts templates used.

Now if you ask this question: 

+ *If I edit the file (artifact) that I generated before to improve visual appearance (web-design), and then go back to generate, I lose it done ?*

  No problem with that, is allowed to provides an iterative cycle and scalable for generating artifacts.

![](https://raw.github.com/jcolombo1/gart-scripting/master/GArt-scheme.jpg)

# Roles considered in this framework

- <b>Grails developers:</b><br/>
who develops their applications in the wonderful world of Grails.

- <b>Web Designer:</b><br/>
who give the visual aesthetic touch to target artifacts (HTML/CSS). Not required knowing Grails.

<em>Moreover, this project attempts to promote the separation of functions between two important areas:
application developers and web designers.</em>


But there is another role that we want to excite:

- <b>GArt'able plugin developers:</b><br/>
Grails developers also develop GArt'able templates plugins for community use.


# GArt is for collaborative work

<b>This plugin is only a templating engine to assemble target artefacts and to provides an IDE for editing directives to generate them.</b>
 
The idea of this framework is considering that developers community can contribute and add Artefacts & Extension Templates.
So, people can create extensions for hundreds of purposes, as with the framework for Grails plugins, sharing with the community. 
In our case, are standards Grails plugins and also made for use with GArt (GArt'able template plugins).

Below you will read the guidelines for building GArt'able template plugins.

We want you to get involved in this project and welcome all feedback and opinions.

<b>Want to contribute building GArt'able plugins ? Great! :-)<b>

# GArt Templating concepts 

*<b>Artifact templates</b>*

Are templates that outline a skeleton for a type of artifact. Contains places established (<strong>embed</strong>) where extensions can insert the necessary code.
It also has an own marking tags where the designer (developer or web) can freely add code (<strong>sections</strong>).

Artifacts can be html, javascript, groovy and other files. Usually an artifact should not contain too much, just some things specific to their type, 
and mark the places for embeds and insert sections.

For files that result in HTML code the tags are like the comment tag (<!-- -->). 

```html
<!doctype html>
<html>
	<head>
		<!-- section 50 -->
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<!-- EMBED HeadIncludes, %REQUIRED -->
	</head>
	<body>
		<!-- EMBED BodyIncludes, %REQUIRED -->
	
	...
		
```

*<b>Extension templates</b>*

Extensions are files with set of code necessary to achieve the implementation of a specific behavior or commonly used process. They have a specific 
marking to indicate where each piece of code will reside (called "into"). A "<b>into</b>" is embedded within an "<b>embed</b>". 

As Artifacts, extensions may also contain "embeds", this makes recursive. It may also contain "section" tags where add code freely. 

Within a "into" there is a marked called "ask", that provides the ability to give specific instance parameters to built embed code (eg domains, 
properties, behavior options, etc).

Extensions are the flowers of this garden. They provide the highest sense of this paradigm.

```html
<!-- EXTENSION GartDemo	-->

<!-- INTO HeadIncludes, %ONCE -->
	<!-- ASK "JQueryLocation", "JQuery location", "cdn" -->
<%    
	def cdn = 'http://code.jquery.com/jquery-1.7.1.min.js'
	def local = GART.path_js + '/jquery/jquery-1.7.1.min.js'
%>

<script src="${ ASK.JQueryLocation=='cdn' ? cdn : local }"></script>

<!-- ENDINTO -->

    ... others "Into" ... 

```

  It seems strange, but do not despair, then you will find it very easy to understood conventions.

*<b>Design Directives file</b>*

This is like a cooking recipe. Is used internally by the GArt engine to know how to build artifacts. 
Contains the specification of each artefacts procedure to generate, which extensions are included in each artifact 
and specifying instance extension parameters.  It's edited by Grails developer. 

There is only one directives file for the entire application. Need not contain all 
the elements generated in application, only those that developer're interested. Is expected to coexist with the project throughout its full life cycle.

Its format is JSON, making it easy to store in non-relational databases (IDE is in the kitchen!).

# Where templates will reside 

Take a look the folder where the templates must be residing, in paths: 
<code>/src/gart/templates/artefacts</code> (for artefacts)<br> 
<code>/src/gart/templates/extensions</code> (for extensions)

```
	src
	+	gart
		+	templates
			+	artefacts
			|		<your branding folder>
			|			<artefacts files>
			+	extensions
					<your branding folder>
						<extensions files>

```	
# Get starting building GArt'able templates

1- Create a Grails plugin eg called 'my-gartable-first' :

```groovy
grails create-plugin my-gartable-first
```

2- Create necessary paths listed above, and add into each your personal brand folder (which differentiate your plugin with 
its namespace), in my case I called it "jcolombo".  			
 			
3- Into ".../artefacts/[your-brand]" folder create an artefact called "first.html" with this content:
 
-->> SOON TO BE CONTINUED <<--
  			
# Usage 

```groovy
grails gart MyProcedureName
```

# TO DO

