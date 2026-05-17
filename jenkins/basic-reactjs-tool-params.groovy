pipeline {
    
    // any agent available to run it, run it
    agent any
    
    tools {
        // You must intall the NodeJs in plugins
        // and configure the toool in Tools configuration
        nodejs 'node-24-lts'
    }
    environment {
        IMAGE_NAME="jenkins-react-img"
        TAG="${env.BUILD_NUMBER}"   // build-in env
    }

    stages {

        stage('Clone Code') {
            steps {
                git 'https://github.com/M3NGSZE/reactjs-devop11-template.git'
            }
        }

        stage('Run Test') {
            when {
                expression {
                    params.RUN_TEST == true
                }
            }
            steps {
                sh """
                    npm --version
                    node --version

                    echo "Running test with NPM test "
                    echo "Run value is : ${params.RUN_TEST}"

                    npm install
                    npm test
                """
            }
        }

        stage('Build Image') {
            steps {
                sh """
                    docker build -t reactjs-demo-image .
                """
            }
        }

        // Push the docker image to dockerhub
        stage('Push Image to Dockerhub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'DOCKERHUB-CRED', passwordVariable: 'TOKEN', usernameVariable: 'USERNAME')]) {
                    sh """
                        echo "1. Login to Dockerhub account"
                        echo "$TOKEN" | docker login -u ${USERNAME} --password-stdin

                        docker tag reactjs-demo-image ${USERNAME}/${IMAGE_NAME}:v1.0.${TAG}

                        echo "2. Push image to Dockerhub"
                        docker push ${USERNAME}/${IMAGE_NAME}:v1.0.${TAG}
                    """
                }
            }
        }

        // push image to dockerhub
        stage('Deploy container') {
            steps {
                script {
                // ALLOW User to choose which environment to deploy    
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
            
                // sh """
                //     docker stop reactjs-cont || true

                //     docker rm reactjs-cont || true

                //     docker run -d -p 3000:80 --name reactjs-cont \
                //         mengsestark/${IMAGE_NAME}:v1.0.${TAG}
                // """
            }
        }

    }
}