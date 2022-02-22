
#基础镜像使用openjdk:11
FROM openjdk:8

#作者
MAINTAINER fbi <fbi@fbi.com>
# 工作目录
WORKDIR /opt
#定义工作目录的安装目录
ENV WORK_BASE_PATH /opt
ENV APP laboratory.jar
# jdk
ADD ${APP} .
EXPOSE 1170
ENV PATH $PATH:$JAVA_HOME/bin

ENTRYPOINT ["java","-jar","/opt/laboratory.jar","--spring.profiles.active=prod"]
