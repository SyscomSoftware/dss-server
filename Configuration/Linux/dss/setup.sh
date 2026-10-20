#! /bin/bash

show_help(){

CONTENT=$(cat <<EOF

----------------------------------------------\n
  Setup dss server and SSW  proxy server\n
----------------------------------------------\n
Arguments:\n
-c|--cfile	.pfx file with proxy server certificate\n
-p|--cpwd	certificate file password\n
-u|--user	GitHub user\n
-t|--tag	Tag of image to be installed\n
-e|--port	SSL endpoint (port) number\n
-k|--key	GitHub Personal Access Token\n
\n
Example:\n
	./setup.sh -c certificate.pfx -p kolomaz -u SspUser -t 2409301 -e 33443 \n

 SSL certificate "certificate.pfx" with password "kolomaz"\n
 Images for tag 240931 will be downloaded and installed \n
 Github user is SspUser\n
   - Personal Access Token for Github user SspUser must be in environment variable PAT_TOKEN !!!!!! \n
 Proxy server will be available on port 33443\n
EOF
)
	

echo -e $CONTENT

}


SSL_PORT=32443
POSITIONAL_ARGS=()
while [[ $# -gt 0 ]]; do
  case $1 in
    -c|--cfile)
      CERT_FILE="$2"
      shift # past argument
      shift # past value
      ;;
    -p|--cpwd)
      CERT_PASSWORD="$2"
      shift # past argument
      shift # past value
      ;;
    -t|--tag)
      TAG="$2"
      shift # past argument
      shift # past value
      ;;  
    -u|--user)
      GIT_USER="$2"
      shift # past argument
      shift # past value
      ;;  
    -e|--port)
      SSL_PORT="$2"
      shift
      shift
      ;;		
    -k|--key)
      PAT_TOKEN="$2"
      shift # past argument
      shift # past value
      ;;  
    -?|--help)
      show_help	
      exit
      ;;
    --default)
      DEFAULT=YES
      shift # past argument
      ;;
    -*|--*)
      echo "Unknown option $1"
      exit 1
      ;;
    *)
      POSITIONAL_ARGS+=("$1") # save positional arg
      shift # past argument
      ;;
  esac
done

PAT_TOKEN="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"

echo "User: ${GIT_USER}" 
echo "Tag:  ${TAG}"
echo "Cert file: ${CERT_FILE}"
echo "Cert password: ${CERT_PASSWORD}"
echo "GITHUB Token: ${PAT_TOKEN}"

echo $PAT_TOKEN |sudo docker login ghcr.io -u "$GIT_USER" --password-stdin

sudo docker pull ghcr.io/syscomsoftware/bridge-dss-server:"$TAG"

sudo docker pull ghcr.io/syscomsoftware/java-dss-server:"$TAG"

if [ ! -d ~/.aspnet ]; then
  mkdir ~/.aspnet
fi


if [ ! -d ~/.aspnet/https ]; then
  mkdir ~/.aspnet/https
fi

cp "${CERT_FILE}" ~/.aspnet/https/dss_cert.pfx

HTTPS_PATH=~/.aspnet/https



cat > compose_config.yaml <<EOF
services:
  proxy:
    image: ghcr.io/syscomsoftware/bridge-dss-server:$TAG
    restart: unless-stopped
    ports:
      - $SSL_PORT:443
      - 32080:80  
    environment:
      DssServer__Url: "http://server:8080"
      ASPNETCORE_ENVIRONMENT: "Development"
      ASPNETCORE_URLS: "https://+:443;http://+:80"
      ASPNETCORE_Kestrel__Certificates__Default__Password: $CERT_PASSWORD 
      ASPNETCORE_Kestrel__Certificates__Default__Path: /https/dss_cert.pfx 
    volumes:
      - $HTTPS_PATH:/https:ro
    depends_on: 
      - "server"
  server:
    image:  ghcr.io/syscomsoftware/java-dss-server:$TAG
    restart: unless-stopped
    ports:
      - 8080:8080
EOF

echo "Running docker-compose"


sudo docker-compose -f compose_config.yaml -p dss  up -d


echo "Docker containers running:"
echo "-------------------------------"

sudo docker ps

