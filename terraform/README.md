## Running in Kubernetes (using Terraform and Helm Provider)
Add helm repo:
```sh
helm repo add main https://bedag.github.io/helm-charts/  && helm repo update
```
Prepare terraform:
```sh
terraform init
terraform validate
terraform fmt
```
Planning:
```sh
terraform plan -out planed
```
Install:
```sh
terraform apply planed
```
Good Luck
