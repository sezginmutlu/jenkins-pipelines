node {
	properties(
		[parameters(
			[choice(choices: 
			[
				'0.1', 
				'0.2', 
				'0.3', 
				'0.4', 
				'0.5'], 
	description: 'Which version of the app should I deploy? ', 
	name: 'Version'), 
	choice(choices: 
	[
		'dev1.devops-chicago.com', 
		'qa1.devops-chicago.com', 
		'stage1.devops-chicago.com', 
		'prod1.devops-chicago.com'], 
	description: 'Please provide an environment to build the application', 
	name: 'ENVIR')])])
	stage("Stage1"){
		timestamps {
			ws {
                checkout([$class: 'GitSCM', branches: [[name: '${Version}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/farrukh90/artemis.git']]])		}
	}
}
	stage("Get Credentials"){
		timestamps {
			ws{
				sh '''
					aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 713287746880.dkr.ecr.us-east-1.amazonaws.com/artemis
					'''
		    }
	    }
    }
    stage("Build Docker Image"){
		timestamps {
			ws {
				sh '''
					docker build -t artemis:${Version} .
					'''
		    }
	    }
    }

    stage("Tag Image"){
		timestamps {
			ws {
				sh '''
                    docker tag artemis:${Version} 945548589381.dkr.ecr.us-east-1.amazonaws.com/artemis:${Version}
                    '''
				}
			}
		}
    stage("Push Image"){
	    timestamps {
			ws {
				sh '''
					docker push 945548589381.dkr.ecr.us-east-1.amazonaws.com/artemis:${Version}
					'''
				}
			}
		}
	stage("Send slack notifications"){
		timestamps {
			ws {
					echo "Slack"
					//slackSend color: '#BADA55', message: 'Hello, World!'
				}
			}
		}

                 stage("Clean Up"){ 
                     timestamps { 
                        ws { 
                            try { 
                                sh ''' 
                                 #!/bin/bash 
                                IMAGES=$(ssh centos@dev1.${ENVIR} docker ps -aq)  
                                for i in \$IMAGES; do 
                                ssh centos@dev1.${ENVIR} docker stop \$i 
                                 ssh centos@dev1.${ENVIR} docker rm \$i 
                                 done  
                                ''' 
                    } catch(e) { 
                    println("Script failed with error: ${e}") 
                } 
            } 
        } 
    }


            stage("Run Container"){ 
                timestamps { 
                   ws { 
                    sh ''' 
                    aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 945548589381.dkr.ecr.us-east-1.amazonaws.com/artemis
                    ssh centos@${ENVIR} docker run -dti -p 5001:5000 713287746880.dkr.ecr.us-east-1.amazonaws.com/artemis:${Version} 
                     ''' 
              } 
         } 
     } 
}