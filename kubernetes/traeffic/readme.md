# Setup traefik 

```bash 
kubectl create namespace traefik 
# kubectl create ns 

# use helm to install traefik on the cluster 
helm repo add traefik https://traefik.github.io/charts
helm repo update

helm install traefik traefik/traefik \
 -n traefik \
 -f traefik-worker-values.yaml


# check it 
kubectl get pod -n traefik 
kubectl get pod -n traefik -o wide 

# to avoid publicly expose traefik , we can temporarily used kubectl features called port-forward 

kubectl port-forward \
    -n traefik deployment/traefik 8080:8080 
```