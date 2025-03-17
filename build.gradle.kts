//import java.time.Instant
//import java.time.ZoneOffset
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//
//plugins {
//    id("java-library")
//    id("maven-publish")
//    id("me.champeau.jmh").version("0.7.2") // requires gradle 7.0+
//}
//
//group = "com.esaulpaugh"
//version = "13.1.2-SNAPSHOT"
//
//java {
//    sourceCompatibility = JavaVersion.VERSION_1_8
//    targetCompatibility = JavaVersion.VERSION_1_8
//}
//
//tasks.withType<JavaCompile> {
//    if (JavaVersion.current() >= JavaVersion.VERSION_1_10) {
//        println("setting release 8")
//        options.release.set(8)
//    }
//    options.encoding = "US-ASCII"
//}
//
//tasks.withType<Test> {
//    maxParallelForks = Runtime.getRuntime().availableProcessors()
//    useJUnitPlatform()
//}
//
//val sourcesJar by tasks.registering(Jar::class) {
//    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
//    archiveClassifier.set("sources")
//    from(sourceSets["main"].allSource)
//}
//
//val javadocJar by tasks.registering(Jar::class) {
//    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
//    archiveClassifier.set("javadoc")
//    from(sourceSets["main"].allSource)
//}
//
//artifacts {
//    add("archives", sourcesJar)
//    add("archives", javadocJar)
//}
//
//val dateFormatter : DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH).withZone(ZoneOffset.UTC)
//
//tasks.withType<Jar> {
//    manifest {
//        attributes(
//            "Implementation-Title" to project.name,
//            "Implementation-Version" to project.version,
//            "Automatic-Module-Name" to "com.esaulpaugh.headlong",
//            "Created-By" to "Gradle KTS",
//            "Build-Date" to dateFormatter.format(Instant.now())
//        )
//    }
//}
//
//publishing {
//    publications {
//        register("mavenJava", MavenPublication::class) {
//            from(components["java"])
//            artifact(sourcesJar.get())
//            artifact(javadocJar.get())
//        }
//    }
//}
//
//repositories {
//    mavenCentral()
//}
//
//val junitVersion = "5.11.4"
//val bcVersion = "1.80"
//
//dependencies {
//    implementation("com.google.code.gson:gson:") {
//        version {
//            require("[2.1, 2.12.0]") // published in "dependencies"
//        }
//        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
//    }
//
//    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
//    testImplementation("org.bouncycastle:bcprov-jdk14:$bcVersion")
//
//    jmhImplementation("commons-codec:commons-codec:1.17.2")
//}
//
//jmh {
//    jmhVersion.set("1.37")
//}
//
//tasks.withType<AbstractArchiveTask>().configureEach {
//    isPreserveFileTimestamps = false
//    isReproducibleFileOrder = true
//}