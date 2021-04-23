## Running in Kubernetes (using Terraform and Helm Chart)
Add helm repo:
```sh
helm repo add main https://bedag.github.io/helm-charts/  && helm repo update
```
Prepare terraform:
```sh
terraform validate
terraform fmt
```
Planning:
```sh
terraform plan
```
Install:
```sh
terraform apply
```
then Enter "yes"

Good Luck
