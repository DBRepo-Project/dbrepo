FROM nginx:alpine AS runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at

COPY ./dbrepo.conf /etc/nginx/conf.d/default.conf

EXPOSE 443
EXPOSE 80