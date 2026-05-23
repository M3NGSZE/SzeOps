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
        stage("SonarQube Full Report") {
            steps {
                script {

                    def response = sh(
                        script: """
                        curl -s -u ${SONARQUBE_TOKEN}: \
                        "https://sonarqube-sc.sentry-void.uk/api/measures/component?component=my-project&metricKeys=bugs,vulnerabilities,code_smells,coverage"
                        """,
                        returnStdout: true
                    ).trim()

                    def json = readJSON text: response

                    def measures = json.component.measures

                    def getValue = { key ->
                        return measures.find { it.metric == key }?.value ?: "0"
                    }

                    def bugs = getValue("bugs")
                    def vulnerabilities = getValue("vulnerabilities")
                    def codeSmells = getValue("code_smells")
                    def coverage = getValue("coverage")

                    def token=""
                    def chatId=""

                    def message = """
                    📊 SonarQube Report
                    ────────────────────
                    Project: ${env.JOB_NAME}
                    Build: #${env.BUILD_NUMBER}

                    🐞 Bugs: ${bugs}
                    ⚠️ Vulnerabilities: ${vulnerabilities}
                    💨 Code Smells: ${codeSmells}
                    📈 Coverage: ${coverage}%
                    """

                    sh """
                        curl -s -X POST "https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage" \
                        -d chat_id=${CHAT_ID} \
                        --data-urlencode "text=${message}"
                    """
                }
            }
        }
    }
}
