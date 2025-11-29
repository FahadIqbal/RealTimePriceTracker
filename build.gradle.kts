// Root build script
plugins {
    // versions aligned for stability with Compose and Serialization
    id("com.android.application") version "8.3.2" apply false
    kotlin("android") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
}

// >> GIT_ALL_IN_ONE:startApi
tasks.register<Exec>("startApi") {
    workingDir = rootProject.projectDir
    val os = org.gradle.internal.os.OperatingSystem.current()
    if (os.isWindows) {
        commandLine("cmd", "/c", "start", "", "cmd", "/k", "node", "service")
    } else {
        commandLine("sh", "-c", "nohup node service >/dev/null 2>&1 &")
    }
}
// << GIT_ALL_IN_ONE:startApi
