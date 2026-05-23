pipeline {
    agent any

    environment {
        IMAGE_NAME     = "Jenkins-Master-Slave"
        TAG            = "${env.BUILD_NUMBER}"
        CONTAINER_NAME = "react-app"
    }

    stages {
        stage('Clone code') {
            agent { label "master" }
            steps {
                git branch: 'main', url: 'https://github.com/M3NGSZE/reactjs-devop11-template.git'
            }
        }

        stage('Scan code') {
            agent { label "master" }
            environment { scannerHome = tool 'sonar-scanner' }
            steps {
                withSonarQubeEnv(credentialsId: 'SONARQUBE_TOKEN', installationName: 'sonar-scanner') {
                    sh """
                        ${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=reactjs-devops11-template \
                        -Dsonar.projectName="ReactjsDevOps11template" \
                        -Dsonar.projectVersion=1.0.0
                    """
                }
            }
        }

        stage('Wait for Quality Gate') {
            agent { label "master" }
            steps {
                script {
                    def qg = waitForQualityGate()
                    env.QG_STATUS = qg.status        // ✅ promote scope

                    if (qg.status != 'OK') {
                        withCredentials([
                            string(credentialsId: 'TELEGRAM_TOKEN',   variable: 'TG_TOKEN'),
                            string(credentialsId: 'TELEGRAM_CHAT_ID', variable: 'TG_CHAT')
                        ]) {
                            sendTelegramMessage("❌ Quality Gate FAILED\nJob: ${env.JOB_NAME}\nBuild: #${env.BUILD_NUMBER}\nStatus: ${qg.status}", TG_TOKEN, TG_CHAT)
                        }
                        error("Stopping pipeline: Quality Gate failure")
                    }
                    echo "✅ Quality Gate PASSED"
                }
            }
        }

        stage('Build image') {
            agent { label "master" }
            steps {
                sh "docker build -t jenkins-reactjs-img ."
            }
        }

        stage('Push Image to Dockerhub') {
            agent { label "master" }
            steps {
                withCredentials([usernamePassword(credentialsId: 'DOCKERHUB-CRED', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
                    sh """
                        echo "\$TOKEN" | docker login -u \$USERNAME --password-stdin
                        docker tag jenkins-reactjs-img \$USERNAME/${IMAGE_NAME}:v1.0.${TAG}
                        docker push \$USERNAME/${IMAGE_NAME}:v1.0.${TAG}
                    """
                    script { env.DOCKERHUB_USER = USERNAME }  // ✅ promote to env
                }
            }
        }

        stage('Deploy container') {
            agent { label "slave-01" }
            steps {
                sh """
                    docker stop ${CONTAINER_NAME} || true
                    docker rm   ${CONTAINER_NAME} || true
                    docker run -dp 3000:80 --name ${CONTAINER_NAME} \
                        ${env.DOCKERHUB_USER}/${IMAGE_NAME}:v1.0.${TAG}
                """
            }
        }

        stage('Success Alert') {
            agent { label "slave-01" }
            steps {
                script {                              // ✅ wrapped in script
                    withCredentials([
                        string(credentialsId: 'TELEGRAM_TOKEN',   variable: 'TG_TOKEN'),
                        string(credentialsId: 'TELEGRAM_CHAT_ID', variable: 'TG_CHAT')
                    ]) {
                        sendTelegramMessage("✅ Deploy SUCCESS\nJob: ${env.JOB_NAME}\nBuild: #${env.BUILD_NUMBER}\nStatus: ${env.QG_STATUS}", TG_TOKEN, TG_CHAT)
                    }
                }
            }
        }
    }

    post {
        always {
            node('master') {
                sh 'docker image prune -f || true'
            }
        }
    }
}

def sendTelegramMessage(String message, String token, String chatId) {
    def encodedMessage = URLEncoder.encode(message, "UTF-8")
    sh """
        curl -s -X POST https://api.telegram.org/bot${token}/sendMessage \
        -d chat_id="${chatId}" \
        -d text="${encodedMessage}" > /dev/null
    """
}