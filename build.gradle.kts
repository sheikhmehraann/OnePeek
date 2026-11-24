plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.spotless)
}

fun runGitCommand(vararg args: String): String? =
    runCatching {
        val process = ProcessBuilder(*args)
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        check(process.waitFor() == 0) { output }
        output
    }.getOrNull()

val commitHash = runGitCommand("git", "rev-parse", "--short", "HEAD") ?: "dev"
val commitCount = runGitCommand("git", "rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1
val exactTag = System.getenv("ONEPEEK_VERSION_NAME")
    ?.takeIf { it.isNotBlank() }
    ?.removePrefix("v")
    ?: runGitCommand("git", "describe", "--tags", "--exact-match")?.removePrefix("v")

extra["gitCommitId"] = commitHash
extra["gitCommitCount"] = commitCount
extra["appVersionName"] = exactTag ?: "0.1.0-dev.$commitCount"

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX

    kotlin {
        target("**/src/*/java/**/*.kt", "**/src/*/kotlin/**/*.kt")
        targetExclude("**/build/**")
        ktlint().editorConfigOverride(
            mapOf("ktlint_standard_max-line-length" to "disabled"),
        )
    }
}
