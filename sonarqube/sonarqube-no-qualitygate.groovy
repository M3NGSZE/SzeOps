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
                git 'https://github.com/M3NGSZE/reactjs-devop11-template.git'
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

        // Check the quality gate ( passed or failed )
        stage("Wait for Quality Gate "){
            steps{
                script{
                    // We must configure webhook to let jenkins know when the result is return 
                    def qg = waitForQualityGate()
                    if ( qg.status != 'OK'){
                        sh """
                            echo " No need to build since you QG is failed "
                        """
                        currentBuild.result='FAILURE'
                        error("Quality Gate is Failed !! ")
                        return 
                    }else {
                        echo "Quality of code is okay!! "
                        currentBuild.result='SUCCESS'
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