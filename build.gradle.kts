//import org.gradle.internal.jvm.Jvm
//import java.time.Instant
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//import java.time.ZoneId
//
//plugins {
//    id("java-library")
//    id("maven-publish")
//    id("me.champeau.jmh").version("0.6.6")
//}
//
//group = "com.esaulpaugh"
//version = "6.6.3-SNAPSHOT"
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
//    options.encoding = "UTF-8"
//}
//
//tasks.withType<Test> {
//    maxParallelForks = Runtime.getRuntime().availableProcessors()
//    useJUnitPlatform()
//}
//
//tasks {
//    val sourcesJar by creating(Jar::class) {
//        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
//        archiveClassifier.set("sources")
//        from(sourceSets["main"].allSource)
//    }
//
//    val javadocJar by creating(Jar::class) {
//        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
//        archiveClassifier.set("javadoc")
//        from(JavaPlugin.JAVADOC_TASK_NAME)
//        finalizedBy("sourcesJar")
//    }
//
//    artifacts {
//        add("archives", sourcesJar)
//        add("archives", javadocJar)
//    }
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
//        create<MavenPublication>("maven") {
//            groupId = "com.esaulpaugh"
//            artifactId = "headlong"
//            version = "5.6.2-SNAPSHOT"
//            from(components["java"])
//            artifact("sourcesJar")
//            artifact("javadocJar")
//        }
//    }
//}
//
//repositories {
//    mavenCentral()
//}
//
//val junitVersion = "5.8.2"
//val bcVersion = "1.71"
//
//dependencies {
//    implementation("com.google.code.gson:gson:2.9.0")
//
//    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
//    testImplementation("org.bouncycastle:bcprov-jdk15to18:$bcVersion")
//
//    jmhImplementation("commons-codec:commons-codec:1.15")
//}