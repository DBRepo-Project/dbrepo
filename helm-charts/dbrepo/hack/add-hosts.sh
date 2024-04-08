#!/bin/bash
cat /etc/hosts | grep "dbrepo.local"
if [ "$?" -ne 0 ]; then
  echo "$(minikube ip)  dbrepo.local" | sudo tee -a /etc/hosts
fi