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

extra["gitCommitId"] = runGitCommand("git", "rev-parse", "--short", "HEAD") ?: "dev"
extra["gitCommitCount"] = runGitCommand("git", "rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1

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
