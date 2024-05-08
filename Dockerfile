FROM maven:3.9.5-eclipse-temurin-17 as build

COPY pom.xml /usr/src/mymaven/dss-demonstrations/

# COPY dss-standalone-app/pom.xml /usr/src/mymaven/dss-demonstrations/dss-standalone-app/
# COPY dss-standalone-app/src /usr/src/mymaven/dss-demonstrations/dss-standalone-app/src

# COPY dss-standalone-app-package/pom.xml /usr/src/mymaven/dss-demonstrations/dss-standalone-app-package/
# COPY dss-standalone-app-package/src /usr/src/mymaven/dss-demonstrations/dss-standalone-app-package/src

COPY dss-demo-webapp/pom.xml /usr/src/mymaven/dss-demonstrations/dss-demo-webapp/
COPY dss-demo-webapp/src /usr/src/mymaven/dss-demonstrations/dss-demo-webapp/src

# COPY dss-demo-bundle/pom.xml /usr/src/mymaven/dss-demonstrations/dss-demo-bundle/

# COPY dss-rest-doc-generation/pom.xml /usr/src/mymaven/dss-demonstrations/dss-rest-doc-generation/
# COPY dss-esig-validation-tests/pom.xml /usr/src/mymaven/dss-demonstrations/dss-esig-validation-tests/

# OPTIONAL: provide documentation + javaDoc (change "pathTo")
#COPY pathTo/generated-docs /usr/src/mymaven/dss/dss-cookbook/target/generated-docs
#COPY pathTo/site/apidocs /usr/src/mymaven/dss/target/site/apidocs

WORKDIR /usr/src/mymaven/dss-demonstrations
#RUN mvn package -pl dss-standalone-app,dss-standalone-app-package,dss-demo-webapp -P quick
RUN mvn package -pl dss-demo-webapp -P quick

FROM tomcat:10
COPY --from=build /usr/src/mymaven/dss-demonstrations/dss-demo-webapp/target/dss-demo-webapp-*.war /usr/local/tomcat/webapps/ROOT.war

# Copy POSTSIGNUM Certification authority root certificates
COPY  postsignum-certificates/postsignum_qca2_root.cer /opt/
COPY  postsignum-certificates/postsignum_qca4_root.cer /opt/

# Copy POSTSIGNUM TSA authorities certificates
# COPY postsignum-certificates/postsignum_tsa_tsu1.cer /opt/
# COPY postsignum-certificates/postsignum_tsa_tsu2.cer /opt/
# COPY postsignum-certificates/postsignum_tsa_tsu3.cer /opt/
# COPY postsignum-certificates/postsignum_tsa_tsu4.cer /opt/
# COPY postsignum-certificates/postsignum_tsa_tsu5.cer /opt/
# COPY postsignum-certificates/postsignum_tsa_tsu6.cer /opt/

RUN keytool  -cacerts -storepass changeit -importcert -alias postsignum_qca2_root -noprompt -file /opt/postsignum_qca2_root.cer
RUN keytool  -cacerts -storepass changeit -importcert -alias postsignum_qca4_root -noprompt -file /opt/postsignum_qca4_root.cer

# RUN keytool -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -importcert -alias ps_tsu1 -noprompt -file /opt/postsignum_tsa_tsu1.cer
# RUN keytool -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -importcert -alias ps_tsu2 -noprompt -file /opt/postsignum_tsa_tsu2.cer
# RUN keytool -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -importcert -alias ps_tsu3 -noprompt -file /opt/postsignum_tsa_tsu3.cer
# RUN keytool -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -importcert -alias ps_tsu4 -noprompt -file /opt/postsignum_tsa_tsu4.cer
# RUN keytool -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -importcert -alias ps_tsu5 -noprompt -file /opt/postsignum_tsa_tsu5.cer
# RUN keytool -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -importcert -alias ps_tsu6 -noprompt -file /opt/postsignum_tsa_tsu6.cer
# RUN keytool  -list -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit |grep postsignum
RUN echo "Installed postsignum certificates:"
RUN echo "----------------------------------"
RUN keytool  -list -cacerts -storepass changeit|grep postsignum
RUN echo "---- END OF CERTIFICATES LIST ----"

EXPOSE 8080