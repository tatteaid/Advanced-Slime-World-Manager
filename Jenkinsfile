pipeline {
    agent any

    tools {
        maven '3.6.3'
        jdk '8u262'
    }
    environment {
        NEXUS_VERSION = "nexus3"
        NEXUS_PROTOCOL = "https"
        NEXUS_URL = "repo.rapture.pw"
        NEXUS_RELEASE_REPOSITORY = "maven-releases"
	NEXUS_SNAPSHOT_REPOSITORY = "maven-snapshots"
        NEXUS_CREDENTIAL_ID = "rapture.pw-nexus"
    }
    stages {
        stage ('Initialize') {
            steps {
                sh '''
                    wget https://cdn.getbukkit.org/spigot/spigot-1.16.1.jar && mvn install:install-file -Dfile=spigot-1.16.1.jar -DgroupId=org.spigotmc -DartifactId=spigot -Dversion=1.16.1-R0.1-SNAPSHOT -Dpackaging=jar
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
            }
        }

        stage ('Build') {
            steps {
                dir('.'){
                    sh 'mvn -Dmaven.test.failure.ignore=true install'
                }
            }
        }

        stage('Release') {
              steps {
                    dir('.'){
                        echo 'Creating artifacts...';
                        sh "mkdir -p output"
                        sh "mv slimeworldmanager-api/target/slimeworldmanager*.jar output/"
                        sh "mv slimeworldmanager-classmodifier/target/slimeworldmanager*.jar output/"
                        sh "mv slimeworldmanager-plugin/target/slimeworldmanager*.jar output/"
                        sh "mv slimeworldmanager-importer/target/slimeworldmanager*.jar output/"
                        archiveArtifacts artifacts: 'output/*'
                    }
              }
        }

        stage('Maven Publish RELEASE') {
              when {
                branch 'master';
              }
              steps {
                echo 'Publishing artifacts to Nexus...';
		script {
                  pom = readMavenPom file: "pom.xml";
                  filesByGlob = findFiles(glob: "target/*.${pom.packaging}");
                  echo "${filesByGlob[0].name} ${filesByGlob[0].path} ${filesByGlob[0].directory} ${filesByGlob[0].length} ${filesByGlob[0].lastModified}"
                  artifactPath = filesByGlob[0].path;
                  artifactExists = fileExists artifactPath;
                  if(artifactExists) {
                    echo "*** File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${pom.version}";
                    nexusArtifactUploader(
                      nexusVersion: NEXUS_VERSION,
                      protocol: NEXUS_PROTOCOL,
                      nexusUrl: NEXUS_URL,
                      groupId: pom.groupId,
                      version: pom.version,
                      repository: NEXUS_RELEASE_REPOSITORY,
                      credentialsId: NEXUS_CREDENTIAL_ID,
                      artifacts: [
                        [artifactId: pom.artifactId,
                        classifier: '',
                        file: artifactPath,
                        type: pom.packaging],
                        [artifactId: pom.artifactId,
                        classifier: '',
                        file: "pom.xml",
                        type: "pom"]
                        ]
                    );
                  } else {
                    error "*** File: ${artifactPath}, could not be found";
                  }
              }
        }

        stage('Maven Publish SNAPSHOT') {
              when {
                branch 'develop';
              }
              steps {
                echo 'Publishing artifacts to Nexus...';
                script {
                  pom = readMavenPom file: "pom.xml";
                  filesByGlob = findFiles(glob: "target/*.${pom.packaging}");
                  echo "${filesByGlob[0].name} ${filesByGlob[0].path} ${filesByGlob[0].directory} ${filesByGlob[0].length} ${filesByGlob[0].lastModified}"
                  artifactPath = filesByGlob[0].path;
                  artifactExists = fileExists artifactPath;
                  if(artifactExists) {
                    echo "*** File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${pom.version}";
                    nexusArtifactUploader(
                      nexusVersion: NEXUS_VERSION,
                      protocol: NEXUS_PROTOCOL,
                      nexusUrl: NEXUS_URL,
                      groupId: pom.groupId,
                      version: pom.version,
                      repository: NEXUS_SNAPSHOT_REPOSITORY,
                      credentialsId: NEXUS_CREDENTIAL_ID,
                      artifacts: [
                        [artifactId: pom.artifactId,
                        classifier: '',
                        file: artifactPath,
                        type: pom.packaging],
                        [artifactId: pom.artifactId,
                        classifier: '',
                        file: "pom.xml",
                        type: "pom"]
                        ]
                    );
                  } else {
                    error "*** File: ${artifactPath}, could not be found";
                  }
              }
              }
        }
    }

    post {
        always {
            cleanWs();
            withCredentials([string(credentialsId: 'cloudnet-discord-ci-webhook', variable: 'url')]) {
                    discordSend description: 'New build for Advanced Slime World manager!', footer: 'New build!', link: env.BUILD_URL, successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), title: JOB_NAME, webhookURL: url
            }
        }
    }
}
