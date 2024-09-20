FROM nginxinc/nginx-unprivileged:alpine
# Arguments that can be passed at build time
ARG COMMIT_SHA=unknown
ARG APP_VERSION=0.0.0
ARG APP_DIR=/opt/app
ENV TZ=Etc/UTC \
    LANG=C.UTF-8
USER root
# Copy the static HTML and JS files to the application directory
COPY ../dist ${APP_DIR}
# Create the directorie where the frontend files will be deployed to (${APP_DIR})
# Ensure UID 101 & GID 0 own all the needed directories
# Applying these changes allows the container to run via the OpenShift default SCC "Restricted" whereby arbitrary an UID and GID=0 are assigned
RUN chown -R 101:0 ${APP_DIR} \
    && chmod -R g=u ${APP_DIR}
# Setup entrypoint
COPY ./docker/etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf
# Copy entrypoint script as /entrypoint.sh
COPY ./docker/entrypoint.sh /entrypoint.sh
# Grant Linux permissions and run entrypoint script
RUN chmod +x /entrypoint.sh
EXPOSE 8000
# Specify the user to run as (in numeric format for compatibility with Kubernetes/OpenShift's SCC)
USER 101
# Specify the container working directory
WORKDIR ${APP_DIR}
ENTRYPOINT ["/entrypoint.sh"]
