# AnimalKingdom

Animal Kingdom is a project and application that will allow you to take a picture of an animal, reconize it, and show it to the community and more ... ! 

### How do I get set up?

* **Clone The project**: 

	 git clone https://kasra21@bitbucket.org/kasra21/animalkingdom.git

* **Details**:

* **Build/Deploy**:

For the spring boot to be deployed at the root directory execute:

	 mvn clean package
	 
This will generate your binary file which you may run:

	java -jar target/boot-animalkingdom-1.0.jar
	
Try to avoid directly running it with `mvn spring-boot:run` which may cause problems for shutting down the service.
Then assuming that the database is already set up you may access the app from:

	http://localhost:8090/
	
Or by making rest requests or by using postman or equivalent software

### Licencing

This app uses TensorFlow which can be used for commercial purposes:

https://www.reddit.com/r/tensorflow/comments/4jrh2t/am_i_allowed_to_use_tensorflow_in_a_commercial/

### Contribution and Management

https://confluence.atlassian.com/bitbucket/use-trello-boards-in-bitbucket-935890052.html