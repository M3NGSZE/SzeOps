## NOTE FOR INSTALLING HEADLAMP 

GOAL: web-based UI service for monitor and working with kubernetes 

Old Version: Kubernetes Dashboard ( Retired )


```bash 
kubectl create namespace headlamp
helm repo list 

helm repo add headlamp https://kubernetes-sigs.github.io/headlamp/
helm repo update

helm install headlamp headlamp/headlamp -n headlamp
# get the token values 
kubectl create token headlamp --namespace headlamp
```


# Accessing the service 
```bash 
# 1. using port forward approach 
export POD_NAME=$(kubectl get pods --namespace headlamp -l "app.kubernetes.io/name=headlamp,app.kubernetes.io/instance=headlamp" -o jsonpath="{.items[0].metadata.name}")

export CONTAINER_PORT=$(kubectl get pod --namespace headlamp $POD_NAME -o jsonpath="{.spec.containers[0].ports[0].containerPort}")

echo "Visit http://127.0.0.1:8080 to use your application"
kubectl --namespace headlamp port-forward $POD_NAME 8080:$CONTAINER_PORT
# if conflig port change to 8081 or any other that not conflig

kubectl create token headlamp --namespace headlamp
# 2. publically exposed ( traefik )

```

# Request Certificate and put dns
```bash
# go to directory first
kubectl apply -f headlamp-ingress.yaml

kubectl apply -f cluster-issuer-prod.yaml

kubectl get ingress -n headlamp

kubectl get ingress -n headlamp -w

```