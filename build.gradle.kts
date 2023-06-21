//import org.gradle.internal.jvm.Jvm
//import java.time.Instant
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//import java.time.ZoneId
//
//plugins {
//    id("java-library")
//    id("maven-publish")
//    id("me.champeau.jmh").version("0.7.1")
//}
//
//group = "com.esaulpaugh"
//version = "9.3.1-SNAPSHOT"
//
//project.ext.set("archivesBaseName", "headlong")
//
//val javaVersion : JavaVersion = Jvm.current().javaVersion!!
//
//tasks.withType<JavaCompile> {
//    if (javaVersion > JavaVersion.VERSION_1_8) {
//        println("setting release 8")
//        options.compilerArgs.addAll(arrayOf("--release", "8"))
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
//    from(sourceSets.main.get().allSource)
//}
//
//val javadocJar by tasks.registering(Jar::class) {
//    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
//    archiveClassifier.set("javadoc")
//    from(sourceSets.main.get().allSource)
//    finalizedBy(sourcesJar)
//}
//
//artifacts {
//    add("archives", sourcesJar)
//    add("archives", javadocJar)
//}
//
//val dateFormatter : DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH).withZone(ZoneId.of("UTC"))
//
//tasks.withType<Jar> {
//    manifest {
//        attributes(
//            Pair<String, Any?>("Implementation-Title", project.name),
//            Pair<String, Any?>("Implementation-Version", project.version),
//            Pair<String, Any?>("Automatic-Module-Name", project.name),
//            Pair<String, Any?>("Build-Date", dateFormatter.format(Instant.now()))
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
//val junitVersion = "5.9.3"
//val bcVersion = "1.73"
//
//dependencies {
//    implementation("com.google.code.gson:gson:2.10.1")
//
//    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
//    testImplementation("org.bouncycastle:bcprov-jdk14:$bcVersion")
//
//    jmhImplementation("commons-codec:commons-codec:1.15")
//
//    jmh("org.openjdk.jmh:jmh-core:1.36")
//    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.36")
//}
//
//tasks.withType<AbstractArchiveTask>().configureEach {
//    isPreserveFileTimestamps = false
//    isReproducibleFileOrder = true
//}