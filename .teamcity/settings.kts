import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.maven

version = "2018.2"

//region simple stuff
//val operatingSystems = listOf("Mac OS X", "Windows", "Linux")
//val jdkVersions = listOf("JDK_18", "JDK_11")
//
//class Build(val os: String, val jdk: String) : BuildType({
//    id("Build_${os}_${jdk}".toExtId())
//    name = "Build ($os, $jdk)"
//
//    vcs {
//        root(DslContext.settingsRoot)
//    }
//})
//endregion

project {
    forEachJVMAndOS { os, jdk ->
        build(os, jdk)
    }.forEach(this::buildType)
}


fun forEachJVMAndOS(a: (BuildOS, JdkVersion) -> BuildType?): List<BuildType> {
    return JdkVersion.values().flatMap { jdk ->
        BuildOS.values().mapNotNull { os ->
            a(os, jdk)
        }
    }
}

fun build(
    os: BuildOS,
    jdk: JdkVersion
) = BuildType {
    id("Build_${os.caption}_${jdk.caption}".toExtId())
    name = "Build (${os.caption}, ${jdk.caption})"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        maven {
            goals = "clean package"
            mavenVersion = defaultProvidedVersion()
            jdkHome = jdk.jdkHome
        }
    }

    os(this)
}

enum class BuildOS(
    val caption: String,
    setup: Requirements.() -> Unit
) : (BuildType) -> Unit {

    Linux("Linux", setup = {
        matches(param = "teamcity.agent.jvm.os.name", value = ".*[Ll]inux.*")
    }),

    Windows("Windows", setup = {
        matches(param = "teamcity.agent.jvm.os.name", value = ".*[wW]indows.*")
    }),

    Mac("Mac", setup = {
        matches(param = "teamcity.agent.jvm.os.name", value = ".*OS.*X.*")
    }),

    ;

    override fun invoke(buildType: BuildType) {
        buildType.requirements(requirements)
    }

    val requirements: Requirements.() -> Unit = {
        setup()
    }
}

enum class JdkVersion(teamcityProperty: String) {
    JDK_8("env.JDK_18_x64"),
    JDK_11("env.JDK_11_x64");

    val caption = name.toLowerCase().replace("_", "")

    val jdkHome = "%$teamcityProperty%"
}

