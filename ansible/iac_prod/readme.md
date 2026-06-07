## Note for working with IaC

1. playbook that talk to google cloud api , for creating the gcp instance by using ServiceAccount but now it's disable for most account esp free trail account

2. We will use adc for the auth process instead by installation gcloud

```bash
sudo apt-get update
sudo apt-get install apt-transport-https ca-certificates gnupg curl
sudo apt-get update && sudo apt-get install google-cloud-cli



gcloud auth login 
gcloud auth application-default login 


adc_file=/home/mengse/.config/gcloud/application_default_credentials.json
```