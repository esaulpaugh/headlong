import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.gradle.internal.jvm.Jvm;

plugins {
    id("java-library")
    id("maven-publish")
    id("me.champeau.jmh").version("0.6.6")
}

group = "com.esaulpaugh"
version = "5.6.2-SNAPSHOT"

project.ext.set("archivesBaseName", "headlong")

val javaVersion : JavaVersion = Jvm.current().javaVersion!!

tasks.withType<JavaCompile> {
    if (javaVersion > JavaVersion.VERSION_1_8) {
        options.compilerArgs.addAll(arrayOf("--release", "8"))
    }
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    useJUnitPlatform()
}

tasks {
    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by creating(Jar::class) {
        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
        archiveClassifier.set("javadoc")
        from(JavaPlugin.JAVADOC_TASK_NAME)
        finalizedBy("sourcesJar")
    }

    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }
}

val buildDate : String = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("MMMM d yyyy z"))

tasks.withType<Jar> {
    manifest {
        attributes(
            Pair<String, Any?>("Implementation-Title", project.name),
            Pair<String, Any?>("Implementation-Version", project.version),
            Pair<String, Any?>("Automatic-Module-Name", project.name),
            Pair<String, Any?>("Build-Date", buildDate)
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.esaulpaugh"
            artifactId = "headlong"
            version = "5.6.2-SNAPSHOT"
            from(components["java"])
            artifact("sourcesJar")
            artifact("javadocJar")
        }
    }
}

repositories {
    mavenCentral()
}

val junitVersion = "5.8.2"
val bcVersion = "1.70"

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.bouncycastle:bcprov-jdk15on:$bcVersion")

    jmhImplementation("commons-codec:commons-codec:1.15")
}

//import org.gradle.internal.jvm.Jvm
//
//import java.text.SimpleDateFormat
//
//plugins {
//    id 'java-library'
//    id 'maven-publish'
//}
//
//group 'com.esaulpaugh'
//archivesBaseName = "headlong"
//version '5.6.2-SNAPSHOT'
//
//sourceCompatibility = 1.8
//targetCompatibility = 1.8
//
//compileJava {
//    if (Jvm.current().getJavaVersion() > JavaVersion.VERSION_1_8) {
//        options.compilerArgs.addAll(['--release', '8'])
//    }
//}
//
//tasks.withType(JavaCompile) {
//    options.encoding = 'UTF-8'
//}
//
//test {
//    maxParallelForks = Runtime.runtime.availableProcessors()
//}
//
//task javadocJar(type: Jar, dependsOn: javadoc) {
//    classifier = 'javadoc' // deprecated
//    from javadoc.destinationDir
//}
//
//task sourcesJar(type: Jar, dependsOn: classes) {
//    classifier = 'sources' // deprecated
//    from sourceSets.main.allSource
//    finalizedBy(javadocJar)
//}
//
//def junitVersion = '5.8.2'
//def jmhVersion = '1.34'
//def bcVersion = '1.70'
//
//static def todayUTC() {
//    SimpleDateFormat sdf = new SimpleDateFormat("MMMMM d yyyy")
//    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
//    return sdf.format(new Date())
//}
//
//jar {
//    manifest {
//        attributes(
//                'Implementation-Title': project.name,
//                'Implementation-Version': project.version,
//                'Automatic-Module-Name': project.name,
//                'Build-Date': todayUTC()
//        )
//    }
//    finalizedBy(sourcesJar)
//}
//
//artifacts {
//    archives javadocJar, sourcesJar
//}
//
//publishing {
//    publications {
//        headlong(MavenPublication) {
//            from components.java
//            artifact sourcesJar
//            artifact javadocJar
//        }
//    }
//}
//
//sourceSets {
//    jmh {
//        java.srcDirs = ['src/jmh/java']
//        compileClasspath += sourceSets.main.runtimeClasspath
//    }
//}
//
//repositories {
//    mavenCentral()
//}
//
//dependencies {
//
//    implementation 'com.google.code.gson:gson:2.8.9'
//
//    test.useJUnitPlatform()
//    testImplementation 'org.junit.jupiter:junit-jupiter-api:' + junitVersion
//    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:' + junitVersion
//    testImplementation 'org.bouncycastle:bcprov-jdk15on:' + bcVersion
//
//    jmhImplementation 'commons-codec:commons-codec:1.15'
//    jmhImplementation 'org.openjdk.jmh:jmh-core:' + jmhVersion
//    jmhAnnotationProcessor 'org.openjdk.jmh:jmh-generator-annprocess:' + jmhVersion
//    jmhImplementation 'org.bouncycastle:bcprov-jdk15on:' + bcVersion
//}
//
//task jmh(type: JavaExec, dependsOn: jmhClasses) { // run benchmarks with `gradle jmh`
//    main = 'org.openjdk.jmh.Main'
//    classpath = sourceSets.jmh.compileClasspath + sourceSets.jmh.runtimeClasspath
//}
//
//classes.finalizedBy(jmhClasses)