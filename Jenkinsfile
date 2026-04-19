pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    BACKEND_IMAGE = 'pharmacy-backend'
    FRONTEND_IMAGE = 'pharmacy-frontend'
    IMAGE_TAG = 'local'
    DOCKER_REGISTRY = "${env.DOCKER_REGISTRY ?: ''}"
    DOCKER_PUSH = "${env.DOCKER_PUSH ?: 'false'}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Prepare Build Metadata') {
      steps {
        script {
          def shortSha = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortSha}"
        }
      }
    }

    stage('Gradle Clean Build') {
      steps {
        sh './gradlew :backend:clean :backend:build --no-daemon -x test -x integrationTest'
      }
    }

    stage('Unit Tests') {
      steps {
        sh './gradlew :backend:test --no-daemon'
      }
    }

    stage('Integration Tests') {
      steps {
        sh './gradlew :backend:integrationTest :backend:check --no-daemon'
      }
    }

    stage('Frontend Build') {
      steps {
        dir('frontend') {
          sh 'npm ci'
          sh 'npm run build'
        }
      }
    }

    stage('Build Docker Images') {
      steps {
        sh 'docker build -f backend/Dockerfile -t ${BACKEND_IMAGE}:${IMAGE_TAG} .'
        sh 'docker build -f frontend/Dockerfile -t ${FRONTEND_IMAGE}:${IMAGE_TAG} .'
      }
    }

    stage('Container Vulnerability Scan') {
      steps {
        sh 'if command -v trivy >/dev/null 2>&1; then trivy image --severity HIGH,CRITICAL --exit-code 1 ${BACKEND_IMAGE}:${IMAGE_TAG}; else echo "Trivy not installed - skipping scan"; fi'
        sh 'if command -v trivy >/dev/null 2>&1; then trivy image --severity HIGH,CRITICAL --exit-code 1 ${FRONTEND_IMAGE}:${IMAGE_TAG}; else echo "Trivy not installed - skipping scan"; fi'
      }
    }

    stage('Push Docker Images') {
      when {
        expression { env.DOCKER_PUSH == 'true' && env.DOCKER_REGISTRY?.trim() }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
          sh 'echo "$REG_PASS" | docker login $DOCKER_REGISTRY -u "$REG_USER" --password-stdin'
          sh 'docker tag ${BACKEND_IMAGE}:${IMAGE_TAG} $DOCKER_REGISTRY/${BACKEND_IMAGE}:${IMAGE_TAG}'
          sh 'docker tag ${FRONTEND_IMAGE}:${IMAGE_TAG} $DOCKER_REGISTRY/${FRONTEND_IMAGE}:${IMAGE_TAG}'
          sh 'docker push $DOCKER_REGISTRY/${BACKEND_IMAGE}:${IMAGE_TAG}'
          sh 'docker push $DOCKER_REGISTRY/${FRONTEND_IMAGE}:${IMAGE_TAG}'
          sh 'docker logout $DOCKER_REGISTRY'
        }
      }
    }
  }

  post {
    always {
      junit testResults: 'backend/build/test-results/test/*.xml,backend/build/test-results/integrationTest/*.xml', allowEmptyResults: true
      archiveArtifacts artifacts: 'backend/build/reports/**,frontend/dist/**', allowEmptyArchive: true
    }
  }
}


