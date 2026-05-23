pipeline {
    
    agent any

    stages {
        stage('Work 1') {

            agent {
                label "slave-01"
            }

            steps {
                sh """
                    whoami

                    curl ifconfig.me

                    pwd
                """
            }
        }

        stage('Work 2') {

            agent {
                label "master"
            }

            steps {
                sh """
                    whoami

                    curl ifconfig.me

                    pwd
                """
            }
        }
    }
}