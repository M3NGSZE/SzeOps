pipeline {

    // any agent available to run it , run it 
    agent any
    environment{
        IMAGE_NAME="jenkins-reactjs-img"
        TAG="${env.BUILD_NUMBER}" // built-in env 
    }
    stages {
        stage('Clone Code ') {
            steps {
                git 'https://github.com/keoKAY/reactjs-devop11-template.git'
            }
        }

        stage("Check Code Quality in Sonarqube "){
            
            environment {
                scannerHome= tool 'sonar-scanner' 
            }

            steps{
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

        stage('Building Image') {
            steps {
                sh """
                docker build -t reactjs-demo-image  . 
                """
            }
        }

        //  Push the docker image to the dockerhub 
        stage("Push Image to Dockerhub "){
            steps{
                withCredentials([usernamePassword(credentialsId: 'DOCKERHUB-CRED', passwordVariable: 'TOKEN', usernameVariable: 'USERNAME')]) {

                    sh """
                        echo "1. Login to Dockerhub account " 
                        echo "$TOKEN" | docker login -u ${USERNAME} --password-stdin

                        docker tag reactjs-demo-image ${USERNAME}/${IMAGE_NAME}:v1.0.${TAG}
                        echo "2. Push image to Dockerhub"
                        docker push ${USERNAME}/${IMAGE_NAME}:v1.0.${TAG}
                    """
                }
            }
        }
    }
}