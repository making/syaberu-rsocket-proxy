#!/bin/bash
mkdir -p ${HOME}/workspace/mysql
docker run -w /root \
           -v ${HOME}/workspace/mysql:/var/lib/mysql \
           -e MYSQL_DATABASE=syaberu \
           -e MYSQL_ROOT_PASSWORD=password \
           -e MYSQL_USER=demo \
           -e MYSQL_PASSWORD=password \
           -p 3306:3306 \
           -d \
           mysql:5.7
