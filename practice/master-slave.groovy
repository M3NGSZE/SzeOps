pipeline {
    agent any
    
    environment {
        IMAGE_NAME="jenkins-master-slave"
        TAG="${env.BUILD_NUMBER}"
        CONTAINER_NAME = "react-app"
    }

    stages {
        // clone code from github
        stage('Clone code') {
            agent {
                label "master"
            }
            steps {
                git 'https://github.com/M3NGSZE/reactjs-devop11-template.git'
            }
        }

        // scan code with sonarqube
        stage('Scan code') {
            agent {
                label "master"
            }
            environment {
                scannerHome= tool 'sonar-scanner' 
            }

            steps {
                withSonarQubeEnv(credentialsId: 'SONARQUBE_TOKEN', installationName: 'sonar-scanner') {
                script{
                    def projectKey = 'reactjs-devops11-template' 
                    def projectName = 'ReactjsDevOps11template'
                    def projectVersion = '1.0.0'

                    sh """
                        ${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=${projectKey} \
                        -Dsonar.projectName="${projectName}" \
                        -Dsonar.projectVersion=${projectVersion} \
                    """   
                    }
                }
            }
        }

        // Check the quality gate ( passed or failed )
        stage("Wait for Quality Gate "){
            agent {
                label "master"
            }
            steps{
                script{
                    // We must configure webhook to let jenkins know when the result is return 
                    def qg = waitForQualityGate()
                    if ( qg.status != 'OK'){
                        sh """
                            echo " No need to build since you QG is failed "
                        """
                        currentBuild.result='FAILURE'
                        
                        def token="TELEGRAM_TOKEN"
                        def chatId="CHAT_ID"
                        def message="""
                            ❌ SonarQube Quality Gate FAILED
                            Project: ${env.JOB_NAME}
                            Build: #${env.BUILD_NUMBER}
                            Status: ${qg.status}
                        """
                        sendTelegramMessage("${message}", "${token}", "${chatId}")

                        error("Quality Gate is Failed !! ")

                        return 
                    }else {
                        echo "Quality of code is okay!! "
                        currentBuild.result='SUCCESS'
                    }
                }
            }
        }

        stage('Build image') {
            agent { label "master" }
            steps {
                sh "docker build -t jenkins-reactjs-img ."
            }
        }

        //  Push the docker image to the dockerhub 
        stage("Push Image to Dockerhub "){
            agent { label "master" }
            steps{
                withCredentials([
                    usernamePassword(
                        credentialsId: 'DOCKERHUB-CRED', 
                        passwordVariable: 'TOKEN', 
                        usernameVariable: 'USERNAME'
                        )
                    ]) {

                    sh """
                        echo "1. Login to Dockerhub account " 
                        echo "$TOKEN" | docker login -u ${USERNAME} --password-stdin

                        docker tag jenkins-reactjs-img ${USERNAME}/${IMAGE_NAME}:v1.0.${TAG}
                        echo "2. Push image to Dockerhub"
                        docker push ${USERNAME}/${IMAGE_NAME}:v1.0.${TAG}
                    """
                }
            }
        }

        // slave machine
        stage('Deploy container') {
            agent {
                label "slave-01"
            }

            steps {
                sh"""
                    docker stop ${CONTAINER_NAME} || true
                    docker rm ${CONTAINER_NAME} || true

                    docker run -dp 3000:80 --name ${CONTAINER_NAME} \
                    ${USERNAME}/${IMAGE_NAME}:v1.0${TAG}
                """
            }
        }

        stage('Success Alert') {
            agent {
                label "slave-01"
            }

            steps {

                script{
                    def token="TELEGRAM_TOKEN"
                    def chatId="CHAT_ID"

                def message = """
                    ✅ SonarQube Quality Gate PASSED
                """

                sendTelegramMessage("${message}", "${token}", "${chatId}");
                }
            }
        }
            
    }
}


def sendTelegramMessage(String message, String token, String chatId) {
    // upgrade to use Markdown versin instead
    def encodedMessage = URLEncoder.encode(message, "UTF-8")
    sh """ 
        curl -X POST https://api.telegram.org/bot${token}/sendMessage \\
        -d chat_id="${chatId}" \\
        -d text="${encodedMessage}" > /dev/null
    
    """
}