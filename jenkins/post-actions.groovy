pipeline {

    // any agent available to run it , run it 
    agent any
    tools{ 
        // YOu must install the NodeJs in plugins 
        // and configure the tool in Tools configuration 
        nodejs 'node-24-lts'
    }
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


        stage("Run test "){
            when {
                expression {
                    params.RUN_TEST == true 
                }
            }
            steps{
                sh """ 

                npm --version 
                node --version 
                echo "Runing test with NPM test " 
                echo "Run Test value is : ${params.RUN_TEST}"
                npm install 
                npm test

                """
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
        stage('Deploy container ') {

            steps{
            // ALLOW User to choose which environment to deploy 
            script {
                    
                    def userInput = input(
                        id: 'DeployConfig', 
                        message: 'Provide deployment details', 
                        parameters: [
        
                            choice(name: 'ENVIRONMENT', choices: ['Staging', 'Production'], description: 'Target Environment')
                        ]
                    )                      
                    if (userInput=="Production"){
                        sh """
                            echo "Deploying the service inside Production Server "
                        """
                    }else {
                        echo "Deploying the service in other ENVIROnment "
                    }
                }
            }
            
        }
    }

    post{
        always{
            echo "This config always RUN "
            cleanWs() 
        }
        success{
            echo "Only RUn when success "
        }
        failure{
            echo "Only Show when project is failure "
        }
    }
}