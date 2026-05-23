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

                        def token=""
                        def chatId=""
                        def message1="""
                        hello world
                        welcome to jenkins telelgram message 2
                        """
                        sendTelegramMessage("${message2}", "${token}", "${chatId}")

                        return 
                    }else {
                        echo "Quality of code is okay!! "
                        currentBuild.result='SUCCESS'
                    }
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