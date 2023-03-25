# From: https://github.com/narathit/unboundid-ldap/blob/master/Dockerfile

FROM openjdk:8-jre-alpine

ENV UNBOUNDID_HOME /opt/unboundid-ldap
ENV UNBOUNDID_VERSION unboundid-ldapsdk-3.1.0-se

ENV port=389
ENV LDAP_LDIF_FILE=/etc/ldap.ldif

ADD https://docs.ldap.com/ldap-sdk/files/${UNBOUNDID_VERSION}.zip unboundid-ldapsdk-se.zip
RUN mkdir -p ${UNBOUNDID_HOME}
RUN mkdir -p ${UNBOUNDID_HOME}/conf
RUN mkdir -p ${UNBOUNDID_HOME}/log
RUN unzip unboundid-ldapsdk-se.zip -d ${UNBOUNDID_HOME}

ENTRYPOINT ./${UNBOUNDID_HOME}/${UNBOUNDID_VERSION}/tools/in-memory-directory-server \
    --baseDN ${baseDN} \
    --port ${port} \
    --additionalBindDN ${additionalBindDN} \
    --additionalBindPassword ${additionalBindPassword} \
    --ldifFile ${LDAP_LDIF_FILE} \
    --accessLogToStandardOut

EXPOSE ${port}
