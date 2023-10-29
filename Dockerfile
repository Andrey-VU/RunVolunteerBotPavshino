FROM  openjdk:17.0.2-jdk-slim-buster
ENV PREFIX=/RunVolunteerBotPavshino
ENV LOCAL_CONFIGDI_R=$RunVolunteerBotPavshinoDir/local_config
ENV LOCAL_STORAGE_DIR=$PREFIX/local_storage
RUN mkdir -p $LOCAL_CONFIG_DIR
RUN mkdir -p $LOCAL_STORAGE_DIR
COPY target/*.jar $PREFIX/app.jar
COPY local_config/* $LOCAL_CONFIG_DIR
COPY local_data/* $LOCAL_STORAGE_DIR
ENTRYPOINT ["java","-jar","/RunVolunteerBotPavshino/app.jar"]