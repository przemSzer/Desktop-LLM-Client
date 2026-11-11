# KaTeX (math rendering)

KaTeX for the chat WebView is **not** stored in Git. It is downloaded from
[KaTeX](https://github.com/KaTeX/KaTeX) v0.19.1 during `mvn compile` / `generate-resources`
and packaged under `chat/katex/` in the classpath.

`chat.html` loads `katex.min.js`, `katex.min.css`, and `contrib/auto-render.min.js`.
CSS loads the fonts next to itself from `fonts/`.

If formulas stay as raw `$$`, run a Maven build for the `ui` module (IDE-only runs need
`target/classes` populated at least once).

See the repository root `NOTICE` file for licensing (MIT).
