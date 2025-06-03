pipeline {
    agent any

    tools {
        jdk 'jdk17'
        maven 'maven3'
    }

    environments {

        SCANNER_HOME = tool 'sonar-scanner'

    }

    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/Vikasghandge/Full-Stack-App.git'
            }
        }
        // stage('Build Code') {
        //     steps {

        //     }
        // }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar-server') {
                    sh '''${SCANNER_HOME}/bin/sonar-scanner \
                    -Dsonar.projectName=Hotstar \
                    -Dsonar.projectKey=Hotstar'''
                }
            }
        }
        stage('Quality Gate') {
            steps {
                script {
                    waitForQualityGate abortPipeline: false, credentialsId: 'Sonar-token'
                }
            }
        }
        stage('Docker Scout FS') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker', toolName: 'docker') {
                        sh 'docker-scout quickview fs://.'
                        sh 'docker-scout cves fs://.'
                    }
                }
            }
        }
        stage('Docker Build & Push') {
            steps {
                dir('Hotstar-Clone-main') {
                    script {
                        withDockerRegistry(credentialsId: 'docker', toolName: 'docker') {
                            sh "docker build -t full-stack ."
                            sh "docker tag full-stack ghandgevikas/full-stack:latest"
                            sh "docker push full-stack"
                        }
                    }
                }
            }
        }
        stage('Docker Scout Image') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker', toolName: 'docker') {
                        sh "docker-scout quickview full-stack"
                        sh "docker-scout cves full-stack"
                        sh "docker-scout recommendations full-stack"
                    }
                }
            }
        }
        stage('Deploy Locally Container') {
            steps {
                dir('') {
                    sh 'docker run -d --name full-stack-app -p 8080:8080 full-stack'
                }
            }
        }
    }
}