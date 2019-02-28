import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.maven

version = "2018.2"

val operatingSystems = listOf("Mac OS X", "Windows", "Linux")
val jdkVersions = listOf("JDK_18", "JDK_11")

project {
//    for (os in operatingSystems) {
//        for (jdk in jdkVersions) {
//            buildType(Build(os, jdk))
//        }
//    }

    forEachJVMAndOS {os, jdk ->
        build(os, jdk)
    }.forEach(this::buildType)
}

class Build(val os: String, val jdk: String) : BuildType({
    id("Build_${os}_${jdk}".toExtId())
    name = "Build ($os, $jdk)"

    vcs {
        root(DslContext.settingsRoot)
    }
})


fun forEachJVMAndOS(a : (BuildOS, JdkVersion) -> BuildType?) : List<BuildType> {
    return JdkVersion.values().flatMap { jdk ->
        BuildOS.values().mapNotNull { os ->
            a(os, jdk)
        }
    }
}

open class JdkAndOsBuildType(
    val os: BuildOS,
    val jdk: JdkVersion
) : BuildType({

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        maven {
            goals = "clean package"
            mavenVersion = defaultProvidedVersion()
            jdkHome = "%env.${jdk.jdkHome}%"
        }
    }

})

fun build(
    os: BuildOS,
    jdk: JdkVersion
) = JdkAndOsBuildType(os, jdk).apply {
    id("Build_${os.caption}_${jdk.caption}".toExtId())
    name = "Build (${os.caption}, ${jdk.caption})"

    steps {
        maven {
            goals = "clean package"
            mavenVersion = defaultProvidedVersion()
            jdkHome = "%env.${jdk.jdkHome}%"
        }
    }

    os(this)
}

enum class BuildOS(
    val caption: String,
    setup: Requirements.() -> Unit
) : (JdkAndOsBuildType) -> Unit {

    Linux("Linux", setup = {
        matches(param = "teamcity.agent.jvm.os.name", value = ".*[Ll]inux.*")
        exists(param = "docker")
        matches(param = "system.agent.name", value = "^(default-.*|dotnet-linux(-clean)?(-admin)?-app-?\\d+-\\d+)$")
    }),

    Windows("Windows", setup = {
        matches(param = "teamcity.agent.jvm.os.name", value = ".*[wW]indows.*")
        matches(param = "system.agent.name", value = "^(default-.*|dotnet-windows(-clean)?(-admin)?-app-?\\d+-\\d+)$")
    }),

    Mac("Mac", setup = {
        matches(param = "teamcity.agent.jvm.os.name", value = ".*OS.*X.*")
        matches(param = "system.agent.name", value = "^(default-.*|dotnet-mac-unit-?\\d+-\\d+)$")
    }),

    ;

    override fun invoke(buildType: JdkAndOsBuildType) {
        buildType.requirements(requirements)
    }

//    val suffix = "_" + caption.toLowerCase()

    val requirements: Requirements.() -> Unit = {
        setup()
    }
}

enum class JdkVersion(teamcityProperty : String) {
    JDK_8(teamcityProperty = "env.JDK_18_x64"),
    JDK_11(teamcityProperty = "env.JDK_11_x64");

    val suffix = "_" + name.toLowerCase()
    val caption = name.toLowerCase().replace("_", "")

    val jdkHome = "%$teamcityProperty%"
}

