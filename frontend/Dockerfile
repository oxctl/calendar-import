FROM nginx:stable-alpine

# Just used so that we can point at the source repository
LABEL org.opencontainers.image.source https://github.com/oxctl/calendar-import

ENV NGINX_HOST localhost
ENV NGINX_PORT 8000

COPY default.conf.template /etc/nginx/templates/

COPY build /usr/share/nginx/html