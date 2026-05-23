pipeline {
    agent any

    environment {
        IMAGE_NAME = "jenkins-reactjs-img"
        DOCKER_IMAGE = ""
        CONTAINER_NAME = "react-app"
        SONAR_PROJECT_KEY = "reactjs-devops11-template"
        SONAR_PROJECT_NAME = "ReactjsDevOps11template"
        SONAR_PROJECT_VERSION = "1.0.0"
    }

    stages {

        // =========================
        // CLONE CODE
        // =========================
        stage('Clone code') {
            steps {
                git 'https://github.com/M3NGSZE/reactjs-devop11-template.git'
            }
        }

        // =========================
        // SONARQUBE SCAN
        // =========================
        stage('Scan code (SonarQube)') {
            environment {
                scannerHome = tool 'sonar-scanner'
            }

            steps {
                withSonarQubeEnv('sonar-server') {
                    sh """
                        ${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                        -Dsonar.projectName=${SONAR_PROJECT_NAME} \
                        -Dsonar.projectVersion=${SONAR_PROJECT_VERSION} \
                        -Dsonar.sources=.
                    """
                }
            }
        }

        // =========================
        // QUALITY GATE
        // =========================
        stage('Quality Gate') {
            steps {
                script {

                    def qg = waitForQualityGate()

                    env.QG_STATUS = qg.status

                    if (qg.status != 'OK') {

                        echo "❌ Quality Gate FAILED: ${qg.status}"

                        def message = """
❌ SonarQube Quality Gate FAILED
Project: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Status: ${qg.status}
"""

                        sendTelegramMessage(message)

                        error("Stopping pipeline due to Quality Gate failure")

                    } else {

                        echo "✅ Quality Gate PASSED"
                    }
                }
            }
        }

        // =========================
        // BUILD DOCKER IMAGE
        // =========================
        stage('Build image') {
            steps {
                script {
                    env.TAG = "${BUILD_NUMBER}"

                    sh """
                        docker build -t ${IMAGE_NAME}:${TAG} .
                    """
                }
            }
        }

        // =========================
        // LOGIN + PUSH DOCKERHUB
        // =========================
        stage("Push Image to DockerHub") {

            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'DOCKERHUB-CRED',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {

                        env.DOCKER_IMAGE = "${DOCKER_USER}/${IMAGE_NAME}:v1.0.${TAG}"

                        sh """
                            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

                            docker tag ${IMAGE_NAME}:${TAG} ${DOCKER_IMAGE}
                            docker push ${DOCKER_IMAGE}
                        """
                    }
                }
            }
        }

        // =========================
        // DEPLOY ON SLAVE
        // =========================
        stage('Deploy container') {
            agent {
                label "slave-01"
            }

            steps {
                sh """
                    docker stop ${CONTAINER_NAME} || true
                    docker rm ${CONTAINER_NAME} || true

                    docker pull ${DOCKER_IMAGE}

                    docker run -d -p 3000:80 --name ${CONTAINER_NAME} ${DOCKER_IMAGE}
                """
            }
        }

        // =========================
        // SUCCESS NOTIFICATION
        // =========================
        stage('Success Alert') {
            steps {
                script {

                    def message = """
✅ CI/CD Pipeline SUCCESS
Project: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Image: ${DOCKER_IMAGE}
Quality Gate: ${env.QG_STATUS}
"""

                    sendTelegramMessage(message)
                }
            }
        }
    }
}


// =========================
// TELEGRAM FUNCTION
// =========================
def sendTelegramMessage(String message) {

    def token = "YOUR_TELEGRAM_TOKEN"
    def chatId = "YOUR_CHAT_ID"

    sh """
        curl -s -X POST "https://api.telegram.org/bot${token}/sendMessage" \
        -d chat_id="${chatId}" \
        --data-urlencode "text=${message}"
    """
}