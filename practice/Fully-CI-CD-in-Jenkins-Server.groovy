pipeline {
    agent any
    
    environment {
        IMAGE_NAME = "jenkins-master-slave"
        TAG = "${env.BUILD_NUMBER}"
        CONTAINER_NAME = "react-app"
        DOCKERHUB_USERNAME = "mengsestark"
    }

    stages {

        // Clone code from GitHub
        stage('Clone code') {
            agent {
                label "master"
            }

            steps {
                git 'https://github.com/M3NGSZE/reactjs-devop11-template.git'
            }
        }

        // Scan code with SonarQube
        stage('Scan code') {

            agent {
                label "master"
            }

            environment {
                scannerHome = tool 'sonar-scanner'
            }

            steps {

                withSonarQubeEnv(
                    credentialsId: 'SONARQUBE_TOKEN',
                    installationName: 'sonar-scanner'
                ) {

                    script {

                        def projectKey = 'reactjs-devops11-template'
                        def projectName = 'ReactjsDevOps11template'
                        def projectVersion = '1.0.0'

                        sh """
                            ${scannerHome}/bin/sonar-scanner \
                            -Dsonar.projectKey=${projectKey} \
                            -Dsonar.projectName="${projectName}" \
                            -Dsonar.projectVersion=${projectVersion}
                        """
                    }
                }
            }
        }

        // Wait for SonarQube Quality Gate
        stage('Wait for Quality Gate') {

            agent {
                label "master"
            }

            steps {

                script {

                    def qg = waitForQualityGate()

                    if (qg.status != 'OK') {

                        currentBuild.result = 'FAILURE'

                        def token="TELEGRAM_TOKEN"
                        def chatId="CHAT_ID"

                        def message = """
                            ❌ SonarQube Quality Gate FAILED
                            Project: ${env.JOB_NAME}
                            Build: #${env.BUILD_NUMBER}
                            Status: ${qg.status}
                        """

                        sendTelegramMessage(message, token, chatId)

                        error("Quality Gate Failed")

                    } else {

                        echo "Quality Gate PASSED"
                        currentBuild.result = 'SUCCESS'
                    }
                }
            }
        }

        // Build Docker Image
        stage('Build image') {

            agent {
                label "master"
            }

            steps {

                sh '''
                    docker build -t jenkins-reactjs-img .
                '''
            }
        }

        // Push Image to DockerHub
        stage('Push Image to DockerHub') {

            agent {
                label "master"
            }

            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'DOCKERHUB-CRED',
                        usernameVariable: 'USERNAME',
                        passwordVariable: 'TOKEN'
                    )
                ]) {

                    sh '''
                        echo "1. Login to DockerHub"

                        echo "$TOKEN" | docker login \
                        -u "$USERNAME" \
                        --password-stdin

                        echo "2. Tag image"

                        docker tag jenkins-reactjs-img \
                        "$USERNAME/$IMAGE_NAME:v1.0.$TAG"

                        echo "3. Push image"

                        docker push \
                        "$USERNAME/$IMAGE_NAME:v1.0.$TAG"
                    '''
                }
            }
        }

        // Deploy Container on Slave Machine
        stage('Deploy container') {

            agent {
                label "slave-01"
            }

            steps {

                sh '''
                    docker stop $CONTAINER_NAME || true

                    docker rm $CONTAINER_NAME || true

                    docker pull \
                    $DOCKERHUB_USERNAME/$IMAGE_NAME:v1.0.$TAG

                    docker run -dp 3000:80 \
                    --name $CONTAINER_NAME \
                    $DOCKERHUB_USERNAME/$IMAGE_NAME:v1.0.$TAG
                '''
            }
        }

        // Success Alert
        stage('Success Alert') {

            agent {
                label "slave-01"
            }

            steps {

                script {

                    def token="TELEGRAM_TOKEN"
                    def chatId="CHAT_ID"

                    def message = """
                        ✅ CI/CD Pipeline SUCCESS

                        Project: ${env.JOB_NAME}
                        Build: #${env.BUILD_NUMBER}
                    """

                    sendTelegramMessage(message, token, chatId)
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


// Telegram Function
def sendTelegramMessage(String message, String token, String chatId) {

    def encodedMessage = URLEncoder.encode(message, "UTF-8")

    sh """
        curl -X POST https://api.telegram.org/bot${token}/sendMessage \\
        -d chat_id="${chatId}" \\
        -d text="${encodedMessage}" > /dev/null
    """
}