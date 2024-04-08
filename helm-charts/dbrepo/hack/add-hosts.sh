#!/bin/bash
echo "$(minikube ip)  dbrepo.local" | sudo tee -a /etc/hosts