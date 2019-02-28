import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script

version = "2018.2"

val operatingSystems = listOf("Mac OS X", "Windows", "Linux")
val jdkVersions = listOf("JDK_18", "JDK_9", "JDK_10", "JDK_11")

project {
    for (os in operatingSystems) {
        for (jdk in jdkVersions) {
            buildType(Build(os, jdk))
        }
    }
}

class Build(val os: String, val jdk: String) : BuildType({
    name = "Build_${os}_${jdk}"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
//        maven {
//            goals = "clean package"
//            mavenVersion = defaultProvidedVersion()
//            jdkHome = "%env.${jdk}%"
//        }
        script {
            scriptContent = "echo $os, $jdk"
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", os)
    }
})
