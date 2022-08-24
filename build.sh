mvn clean install -DskipTests=true
docker build -f Dockerfile1 -t service1 .
docker build -f Dockerfile2 -t service2 .
docker build -f Dockerfile3 -t service3 .
docker build -f Dockerfile4 -t service4 .
docker build -f Dockerfile5 -t service5 .
docker-compose up -d

