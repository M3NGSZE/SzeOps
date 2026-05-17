pipeline {
    agent: any

    nvironment {
        IMAGE_NAME="jenkins-react-img"
        TAG="${env.BUILD_NUMBER}"   // build-in env
    }

    stages {
        stage('Clone Code') {
            steps {
                git 'https://github.com/M3NGSZE/reactjs-devop11-template.git'
            }
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

    stages {
        stage('Send Telegram') {
            steps {
                script{
                    // You need to replace the value here 
                    def token="BotTOken"
                    def chatId="ChatID"
                    def message1="""
                    hello world 
                    welcome to jenkins telegram message
                    """

                    def message2="""
                    *Hello World* 
                    Testing markdown 
                    """
                    sendTelegramMessage("${message1}","${token}","${chatId}")
                    sendTelegramMessageV1("${message2}","${token}","${chatId}")
                }
                
                
            }
        }
    }
}