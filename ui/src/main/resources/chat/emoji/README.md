# Twemoji PNG assets (72×72)

Emoji images for the chat WebView are **not** stored in Git. They are downloaded from
[Twemoji](https://github.com/twitter/twemoji) v14.0.2 during `mvn compile` / `generate-resources`
and packaged under `chat/emoji/` in the classpath.

If emoji are missing at runtime, run a Maven build for the `ui` module (IDE-only runs need
`target/classes` populated at least once).

See the repository root `NOTICE` file for licensing (CC BY 4.0).
