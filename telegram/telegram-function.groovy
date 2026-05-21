pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                script{
                def token="***********"
                def chatId="**************"
                sh """ curl -X POST https://api.telegram.org/bot${token}/sendMessage -d chat_id="${chatId}" -d "text=Hello from my terminal!" """
            }
            }
        }
    }
}
