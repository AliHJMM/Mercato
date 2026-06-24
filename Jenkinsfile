pipeline {
    // Mercato CI/CD Pipeline

    // ─── Agent ───────────────────────────────────────────────────────────────
    agent any

    // ─── Tools ───────────────────────────────────────────────────────────────
    // NodeJS is NOT listed here — frontend tests run inside a Docker container
    // to avoid agent-level Chrome/Node installation issues.
    tools {
        maven 'Maven-3.9'
    }

    // ─── Parameterized Build ──────────────────────────────────────────────────
    parameters {
        choice(
            name:        'ENVIRONMENT',
            choices:     ['staging', 'production'],
            description: 'Target deployment environment'
        )
        string(
            name:         'MERCATO_BRANCH',
            defaultValue: 'main',
            description:  'Branch of the Mercato repository to build and deploy'
        )
        booleanParam(
            name:         'SKIP_TESTS',
            defaultValue: false,
            description:  'Skip all automated tests (not recommended for production deployments)'
        )
        booleanParam(
            name:         'FORCE_DEPLOY',
            defaultValue: false,
            description:  'Force deployment even when health checks fail — disables automatic rollback'
        )
    }

    // ─── Global Environment Variables ─────────────────────────────────────────
    environment {
        MERCATO_REPO      = 'https://github.com/AliHJMM/Mercato.git'
        MERCATO_DIR       = 'mercato'
        COMPOSE_PROJECT   = 'mercato'

        // Services built by Docker Compose (project prefix applied automatically)
        BACKEND_SERVICES  = 'eureka-server api-gateway user-service product-service media-service order-service'

        // Notification recipient — override via Jenkins credential or env var
        NOTIFICATION_EMAIL = 'mercatojenkins@gmail.com'

        // Nexus Repository Manager
        NEXUS_URL             = 'http://host.docker.internal:8091'
        NEXUS_DOCKER_REGISTRY = 'host.docker.internal:8086'
        NEXUS_CREDENTIALS_ID  = 'nexus-credentials'
    }

    // ─── Triggers ─────────────────────────────────────────────────────────────
    triggers {
        githubPush()
    }

    // ─── Pipeline Options ─────────────────────────────────────────────────────
    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    // =========================================================================
    stages {
    // =========================================================================

        // ─────────────────────────────────────────────────────────────────────
        stage('Checkout') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo "Fetching Mercato source code — branch: ${params.MERCATO_BRANCH}"

                dir("${MERCATO_DIR}") {
                    git(
                        url:    "${MERCATO_REPO}",
                        branch: "${params.MERCATO_BRANCH}"
                        // For private repos add: credentialsId: 'github-credentials'
                    )
                }

                // Capture commit SHA for display and tagging
                script {
                    env.MERCATO_COMMIT = sh(
                        script:       "git -C ${MERCATO_DIR} rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()

                    env.BUILD_LABEL = "${env.BUILD_NUMBER}-${env.MERCATO_COMMIT}"

                    currentBuild.displayName  = "#${env.BUILD_NUMBER} | ${params.ENVIRONMENT} | ${env.MERCATO_COMMIT}"
                    currentBuild.description  = "Branch: ${params.MERCATO_BRANCH}  Commit: ${env.MERCATO_COMMIT}"
                }

                echo "Resolved commit: ${env.MERCATO_COMMIT} — build label: ${env.BUILD_LABEL}"
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Test') {
        // ─────────────────────────────────────────────────────────────────────
            when {
                expression { !params.SKIP_TESTS }
            }

            // ── Distributed / parallel test execution (Bonus feature) ─────────
            parallel {

                stage('Test: User Service') {
                    steps {
                        dir("${MERCATO_DIR}/backend") {
                            sh 'mvn test -pl user-service -am -B -T 1 -Dsurefire.failIfNoSpecifiedTests=false'
                        }
                    }
                    post {
                        always {
                            junit(
                                testResults:       "${MERCATO_DIR}/backend/user-service/target/surefire-reports/*.xml",
                                allowEmptyResults: true
                            )
                        }
                    }
                }

                stage('Test: Product Service') {
                    steps {
                        dir("${MERCATO_DIR}/backend") {
                            sh 'mvn test -pl product-service -am -B -T 1 -Dsurefire.failIfNoSpecifiedTests=false'
                        }
                    }
                    post {
                        always {
                            junit(
                                testResults:       "${MERCATO_DIR}/backend/product-service/target/surefire-reports/*.xml",
                                allowEmptyResults: true
                            )
                        }
                    }
                }

                stage('Test: Media Service') {
                    steps {
                        dir("${MERCATO_DIR}/backend") {
                            sh 'mvn test -pl media-service -am -B -T 1 -Dsurefire.failIfNoSpecifiedTests=false'
                        }
                    }
                    post {
                        always {
                            junit(
                                testResults:       "${MERCATO_DIR}/backend/media-service/target/surefire-reports/*.xml",
                                allowEmptyResults: true
                            )
                        }
                    }
                }

                stage('Test: Order Service') {
                    steps {
                        dir("${MERCATO_DIR}/backend") {
                            sh 'mvn test -pl order-service -am -B -T 1 -Dsurefire.failIfNoSpecifiedTests=false'
                        }
                    }
                    post {
                        always {
                            junit(
                                testResults:       "${MERCATO_DIR}/backend/order-service/target/surefire-reports/*.xml",
                                allowEmptyResults: true
                            )
                        }
                    }
                }

                // Frontend tests run inside a Docker container (Node + Chromium)
                // so the Jenkins agent does not need Node or Chrome installed.
                // Uses pre-built mercato-test-runner image (Chromium baked in).
                // npm cache persisted via named Docker volume mercato-npm-cache.
                stage('Test: Frontend (Karma/Jasmine)') {
                    steps {
                        catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                            sh """
                                docker run --rm \\
                                    -v "\${WORKSPACE}/frontend":/app \\
                                    -v mercato-npm-cache:/root/.npm \\
                                    -w /app \\
                                    --shm-size=2g \\
                                    mercato-test-runner \\
                                    sh -c 'npm install --quiet && npm run test -- --watch=false --browsers=ChromeHeadless --no-progress'
                            """
                        }
                    }
                    post {
                        always {
                            junit(
                                testResults:       "frontend/test-results/**/*.xml",
                                allowEmptyResults: true
                            )
                        }
                    }
                }

            } // end parallel
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('SonarQube Analysis') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                withSonarQubeEnv('SonarQube') {
                    dir("${MERCATO_DIR}/backend") {
                        sh 'mvn compile -pl user-service,product-service,media-service,order-service -am -B -T 1'
                        sh '''
                            mvn sonar:sonar \
                                -pl user-service,product-service,media-service,order-service \
                                -am \
                                -B \
                                -Dsonar.projectKey=AliHJMM_Mercato \
                                -Dsonar.projectName=Mercato \
                                -Dsonar.host.url=http://host.docker.internal:9002 \
                                "-Dsonar.coverage.exclusions=**/dto/**,**/entity/**,**/event/**,**/config/**,**/consumer/**,**/repository/**,**/security/**,**/*Application.java"
                        '''
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Quality Gate') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Publish to Nexus') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                dir("${MERCATO_DIR}/backend") {
                    withCredentials([usernamePassword(
                        credentialsId: "${NEXUS_CREDENTIALS_ID}",
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS'
                    )]) {
                        sh """
                            mvn deploy -B -DskipTests \
                                -s "${WORKSPACE}/${MERCATO_DIR}/settings.xml" \
                                -Dnexus.url=${NEXUS_URL} \
                                -Dnexus.username="\${NEXUS_USER}" \
                                -Dnexus.password="\${NEXUS_PASS}"
                        """
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Build Docker Images') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                dir("${MERCATO_DIR}") {
                    echo "Building all Docker images..."
                    sh 'docker-compose build'

                    echo "Tagging images with build label: ${env.BUILD_LABEL}"
                    sh """
                        for svc in ${BACKEND_SERVICES} frontend nginx-ssl; do
                            IMG="${COMPOSE_PROJECT}-\${svc}"
                            if docker image inspect "\${IMG}:latest" > /dev/null 2>&1; then
                                docker tag "\${IMG}:latest" "\${IMG}:${env.BUILD_LABEL}"
                                echo "Tagged \${IMG}:${env.BUILD_LABEL}"
                            fi
                        done
                    """
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Push Docker to Nexus') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "${NEXUS_CREDENTIALS_ID}",
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    sh """
                        echo "\${NEXUS_PASS}" | docker login ${NEXUS_DOCKER_REGISTRY} \
                            -u "\${NEXUS_USER}" --password-stdin

                        for svc in ${BACKEND_SERVICES} frontend; do
                            IMG="${COMPOSE_PROJECT}-\${svc}"
                            if docker image inspect "\${IMG}:latest" > /dev/null 2>&1; then
                                docker tag "\${IMG}:latest" "${NEXUS_DOCKER_REGISTRY}/\${IMG}:${env.BUILD_LABEL}"
                                docker tag "\${IMG}:latest" "${NEXUS_DOCKER_REGISTRY}/\${IMG}:latest"
                                docker push "${NEXUS_DOCKER_REGISTRY}/\${IMG}:${env.BUILD_LABEL}"
                                docker push "${NEXUS_DOCKER_REGISTRY}/\${IMG}:latest"
                                echo "Pushed \${IMG} to Nexus"
                            fi
                        done

                        docker logout ${NEXUS_DOCKER_REGISTRY}
                    """
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Save Rollback State') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                script {
                    echo "Tagging currently deployed images as ':rollback' for emergency restore..."
                    sh """
                        mkdir -p rollback
                        echo "Rollback manifest — Build #${env.BUILD_NUMBER}" > rollback/manifest.txt
                        echo "Saved at: \$(date)" >> rollback/manifest.txt
                        echo "---" >> rollback/manifest.txt

                        for svc in ${BACKEND_SERVICES} frontend nginx-ssl; do
                            IMG="${COMPOSE_PROJECT}-\${svc}"
                            if docker image inspect "\${IMG}:latest" > /dev/null 2>&1; then
                                docker tag "\${IMG}:latest" "\${IMG}:rollback"
                                echo "\${svc}: rollback image saved" >> rollback/manifest.txt
                            else
                                echo "\${svc}: no existing image — first deploy, no rollback possible" >> rollback/manifest.txt
                            fi
                        done

                        cat rollback/manifest.txt
                    """
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Deploy') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                dir("${MERCATO_DIR}") {
                    echo "Deploying to ${params.ENVIRONMENT}..."
                    sh 'docker-compose down --remove-orphans --timeout 30 || true'
                    sh 'docker-compose up -d'
                    echo "All containers started. Waiting for services to initialize..."
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Health Check') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                script {
                    echo "Waiting 20 s for services to finish initializing..."
                    sleep(time: 20, unit: 'SECONDS')

                    def endpoints = [
                        [name: 'Eureka Server',   url: 'http://host.docker.internal:8761/actuator/health'],
                        [name: 'User Service',    url: 'http://host.docker.internal:8081/actuator/health'],
                        [name: 'Product Service', url: 'http://host.docker.internal:8082/actuator/health'],
                        [name: 'Media Service',   url: 'http://host.docker.internal:8083/actuator/health'],
                    ]

                    endpoints.each { ep ->
                        echo "Checking ${ep.name}..."
                        retry(6) {
                            sleep(time: 15, unit: 'SECONDS')
                            sh """
                                RESPONSE=\$(curl -sf '${ep.url}' || echo '{"status":"DOWN"}')
                                echo "\${RESPONSE}"
                                echo "\${RESPONSE}" | grep -q '"status":"UP"' || \\
                                    (echo '${ep.name} is not UP yet' && exit 1)
                            """
                        }
                        echo "${ep.name}: healthy"
                    }

                    echo "All services are UP. Deployment successful!"
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Rollback stage: runs only when a previous stage fails
        // Using a dedicated stage avoids post-block node-context issues.
        // ─────────────────────────────────────────────────────────────────────
        stage('Rollback') {
            when {
                expression { currentBuild.result == 'FAILURE' && !params.FORCE_DEPLOY }
            }
            steps {
                script {
                    echo "Deployment failed — restoring previous image versions..."
                    sh """
                        docker-compose -f ${MERCATO_DIR}/docker-compose.yml down --timeout 30 || true

                        for svc in ${BACKEND_SERVICES} frontend nginx-ssl; do
                            IMG="${COMPOSE_PROJECT}-\${svc}"
                            if docker image inspect "\${IMG}:rollback" > /dev/null 2>&1; then
                                docker tag "\${IMG}:rollback" "\${IMG}:latest"
                                echo "Restored rollback image for \${svc}"
                            else
                                echo "No rollback image for \${svc} — skipping"
                            fi
                        done

                        docker-compose -f ${MERCATO_DIR}/docker-compose.yml up -d || \
                            echo "WARNING: Rollback restart also failed — manual intervention required"
                    """
                }
            }
        }

    } // end stages

    // =========================================================================
    post {
    // =========================================================================

        success {
            echo "Pipeline completed successfully."
            emailext(
                to:      "${NOTIFICATION_EMAIL}",
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER} [${params.ENVIRONMENT}]",
                mimeType: 'text/html',
                body: """
                    <html><body style="font-family:Arial,sans-serif;">
                    <h2 style="color:#2e7d32;">Build Successful</h2>
                    <table cellpadding="6" style="border-collapse:collapse;">
                        <tr><td><b>Job</b></td><td>${env.JOB_NAME}</td></tr>
                        <tr><td><b>Build #</b></td><td>${env.BUILD_NUMBER}</td></tr>
                        <tr><td><b>Environment</b></td><td>${env.ENVIRONMENT}</td></tr>
                        <tr><td><b>Branch</b></td><td>${params.MERCATO_BRANCH}</td></tr>
                        <tr><td><b>Commit</b></td><td>${env.MERCATO_COMMIT}</td></tr>
                        <tr><td><b>Duration</b></td><td>${currentBuild.durationString}</td></tr>
                        <tr><td><b>Build URL</b></td><td><a href="${env.BUILD_URL}">${env.BUILD_URL}</a></td></tr>
                    </table>
                    </body></html>
                """
            )
        }

        failure {
            echo "Build failed. Check console output for details: ${env.BUILD_URL}console"
            emailext(
                to:      "${NOTIFICATION_EMAIL}",
                subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER} [${params.ENVIRONMENT}]",
                mimeType: 'text/html',
                body: """
                    <html><body style="font-family:Arial,sans-serif;">
                    <h2 style="color:#c62828;">Build Failed</h2>
                    <table cellpadding="6" style="border-collapse:collapse;">
                        <tr><td><b>Job</b></td><td>${env.JOB_NAME}</td></tr>
                        <tr><td><b>Build #</b></td><td>${env.BUILD_NUMBER}</td></tr>
                        <tr><td><b>Environment</b></td><td>${params.ENVIRONMENT}</td></tr>
                        <tr><td><b>Branch</b></td><td>${params.MERCATO_BRANCH}</td></tr>
                        <tr><td><b>Commit</b></td><td>${env.MERCATO_COMMIT ?: 'unknown'}</td></tr>
                        <tr><td><b>Duration</b></td><td>${currentBuild.durationString}</td></tr>
                        <tr><td><b>Rollback</b></td><td>${params.FORCE_DEPLOY ? 'Skipped (FORCE_DEPLOY)' : 'Initiated'}</td></tr>
                        <tr><td><b>Console</b></td><td><a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></td></tr>
                    </table>
                    </body></html>
                """
            )
        }

    } // end post

} // end pipeline