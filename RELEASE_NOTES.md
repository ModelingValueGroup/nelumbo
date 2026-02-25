To start the **nelumbo** editor:

- install java (version 21 or higher, if not yet installed)
- download `nelumbo-0.9.6-editor.jar`
- in your command line environment run `java -jar nelumbo-0.9.6-editor.jar`

Always start a **nelumbo** specification with an import-statement. For example:
```
import    nelumbo.strings

String  a

"foo"+"bar"=a  ?    [(a="foobar")][..]
```
Query results are calculated and shown on the fly.
This editor is not an LSP-based editor, and useful for educational and demo purposes.
