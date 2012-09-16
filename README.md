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

+ *If I modify an artefact to improve visual appearance (web-design), and then go back to generating its procedure, I lose it done ?*

  No problem with that, is allowed to provides an iterative cycle and scalable for generating artifacts.

![](https://raw.github.com/jcolombo1/gart-scripting/master/GArt-scheme.jpg)

# GArt is for collaborative work

The idea of this framework was designed so that the developer community can contribute and add Artefacts & Extension Templates.
So, people can create extensions for hundreds of purposes, as with the framework for Grails plugins, sharing with the community. 
In our case, are standards Grails plugins and also made for use with GArt (GArt' compliance).

Below you will read the guidelines for building Artefacts & Extension plugins.

We want you to get involved in this project :-) and welcome all feedback and opinions.


# GArt Engine parts

*<b>Artifact templates</b>*

Are templates that outline a skeleton for a type of artifact. Contains places established (<strong>embed</strong>) where extensions can insert the necessary code.
It also contains a special marking where the designer (developer or web) can freely add code (<strong>sections</strong>).

Artifacts can be html, javascript, groovy and other files. Usually an artifact should not contain too much, just some things specific to their type, 
and mark the places for embeds and insert sections.

In HTML files marks are realized as the comment tag (<!-- -->). 

```html
<!doctype html>
<html>
	<head>
		<!-- section 50 -->
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<!-- EMBED HeadIncludes, %REQUIRED -->
	</head>
	<body>
		<h1>${ME.description}</h1>
	...
		
```

  Below seen in depth each type of marking.

*<b>Extension templates</b>*

Extensions are files with set of code necessary to achieve the implementation of a specific behavior or commonly used process. They have a specific 
marking to indicate where each piece of code will reside (called "into"). A "into" is embedded within an "embed". 

As Artifacts, extensions may also contain "embeds", this makes recursive. It also contains "sections" to add code freely. 

Within a "into" there is a marked called "ask", that provides the ability to give specific instance parameters to built embed code (eg domains, 
properties, behavior options, etc).

Extensions are the flowers of this garden. They provide the highest sense of this paradigm.

```groovy
<!-- 				EXTENSION GartDemo 					-->

<!-- ================================================== -->
<!-- 				INTO HeadIncludes, %ONCE	 	    -->

	<!-- ASK "JQueryLoc", "JQuery location", "cdn" -->
<%    
	def cdn = 'http://code.jquery.com/jquery-1.7.1.min.js'
	def cdn2 = 'http://code.jquery.com/ui/1.8.23/jquery-ui.min.js'
	def local = GART.path_js + '/jquery/jquery-1.7.1.min.js'
	def local2 = GART.path_js + '/jquery/ui/1.8.23/jquery-ui.min.js' 
%>

<script src="${ ASK.JQueryLoc=='cdn' ? cdn : local }"></script>
<script src="${ ASK.JQueryLoc=='cdn' ? cdn2 : local2 }"></script>

<!-- ENDINTO -->

    ... others "Into" ... 

```

  It seems strange, but do not despair, then you will find it very easy to marked.

*<b>Design Directives file</b>*

This file contains the <b>specification of each procedure</b> to generate. Says what artefacts should be built, which 
extensions are included in artifacts, and even specifies the instance parameters and information requested in "ask"s of each "into" to embed.

There is only one directives file for the entire application. Within it reside all specifications for each procedure.

Need not contain all the elements generated in application, only those that you're interested.

Its format is JSON, making it easy to store in non-relational databases (will in future release).

```javascript
[
  {
    "procedures": {
      "Index": {
        "description": "Welcome to GArt Plugin demo",
        "sufixName": "",
        "relPath": "",
        "artefacts": {
          "basic/gart-index.html": {
            "extensions": [
              {
                "name": "basic.gart-demo:GartDemo",
                "priority": 1,
                "domains": {
                  "gartDemo": "gart.demo.GartDemo"
                },
                "intos": {
                  "HeadIncludes": {
                    "asks": {
                      "JQueryLoc": "cdn",
                      "instance": 1,
                      "domain": "gart.demo.GartDemo"
                    }
                  },

		...
```


# Usage

TO DO

:

```groovy
grails gart MyProcedureName
```

