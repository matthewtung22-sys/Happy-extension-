rootProject.name = "Happy-extension-"

file("src").listFiles()?.forEach { langDir ->
    if (langDir.isDirectory) {
        langDir.listFiles()?.forEach { extDir ->
            if (extDir.isDirectory && File(extDir, "build.gradle").exists()) {
                include(":src:${langDir.name}:${extDir.name}")
            }
        }
    }
}
