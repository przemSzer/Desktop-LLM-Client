# Mermaid (diagrams)

Mermaid for the chat WebView is **not** stored in Git. It is downloaded from
the npm package [mermaid](https://www.npmjs.com/package/mermaid) v10.9.5 during
`mvn compile` / `generate-resources` and packaged as `chat/mermaid/mermaid.min.js`.

`chat.html` renders a fenced `mermaid` block after the message HTML is inserted.
An unfinished fence stays as source while the reply is still streaming. The diagram
is drawn once, after the message is committed.

Version 10.9.5 is the last 10.x release. Its `mermaid.min.js` is one browser
script. Pie charts and XY bar/line charts (`xychart-beta`) are included.

See the repository root `NOTICE` file for licensing (MIT).
