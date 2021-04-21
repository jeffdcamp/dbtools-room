pipeline {
    agent any

    environment {
        LIB_MODULE_NAME = 'dbtools-room'
    }

    tools {
        jdk "Java 11"
    }

    stages {
        stage("PR") {
            when {
                changeRequest target: 'master'
            }
            stages {
                stage("Build") {
                    steps {
                        sh './gradlew clean assembleDebug'
                    }
                }
                stage("Test") {
                    steps {
                        sh "./gradlew $LIB_MODULE_NAME:testDebugUnitTest"
                    }
                    post {
                        always {
                            junit '**/build/test-results/**/TEST-*.xml'
                        }
                    }
                }
                stage("Lint") {
                    steps {
                        sh "./gradlew $LIB_MODULE_NAME:lintDebug"
                    }
                    post {
                        always {
                            archiveArtifacts "$LIB_MODULE_NAME/build/reports/*.html"
                        }
                    }
                }
            }
        }
        stage("Alpha") {
            when {
                branch 'master'
            }
            stages {
                stage("Build") {
                    steps {
                        sh './gradlew clean assembleDebug'
                    }
                }
                stage("Test") {
                    steps {
                        sh "./gradlew $LIB_MODULE_NAME:testDebugUnitTest"
                    }
                    post {
                        always {
                            junit '**/build/test-results/**/TEST-*.xml'
                        }
                    }
                }
                stage("Lint") {
                    steps {
                        sh "./gradlew $LIB_MODULE_NAME:lintDebug"
                    }
                    post {
                        always {
                            archiveArtifacts "$LIB_MODULE_NAME/build/reports/*.html"
                        }
                    }
                }
            }
        }
        stage("Release") {
            when {
                branch 'release'
            }
            stages {
                stage("Build") {
                    steps {
                        sh './gradlew clean assembleRelease'
                    }
                }
                stage("Upload to Maven Repo") {
                    steps {
                        sh './gradlew publishMavenPublicationToSoupbowlRepository publishMavenPublicationToMavenCentralRepository'
                    }
                }
            }
        }
    }


    post {
        failure {
            mail(to: 'jeffdcamp@gmail.com',
                    subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) has failed",
                    body: "Please go to ${env.BUILD_URL}.")

            // Notify build breaker
            step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailextrecipients([[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']])])
        }
    }
}