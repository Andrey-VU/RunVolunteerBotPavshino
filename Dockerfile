FROM openjdk:17.0.2-jdk-slim-buster

ENV prefix=/bot

ENV TZ="Europe/Moscow"
RUN echo $TZ > /etc/timezone && dpkg-reconfigure -f noninteractive tzdata

ENV RunVolunteerBotPavshinoLocalConfigDir=$prefix/local_config
ENV RunVolunteerBotPavshinoLocalStorageDir=$prefix/local_storage

RUN mkdir -p $RunVolunteerBotPavshinoLocalConfigDir
RUN mkdir -p $RunVolunteerBotPavshinoLocalStorageDir

#COPY local_config/* $RunVolunteerBotPavshinoLocalConfigDir
#COPY local_storage/* $RunVolunteerBotPavshinoLocalStorageDir

COPY target/*.jar $prefix/app.jar

ENTRYPOINT ["java","-jar","/bot/app.jar"]
