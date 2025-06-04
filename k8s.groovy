pipeline {
    agent any

    tools {
        jdk 'jdk17'
        maven 'maven3'
    }

    environment {
        SCANNER_HOME = tool 'sonar-scanner'
    }

    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/Vikasghandge/Full-Stack-App.git'
            }
        }

        stage('Build with Maven') {
            steps {
                dir('FullStack-Blogging-App-main') {
                    sh 'mvn clean package'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('FullStack-Blogging-App-main') {
                    withSonarQubeEnv('sonar-server') {
                        sh """
                            ${SCANNER_HOME}/bin/sonar-scanner \
                            -Dsonar.projectKey=full-stack \
                            -Dsonar.project.name=full-stack \
                            -Dsonar.java.binaries=target
                        """
                    }
                }
            }
        }

        // Uncomment this if you want to enforce quality gate
        // stage('Quality Gate') {
        //     steps {
        //         script {
        //             waitForQualityGate abortPipeline: true, credentialsId: 'Sonar-token'
        //         }
        //     }
        // }

        stage('Docker Build & Push') {
            steps {
                dir('FullStack-Blogging-App-main') {
                    script {
                        withDockerRegistry(credentialsId: 'docker') {
                            sh 'docker build -t full-stack .'
                            sh 'docker tag full-stack ghandgevikas/full-stack:latest'
                            sh 'docker push ghandgevikas/full-stack:latest'
                        }
                    }
                }
            }
        }

        stage('Docker Scout Image') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh 'echo $DOCKER_PASSWORD | docker login --username $DOCKER_USERNAME --password-stdin'
                    }
                    sh 'docker-scout quickview full-stack'
                    sh 'docker-scout cves full-stack'
                    sh 'docker-scout recommendations full-stack'
                }
            }
        }

        // stage('Deploy Locally Container') {
        //     steps {
        //         sh 'docker run -d --rm --name full-stack-app -p 8089:8080 full-stack'
        //     }
        // }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'k8s']]) {
                    dir('FullStack-Blogging-App-main/K8S') {
                        sh '''
                            aws eks --region ap-south-1 update-kubeconfig --name EKS_CLOUD
                            kubectl apply -f deployment.yml
                            kubectl apply -f service.yml
                        '''
                    }
                }
            }
        }
    }
}
