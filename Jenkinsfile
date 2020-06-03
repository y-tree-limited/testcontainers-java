pipeline {
  agent {
    label "jenkins-gradle5-xl"
  }
  environment {
    APP_NAME = 'lib-ms-core'
    TESTCONTAINERS_RYUK_DISABLED = "true"
  }
  stages {
    stage('Build PR Snapshot') {
      when {
        branch 'PR-*'
      }
      steps {
        container('gradle5') {
          sh "git config --global credential.helper store"
          sh "jx step git credentials"
          sh "echo \$(jx-release-version)-SNAPSHOT > VERSION"
          //sh "export GRADLE_USER_HOME=/opt/gradle"
          //sh "cp /root/gradle_user_home/gradle.properties /opt/gradle/"
          sh "cp /root/gradle_user_home/init.gradle init.gradle"
          sh "./gradlew -Pversion=\$(cat VERSION) clean build publish -x test -x japicmp -x jarFileTest --info"
        }
      }
    }

    stage('Build Release') {
      when {
        branch 'master'
      }
      steps {
        container('gradle5') {
          // ensure we're not on a detached head
          sh "git checkout master"
          sh "git config --global credential.helper store"
          sh "jx step git credentials"

          // so we can retrieve the version in later steps
          sh "echo \$(jx-release-version) > VERSION"
          sh "jx step tag --version \$(cat VERSION)"
          //sh "export GRADLE_USER_HOME=/opt/gradle"
          //sh "cp /root/gradle_user_home/gradle.properties /opt/gradle/"
          sh "cp /root/gradle_user_home/init.gradle init.gradle"

          sh "./gradlew -Pversion=\$(cat VERSION) clean build publish -x test -x japicmp -x jarFileTest --info"
        }
      }
    }
  }
  post {
        always {
          cleanWs()
        }
  }
}
