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
        stage('Building Image') {
            steps {
                sh """
                docker build -t lyvanna544/${IMAGE_NAME}:v1.0.${TAG}  . 
                """
            }
        }

        //  Push the docker image to the dockerhub 
        // stage("Push Image to Dockerhub "){
        //     steps{
        //         sh """ 
        //         docker login -u username -p password
        //         docker push image_name

        //         """
        //     }
        // }
        stage('Deploy container ') {
            steps {
                sh """ 
                docker stop reactjs-cont || true 
                docker rm reactjs-cont || true 
                
                docker run -dp 3000:80 --name reactjs-cont \
                    lyvanna544/${IMAGE_NAME}:v1.0.${TAG}
                """
            }
        }
    }
}