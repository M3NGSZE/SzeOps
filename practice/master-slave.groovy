pipeline {
    
    agent : any
    
    environment {
        IMAGE_NAME="Jenkins-Master-Slave"
        TAG="${env.BUILD_NUMBER}"
    }

    stages {
        // clone code from github
        stage('Clone code') {
            steps {
                git 'https://github.com/M3NGSZE/reactjs-devop11-template.git'
            }
        }

        // scan code with sonarqube
        stage('Scan code') {
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
    }
}