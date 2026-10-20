#! /bin/bash

show_help(){

CONTENT=$(cat <<EOF

----------------------------------------------\n
  Configure JAVA DSS Server timestamp parameters\n
----------------------------------------------\n
Arguments:\n
-k|--keystore	keystore file\n
-p|--pwd	keystore password\n
-c|--cfg	XML file with tsp configuration\n



Example:\n
   ./config.sh -k cfg/postsignum_client.p12 -p "MeAVIS" -c cfg/tsp-config.xml   

   	

EOF
)
	

echo -e $CONTENT

}


while [[ $# -gt 0 ]]; do
  case $1 in
    -k|--keystore)
      KEYSTORE="$2"
      shift # past argument
      shift # past value
      ;;
    -p|--pwd)
      PASSWORD="$2"
      shift # past argument
      shift # past value
      ;;
    -c|--cfg)
      CONFIG="$2"
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

CONTAINER_ID=$( sudo docker ps -a |grep java|awk '{print $1}')


echo "UPDATE DSS Time server configuration"
echo "------------------------------------"

if [[ -n ${CONFIG} ]]
   then
   if  ! test -f ${CONFIG}  
       then
  		echo "Configuration File ${CONFIG}  does not exist."
  		exit -1
	else
  		CONFIG_FILE=$(realpath ${CONFIG})
  		echo "Using config file ${CONFIG_FILE}"
   fi
fi

if [[ -n ${KEYSTORE} ]]
   then
   if ! test -f ${KEYSTORE}
      then
  		echo "Keystore file ${KEYSTORE} does not exist."
  		exit -1
      else
  		KEYSTORE_FILE=$(realpath ${KEYSTORE})
 		CLK=$(keytool  -list   -keystore  ${KEYSTORE_FILE}  -storepass ${PASSWORD})
		CL=$(echo ${CLK}|grep incorrect)
		if [[ -n $CL ]] 
		   then
			echo "Keystore file or keystore password is invalid"
			echo "ERROR DETAIL:"
    			keytool -list -keystore ${KEYSTORE_FILE} -storepass ${PASSWORD}	
    			exit -1
		   else
			echo "Using keystore file ${KEYSTORE_FILE}"	

		   fi 
     fi
fi



echo "Configuring container with ID: ${CONTAINER_ID}"

echo "Stopping container ..."

sudo docker stop ${CONTAINER_ID}
sudo docker cp ${CONFIG_FILE}  ${CONTAINER_ID}:/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/config/tsp-config.xml
sudo docker cp ${KEYSTORE_FILE}  ${CONTAINER_ID}:/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/

echo "Starting container ..."
sudo docker start ${CONTAINER_ID}

echo "Running containers:"
echo ---------------------
sudo docker ps
exit 0
