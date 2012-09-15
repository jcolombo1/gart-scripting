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
No problem, Gart contemplates that in a very simple way for the developer, which gives an iterative cycle and scalable for generating artefacts.

![](https://raw.github.com/jcolombo1/gart-scripting/master/GArt-scheme.jpg)

# GArt Engine parts

TO DO

# Usage

:

```groovy
grails gart MyProcedureName
```

