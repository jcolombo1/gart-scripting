# GArt Engine, what is it?

GArt Grails plugin provides a mechanism that allows the generation of artefacts based on "templates" and "extensions" that are lots 
of code commonly used as procedure portions within lists, edit forms, reports, graphics, and many other possible UI cases. 
It is also based on a directives file, which specializes implementing parties in each procedure. "Procedure" is the complete specification of a set 
of logically related artefacts that are generated jointly (html, js, css, etc.).

As with the scripts 'generate-xxx', this script generates artefacts at development time. But not only generate but also reflects the particular 
design that the developer has specified in the directives file. Thus, you can modify the directives and rebuild the procedure, iteratively.

The specificities that are part of the directives are as fine grain as are prescribed in the "extensions" and "artefacts".
So that, for example, you could draw a list where each column shall choose it up, and also, chaining their content according to 
their parent selector (eg books by an author). If you think you need another column, add it in the directives file and rebuilds the procedure.
You do not need to edit the artifact for that, just add the directive.

Now you might ask: But if I modify a artifact to improve visual appearance (web-design), and then go back to generating the procedure, I lose it done?
No problem, GArt contemplates that in a very simple way for the developer, which gives an iterative cycle and scalable for generating artefacts.

![](https://raw.github.com/jcolombo1/gart-scripting/master/GArt-scheme.jpg)


# GArt is for collaborative work

The idea of this framework was designed so that the developer community can contribute and add Artefacts & Extension Templates. 
Here is the engine, but we need those ...

It seeks a scheme similar to what happens with the "Grails plugins" to expand the possibilities for developers. We intend that you 
and other build plugins with Artefacts & Extension collaboratives with GArt.

We want you to get involved in this project !

Later you will see the guidelines for building plugins with Artefacts & Extension.

# GArt Engine parts

*<b>Artifact templates</b>*

Are templates that outline a skeleton for a type of artifact. Contains places established (<strong>embed</strong>) where extensions can insert the necessary code.
It also contains a special marking where the designer (developer or web) can freely add code (<strong>sections</strong>).

Artifacts can be html, javascript, groovy and other files. Usually an artifact should not contain too much, just some things specific to their type, 
and mark the places for embeds and insert sections. The rest is up to the extensions that are designed for this artifact.

*<b>Extension templates</b>*

Extensions are files with set of code necessary to achieve the implementation of a specific behavior or commonly used process. They have a specific 
marking to indicate where each piece of code will reside (called "into"). A "into" is embedded within an "embed". 

Like Artifacts, extensions may also contain "embeds", this makes recursive. They may also contain "sections" to freely design code. 

Within a "into" there is a marked called "ask" that provides the ability to give specific parametric elements to built embed code (eg domains, 
properties, behavior options, etc).

Extensions are the flowers of garden providing the highest meaning of this paradigm.

*<b>Design Directives file</b>*

The design directives file contains the specification of each procedure to generate. Says what artefacts should be built, which 
extensions are included in each artifact, and even specifies the parameters and information requested in the "ask" of each "into" to embed.

There is only one directives file for the entire application. Within it reside all specifications for each procedure.
 
MORE TO DO

# Usage

:

```groovy
grails gart MyProcedureName
```

