pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "manjukolkar07/test-dev:latest"
        DEPLOY_FILE  = "deploy.yaml"
        DOMAIN       = "scrollweb.duckdns.org"
    }

    stages {

        stage('User Confirmation') {
            steps {
                script {
                    def userInput = input(
                        id: 'userConfirm',
                        message: 'Do you want to build this project?',
                        parameters: [
                            choice(
                                name: 'CONFIRM',
                                choices: ['Yes', 'No'],
                                description: 'Select Yes to proceed or No to abort'
                            )
                        ]
                    )

                    if (userInput == 'No') {
                        echo "Build aborted by user."
                        currentBuild.result = 'ABORTED'
                        error("User chose not to proceed.")
                    }
                }
            }
        }

        stage('Select Branch') {
            steps {
                script {
                    BRANCH = input(
                        message: 'Select Git Branch',
                        parameters: [
                            choice(
                                name: 'BRANCH_NAME',
                                choices: ['master', 'main', 'dev'],
                                description: 'Choose branch to build'
                            )
                        ]
                    )
                }
            }
        }

        stage('Checkout Code') {
            steps {
                git branch: "${BRANCH}",
                    url: 'https://github.com/manjukolkar/scroll-web.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                docker build -t ${DOCKER_IMAGE} .
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASS')]) {
                    sh """
                    echo \$DOCKER_PASS | docker login -u manjukolkar07 --password-stdin
                    docker push ${DOCKER_IMAGE}
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh """
                kubectl apply -f ${DEPLOY_FILE}
                """
            }
        }

        stage('Verify Deployment') {
            steps {
                sh """
                kubectl get pods
                echo "Application deployed at: http://${DOMAIN}"
                """
            }
        }
    }

    post {
        success {
            echo "Pipeline executed successfully 🚀"
        }
        failure {
            echo "Pipeline failed ❌"
        }
        aborted {
            echo "Pipeline aborted by user ⚠️"
        }
    }
}